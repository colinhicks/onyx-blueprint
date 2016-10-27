(ns onyx-tutorial.api
  (:require [cljs.pprint :as pprint]
            [om.next :as om]
            [onyx-local-rt.api :as onyx.api]
            [onyx-tutorial.extensions :as extensions]
            [onyx-tutorial.self-host :as self-host]
            [onyx-tutorial.ui.core :as ui]
            [onyx-tutorial.parser]))


(defn io-compile [cb {:keys [source script-id component-id]}]
  (self-host/eval-str
   source
   script-id
   (fn [result]
     ;; todo: is there a better way to do this?
     (let [merge-payload {:onyx-tutorial/merge-in
                          {:content {:compile-result result
                                     :compile-state (if (seq (:warnings result))
                                                      :compiled-error
                                                      :compiled-success)}
                           :keypath [:tutorial/components
                                     component-id
                                     :component/content]}}]
       (cb merge-payload)))))

(defn io [{:keys [compile]} cb]
  (when compile
    (->> compile
         (om/query->ast)
         :children
         (map :params)
         (run! (partial io-compile cb)))))

(defn merge-tree [st {:keys [onyx-tutorial/merge-in] :as novelty}]
  (if merge-in
    (update-in st (:keypath merge-in)
               #(merge % (:content merge-in)))
    (om/default-merge-tree st novelty)))

(defn into-tree [components layouts]
  (mapv (fn [{:keys [section/id section/layout]}]
          {:section/id id
           :section/rows
           (mapv (fn [items]
                   {:row/items (mapv
                            (fn [cid]
                              (let [component (some #(when (= cid (:component/id %)) %) components)]
                                (assoc component :section/id id)))
                            items)})
                 layout)})
        layouts))

(defn render-tutorial! [components sections target-el]
  (let [init-data {:tutorial/sections (into-tree components sections)}
        normalized-data (om/tree->db ui/Tutorial init-data true)
        reconciler (om/reconciler
                    {:state (atom normalized-data)
                     :parser (om/parser {:read extensions/parser-read
                                         :mutate extensions/parser-mutate})
                     :send io
                     :merge-tree merge-tree
                     :remotes [:compile]})]

    (om/add-root! reconciler ui/Tutorial target-el)))
