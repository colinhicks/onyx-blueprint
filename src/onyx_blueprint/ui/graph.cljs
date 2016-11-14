(ns onyx-blueprint.ui.graph
  (:require [cljs.pprint :as pprint]
            [goog.dom :as gdom]
            [monet.canvas :as canvas]
            [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            [onyx-blueprint.extensions :as extensions]
            [onyx-blueprint.ui.helpers :as helpers]
            [onyx-local-rt.api :as onyx.api]
            [cljsjs.vis]))


(defn vis-opts [props]
  (let [{:keys [graph-direction graph-selectable]
         :or {graph-direction "UD"
              graph-selectable true}} (:layout/hints props)
        opts {:nodes {:shape "dot"}
              :edges {:arrows "to"}
              :layout {:hierarchical {:enabled true
                                      :sortMethod "directed"
                                      :direction graph-direction}}
              :interaction {:zoomView false
                            :dragView false
                            :dragNodes false
                            :selectable graph-selectable
                            :multiselect graph-selectable}}]
    (clj->js opts)))

(defn vis-data [workflow]
  (let [nodes (->> workflow
                   (flatten)
                   (set)
                   (map (fn [task] {:id (name task) :label (name task)}))
                   (into [])
                   (clj->js)
                   (js/vis.DataSet.))
        edges (->> workflow
                   (map (fn [edge]
                          (let [[a b] (map name edge)]
                            {:from a
                             :to b})))
                   (into [])
                   (clj->js)
                   (js/vis.DataSet.))]
    #js {:nodes nodes
         :edges edges}))

(defn vis-graph [graph-id {:keys [link/evaluations] :as props}]
  (let [el (gdom/getElement graph-id)
        workflow (get-in evaluations [:workflow :result :value])]
    (js/vis.Network. el (vis-data workflow) (vis-opts props))))

(defmulti transition! (fn [component graph params]
                        (:action params)))

(defmethod transition! :reset
  [component graph params]
  (.unselectAll graph)
  (.releaseNode graph))

(defmethod transition! :select-tasks
  [component graph params]
  (.selectNodes graph
                (->> params
                     :tasks
                     (map name)
                     (clj->js))
                false))
 
(defmethod transition! :update-workflow
  [component graph params]
  (.setData graph (vis-data (:workflow params))))

(defmethod transition! :canvas-did-redraw
  [component graph {:keys [canvas-context]}]
  (om/update-state! component assoc :canvas-context canvas-context))

(defn segviz! [component]
  (if-let [segviz (om/get-state component :segviz)]
    segviz
    (let [graph-canvas-context (om/get-state component :canvas-context)
          _ (assert graph-canvas-context "Canvas context must be added to state after graph draws.")
          graph-canvas (.-canvas graph-canvas-context)
          segviz-canvas (doto (js/document.createElement "canvas")
                          (gdom/setProperties #js {:width (.-clientWidth graph-canvas)
                                                   :height (.-clientHeight graph-canvas)}))
          _ (js/console.log "svc" segviz-canvas)
          segviz (canvas/init segviz-canvas)]
      (om/update-state! component :segviz segviz)
      segviz))
)

(defmethod transition! :update-segment-visualization
  [component graph {:keys [job-env]}]
  (let [env-summary (onyx.api/env-summary job-env)
        segviz (segviz! component)]
    (js/console.log "---" segviz))
  )

(defn target-tasks [vis-evt]
  (into [] (map keyword (.-nodes vis-evt))))

(defn graph-id [props]
  (str (helpers/component-id props)
       "-graph"))

(defn keep-update [prev curr fselect]
  (let [curr-val (fselect curr)]
    (when (not= curr-val (fselect prev))
      curr-val)))

(defui Graph
  static om/IQuery
  (query [this]
    [:component/id :component/type :content/label :content/graph-direction
     :link/evaluations :layout/hints :ui-state/shared])
  
  Object
  (componentDidMount [this]
    (let [{:keys [component/id ui-state/shared] :as props} (om/props this)
          graph (vis-graph (graph-id props) props)
          shared-state-on-mount? ^boolean shared]
      (.on graph
           "selectNode"
           (fn [vis-evt]
             (om/transact! this `[(ui-state/update {:id ~id
                                                    :params {:action :select-tasks
                                                             :tasks ~(target-tasks vis-evt)}})
                                  :blueprint/sections])))

      (.on graph
           "deselectNode"
           (fn [vis-evt]
             (om/transact! this `[(ui-state/update {:id ~id
                                                    :params {:action :deselect-tasks
                                                             :tasks ~(target-tasks vis-evt)}})
                                  :blueprint/sections])))

      (.on graph
           "afterDrawing"
           (fn [canvas-context]
             (transition! this graph {:action :canvas-did-redraw
                                      :canvas-context canvas-context})))

      (when shared-state-on-mount?
        (transition! this graph shared))
      
      (om/set-state! this {:graph graph})))

  (componentDidUpdate [this prev-props prev-state]
    (let [props (om/props this)
          graph (om/get-state this :graph)]
      (when-let [updated-workflow
                 (keep-update prev-props props #(-> %
                                                    :link/evaluations
                                                    :workflow
                                                    :result
                                                    :value))]
        (transition! this graph {:action :update-workflow
                                 :workflow updated-workflow}))

      (when-let [updated-job
                 (keep-update prev-props props #(-> %
                                                    :link/evaluations
                                                    :job-env
                                                    :result
                                                    :value))]
        (transition! this graph {:action :update-segment-visualization
                                 :job-env updated-job}))))

  (render [this]
    (let [props (om/props this)]
      (dom/div #js {:id (helpers/component-id props)
                    :className (helpers/component-css-classes props)}
               (helpers/label props)
               (dom/div #js {:id (graph-id props)
                             :className "graph-container"})))))

(def graph (om/factory Graph))

(defmethod extensions/component-ui :blueprint/graph [props]
  (graph props))
