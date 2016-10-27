(ns onyx-tutorial.parser
  (:require [om.next :as om]
            [onyx-tutorial.extensions :refer [parser-read parser-mutate]]))


(defmethod parser-read :default
  [{:keys [state query parser] :as env} key params]
  (println "--read-miss" key query)
  {:value :not-found})

(defn denormalize-section [st section]
  (update-in section [:section/rows]
             (fn [rows]
               (mapv (fn [row]
                       (update-in row [:row/items]
                                  (fn [items]
                                    (mapv (fn [item-ref]
                                            (get-in st item-ref))
                                          items))))
                     rows))))

(defmethod parser-read :tutorial/sections
  [{:keys [state query parser target] :as env} key params]
  (let [st @state
        sections (mapv (partial denormalize-section st) (key st))]
    {:value sections}))

(defmethod parser-mutate 'editor/eval
  [{:keys [state] :as env} key {:keys [type source script-id component-id] :as params}]
  {:value {:keys []}
   :compile true
   :action #(swap! state assoc-in
                   [:tutorial/components
                    component-id
                    :component/content
                    :compile-state]
                   :compiling)})
