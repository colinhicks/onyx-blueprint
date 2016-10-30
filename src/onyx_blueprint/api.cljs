(ns onyx-blueprint.api
  (:require [cljs.pprint :as pprint]
            [om.next :as om]
            [onyx-blueprint.extensions :as extensions]
            [onyx-blueprint.ui.core :as ui]
            [onyx-blueprint.parser]
            [onyx-blueprint.self-host :as self-host]))

(defn compile* [{:keys [source script-id component-id]} cb]
  (self-host/eval-str
   (str source)
   script-id
   (fn [result]
     (cb {:component/id component-id
          :result result
          :state (if (seq (:warnings result))
                   :compiled-error
                   :compiled-success)}))))

(defn io-compile [cb {:keys [component-id] :as eval-req}]
  (compile* eval-req (fn [result]
                       ;; todo: is there a better way to do this?
                       (cb {:onyx-blueprint/merge-in
                            {:content result
                             :keypath [:tutorial/compilations component-id]}}))))

(defn io [{:keys [compile]} cb]
  (when compile
    (->> compile
         (om/query->ast)
         :children
         (map :params)
         (run! (partial io-compile cb)))))

(defn merge-tree [st {:keys [onyx-blueprint/merge-in] :as novelty}]
  (let [merge-result (if merge-in
                       (update-in st (:keypath merge-in)
                                  #(merge % (:content merge-in)))
                       (om/default-merge-tree st novelty))]
    ;;(do (println "-MR-") (pprint/pprint merge-result))    
    merge-result))

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

(def run-async! #'cljs.js/run-async!)

(defn compile-default-input! [components done-cb]
  (let [compilations (->> components
                          (filter #(= :editor (keyword (namespace (:component/type %)))))
                          (map #(vector (:component/id %)
                                        (get-in % [:component/content :default-input])))
                          (filter second))
        results (atom {})]
    (run-async!
     (fn [[id default-input] cb]
       (compile* {:component-id id
                  :script-id (str (name id) "-initial-value")
                  :source default-input}
                 (fn [result]
                   (swap! results assoc-in [id] result)
                   (cb nil))))
     compilations
     :unused
     (fn [error-result]
       (done-cb @results)))))

(defn render-tutorial! [components sections target-el]
  ;; todo alternative to blocking here?
  (compile-default-input!
   components
   (fn [compilations]
     (let [init-data {:tutorial/sections (into-tree components sections)}
           normalized-data (assoc (om/tree->db ui/Tutorial init-data true)
                                  :tutorial/compilations compilations)
           reconciler (om/reconciler
                       {:state (atom normalized-data)
                        :parser (om/parser {:read extensions/parser-read
                                            :mutate extensions/parser-mutate})
                        :send io
                        :merge-tree merge-tree
                        :remotes [:compile]})]

       ;;(pprint/pprint @reconciler)

       (om/add-root! reconciler ui/Tutorial target-el)))))
