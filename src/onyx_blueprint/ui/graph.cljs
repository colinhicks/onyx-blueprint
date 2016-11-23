(ns onyx-blueprint.ui.graph
  (:require [cljs.pprint :as pprint]
            [goog.dom :as gdom]
            [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            [onyx-blueprint.extensions :as extensions]
            [onyx-blueprint.ui.helpers :as helpers]
            [cljsjs.vis]))



(defn vis-opts [props]
  (let [{:keys [graph-direction graph-selectable]
         :or {graph-direction "UD"
              graph-selectable true}} (:layout/hints props)
        opts {:autoResize false
              :nodes {:shape "dot"}
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

(defmulti transition! (fn [graph params]
                        (:action params)))

(defmethod transition! :reset
  [graph params]
  (.unselectAll graph)
  (.releaseNode graph))

(defmethod transition! :select-tasks
  [graph params]
  (.selectNodes graph
                (->> params
                     :tasks
                     (map name)
                     (clj->js))
                false))

(defmethod transition! :update-workflow
  [graph params]
  (.setData graph (vis-data (:workflow params))))

(defn target-tasks [vis-evt]
  (into [] (map keyword (.-nodes vis-evt))))

(defn graph-id [props]
  (str (helpers/component-id props)
       "-graph"))

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

      (when shared-state-on-mount?
        (transition! graph shared))
      
      (om/set-state! this {:graph graph})))

  (componentDidUpdate [this prev-props prev-state]
    (let [props (om/props this)
          graph (om/get-state this :graph)
          prev-workflow (get-in prev-props [:link/evaluations :workflow :result :value])
          curr-workflow (get-in props [:link/evaluations :workflow :result :value])]
      (when (not= prev-workflow curr-workflow)
        (transition! graph {:action :update-workflow
                            :workflow curr-workflow}))))

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
