(ns onyx-tutorial.ui.graph
  (:require [cljs.pprint :as pprint]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [onyx-tutorial.extensions :as extensions]))

(defn graph-id [id]
  (str (name id) "-graph"))

(defui Graph
  static om/Ident
  (ident [this {:keys [component/content]}]
    [:tutorial/components (get-in content [:link :id])])
    
  static om/IQuery
  (query [this]
    ;; todo fix
    [[:tutorial/components :onyx-tutorial.core/workflow-editor]])
    
  Object
  (componentDidMount [this]
    (let [{:keys [component/id component/content]} (om/props this)
          ]
      ))
  
  (render [this]
    (let [{:keys [component/id] :as props} (om/props this)
          _ (println "***")
          _ (pprint/pprint props)
          graph-id (graph-id id)]
      (dom/div #js {:id graph-id} graph-id))))

(def graph (om/factory Graph))

(defmethod extensions/component-ui :graph/simple [props]
  (graph props))
