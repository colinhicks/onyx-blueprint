(ns onyx-blueprint.api
  (:require [cljs.pprint :as pprint]
            [om.next :as om]
            [onyx-blueprint.extensions :as extensions]
            [onyx-blueprint.ui.core :as ui]
            [onyx-blueprint.parser]
            [onyx-blueprint.self-host :as self-host]
            [onyx-blueprint.validation]))

(defn evaluate* [{:keys [source script-id component-id validate-spec]} cb]
  (self-host/eval-str
   (str source)
   script-id
   (fn [result]
     (let [^boolean success? (not (seq (:warnings result)))]
       (cb {:component/id component-id
            :result result
            :state (if success? :success :error)
            :validation (if (and success? validate-spec)
                          (extensions/validate validate-spec (:value result)))})))))

(defn io-evaluate [cb {:keys [component-id] :as eval-req}]
  (evaluate* eval-req (fn [evaluation]
                       ;; todo: is there a better way to do this?
                       (cb {:onyx-blueprint/merge-in
                            {:content evaluation
                             :keypath [:blueprint/evaluations component-id]}}))))

(defn io [{:keys [evaluate]} cb]
  (when evaluate
    (->> evaluate
         (om/query->ast)
         :children
         (map :params)
         (run! (partial io-evaluate cb)))))

(defn merge-tree [st {:keys [onyx-blueprint/merge-in] :as novelty}]
  (let [merge-result (if merge-in
                       (update-in st (:keypath merge-in)
                                  #(merge % (:content merge-in)))
                       (om/default-merge-tree st novelty))]
    ;;(do (println "-MR-") (pprint/pprint merge-result))    
    merge-result))

(defn into-tree [components layouts]
  (->> layouts
       (map (fn [{:keys [section/id section/layout]}]
              {:section/id id
               :section/rows
               (->> layout
                    (map (fn [row-expr]
                           (let [items (-> row-expr om/query->ast :children)]
                             {:row/items
                              (->> items
                                   (map
                                    (fn [{:keys [key params]}]
                                      (if-let [component (some
                                                          #(when (keyword-identical? key (:component/id %)) %)
                                                          components)]
                                        (assoc component
                                               :section/id id
                                               :layout/hints params)
                                        {:component/id key
                                         :component/type :error/not-found})))
                                   (into []))})))
                    (into []))}))
       (into [])))

(def run-async! #'cljs.js/run-async!)

(defn initial-evaluations! [components done-cb]
  (let [inits (keep (fn [{:keys [component/id evaluations/init] :as c}]
                      (when init [id (get c init)]))
                    components)
        results (atom {})]
    (run-async!
     (fn [[id init-input] cb]
       (evaluate* {:component-id id
                   :script-id (str (name id) "-initial-value")
                   :source init-input}
                  (fn [result]
                    (swap! results assoc-in [id] result)
                    (cb nil))))
     inits
     :unused
     (fn [error-result]
       (done-cb @results)))))

(defn render-tutorial!
  ([components sections target-el]
   (render-tutorial! components sections {} target-el))
  ([components sections {:keys [custom-component-queries] :as opts} target-el]
   ;; todo alternative to blocking here?
   (initial-evaluations!
    components
    (fn [evaluations]
      (binding [extensions/*custom-component-queries* custom-component-queries]
        (let [init-data {:blueprint/sections (into-tree components sections)}
              normalized-data (assoc (om/tree->db ui/Tutorial init-data true)
                                     :blueprint/evaluations evaluations)
              reconciler (om/reconciler
                          {:state (atom normalized-data)
                           :parser (om/parser {:read extensions/parser-read
                                               :mutate extensions/parser-mutate})
                           :send io
                           :merge-tree merge-tree
                           :remotes [:evaluate]})]

          ;;(pprint/pprint @reconciler)
          (om/add-root! reconciler ui/Tutorial target-el)))))))
