(ns onyx-blueprint.ui.graph
  (:require [cljs.pprint :as pprint]
            [goog.dom :as gdom]
            [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            [onyx-blueprint.extensions :as extensions]
            [onyx-blueprint.ui.helpers :as helpers]
            [cljsjs.vis]))



(defn vis-opts [{:keys [content/graph-direction content/graph-selectable]
                 :or {graph-direction "UD"
                      graph-selectable true}}]
  (let [opts {:edges {:arrows "to"}
              :layout {:hierarchical {:enabled true
                                      :sortMethod "directed"
                                      :direction graph-direction}}
              :interaction {:zoomView false
                            :dragView false
                            :dragNodes false
                            :selectable graph-selectable}}]
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

(defn vis-graph [graph-id {:keys [evaluations/link] :as props}]
  (let [el (gdom/getElement graph-id)
        workflow (get-in link [:workflow :result :value])]
    (js/vis.Network. el (vis-data workflow) (vis-opts props))))

(defmulti transition! (fn [target command params]
                        command))


(defmethod transition! :reset
  [target command params]
  (.unselectAll target)
  (.releaseNode target))

(defmethod transition! :focus
  [target command params]
  (.focus target (:node-id params)))

(defmethod transition! :update-workflow
  [target command params]
  (.setData target (vis-data (:workflow params))))


(defui Graph
  static om/IQuery
  (query [this]
    [:component/id :component/type :content/graph-direction :evaluations/link :layout/hints])
  
  Object
  (componentDidMount [this]
    (let [props (om/props this)
          graph (vis-graph (helpers/component-id props) props)]
      (om/set-state! this {:graph graph})))

  (componentDidUpdate [this prev-props prev-state]
    (let [props (om/props this)
          graph (om/get-state this :graph)
          prev-workflow (get-in prev-props [:evaluations/link :workflow :result :value])
          curr-workflow (get-in props [:evaluations/link :workflow :result :value])]
      (cond
        (not= prev-workflow curr-workflow)
        (transition! graph :update-workflow {:workflow curr-workflow}))))

  (render [this]
    (let [props (om/props this)]
      (dom/div #js {:id (helpers/component-id props)
                    :className (helpers/component-css-classes props)}))))

(def graph (om/factory Graph))

(defmethod extensions/component-ui :graph/workflow [props]
  (graph props))
