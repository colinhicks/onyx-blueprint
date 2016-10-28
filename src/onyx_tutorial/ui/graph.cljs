(ns onyx-tutorial.ui.graph
  (:require [cljs.pprint :as pprint]
            [com.stuartsierra.dependency :as dep]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [goog.dom :as gdom]
            [onyx-tutorial.extensions :as extensions]))


(defn graph-id [id]
  (str (name id) "-graph"))

(defn to-dependency-graph [workflow]
  (reduce (fn [g edge]
            (apply dep/depend g (reverse edge)))
          (dep/graph) workflow))

(defn props->cytoscape-opts [{:keys [component/id component/target]}]
  (let [workflow (-> target :result :value)
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
              :style {:background-color "#666"
                      :label "data(id)"}}
             {:selector "edge"
              :style {:width 3
                      :line-color "#ccc"
                      :target-arrow-color "#ccc"
                      :target-arrow-shape "triangle"}}]
     :layout {:name "breadthfirst"
              :directed true
              :animate true}}))

(defn render-graph! [props]
  (-> props
      (props->cytoscape-opts)
      (clj->js)
      (js/cytoscape)))

(defui Graph
  static om/IQuery
  (query [this]
    [:component/id :component/type :component/target])
    
  Object
  (componentDidMount [this]
    (render-graph! (om/props this)))

  (componentDidUpdate [this pprops pstate]
    ;; todo diff
    (render-graph! (om/props this)))
  
  (render [this]
    (let [{:keys [component/id] :as props} (om/props this)
          graph-id (graph-id id)]
      (dom/div #js {:id graph-id :className "component component-graph"}))))

(def graph (om/factory Graph))

(defmethod extensions/component-ui :graph/simple [props]
  (graph props))
