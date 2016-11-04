(ns onyx-blueprint.ui.graph
  (:require [cljs.pprint :as pprint]
            [com.stuartsierra.dependency :as dep]
            [goog.dom :as gdom]
            [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            [onyx-blueprint.extensions :as extensions]
            [onyx-blueprint.ui.helpers :as helpers]))

(defn graph-id [id]
  (str (name id) "-graph"))

(defn to-dependency-graph [workflow]
  (reduce (fn [g edge]
            (apply dep/depend g (reverse edge)))
          (dep/graph) workflow))

(defn props->cytoscape-opts [{:keys [component/id evaluations/link]}]
  (let [workflow (-> link :workflow :result :value)
        nodes (->> workflow
                   (flatten)
                   (set)
                   (map (fn [task] {:data {:id (name task)}}))
                   (into []))
        edges (->> workflow
                   (map (fn [edge]
                          (let [[a b] (map name edge)]
                               {:data {:id (str a b)
                                       :source a
                                       :target b}}))))]
    {:container (gdom/getElement (graph-id id))
     :elements (into nodes edges)
     :style [{:selector "node"
              :style {:background-color "#4d6884"
                      :font-family "Noto Serif, DejaVu Serif, serif"
                      :label "data(id)"}}
             {:selector "edge"
              :style {:width 3
                      :line-color "#ccc"
                      :target-arrow-color "#ccc"
                      :target-arrow-shape "triangle"}}]
     :layout {:name "breadthfirst"
              :directed true
              :padding 10}
     :userZoomingEnabled false
     :userPanningEnabled false}))

(defn render-graph! [props]
  (-> props
      (props->cytoscape-opts)
      (clj->js)
      (js/cytoscape)))

(defui Graph
  static om/IQuery
  (query [this]
    [:component/id :component/type :evaluations/link :layout/hints])
    
  Object
  (componentDidMount [this]
    (render-graph! (om/props this)))

  (componentDidUpdate [this pprops pstate]
    ;; todo diff
    (render-graph! (om/props this)))
  
  (render [this]
    (let [{:keys [component/id layout/hints] :as props} (om/props this)
          graph-id (graph-id id)]
      (dom/div #js {:id graph-id :className (helpers/component-css-classes props)}))))

(def graph (om/factory Graph))

(defmethod extensions/component-ui :graph/workflow [props]
  (graph props))
