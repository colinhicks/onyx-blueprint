(ns onyx-blueprint.parser
  (:require [cljs.pprint :as pprint]
            [om.next :as om]
            [onyx-blueprint.extensions :refer [parser-mutate parser-read]]
            [onyx-local-rt.api :as onyx.api]))

(defn doparse [parser env query data]
  (parser (assoc env :data data) query))

(defmethod parser-read :default
  [{:keys [state data] :as env} key _]
  (let [data (or data @state)]
    {:value (get data key)}))

(defn resolve-link [link source]
  (reduce-kv (fn [m k linkref]
               (if (map? linkref)
                 (assoc m k (resolve-link linkref source))
                 (assoc m k (get-in source (if (vector? linkref)
                                             linkref
                                             [linkref])))))
             {}
             link))

(defmethod parser-read :link/ui-state
  [{:keys [state query parser data] :as env} key _]
  (let [link (get data key)]
    {:value (resolve-link link (:blueprint/ui-state @state))}))

(defmethod parser-read :link/evaluations
  [{:keys [state query parser data] :as env} key _]
  (let [link (get data key)]
    {:value (resolve-link link (:blueprint/evaluations @state))}))

;; obsolete
(defmethod parser-read :evaluations/link
  [{:keys [state query parser data] :as env} key _]
  (let [link (get data key)]
    {:value (resolve-link link (:blueprint/evaluations @state))}))

(defmethod parser-read :row/items
  [{:keys [state query parser data] :as env} key _]
  (let [st @state
        parsed-items
        (->> (get data key)
             ;; resolve
             (map (partial get-in st))
             ;; parse
             (map (fn [{:keys [component/type] :as c}]
                    (let [type-ns (keyword (namespace type))
                          ;; todo: less weird
                          focused-query (if (map? query)
                                          (get query type-ns [:component/id :component/type])
                                          query)]
                      (doparse parser env focused-query c))))
             (into []))]
    {:value parsed-items}))

(defmethod parser-read :section/rows
  [{:keys [state query parser sections data] :as env} key _]
  (let [rows (get data key)]
    {:value (into [] (map (partial doparse parser env query) rows))}))

(defmethod parser-read :blueprint/sections
  [{:keys [state query parser] :as env} key params]
  (let [st @state
        sections (get st key)]
    {:value (into [] (map (partial doparse parser env query) sections))}))

(defmethod parser-mutate 'evaluations/evaluate
  [{:keys [state ast] :as env} key {:keys [component-id] :as params}]
  (let [validate-spec
        (get-in @state [:blueprint/components component-id :evaluations/validate-spec])]
    {:evaluate (update-in ast [:params] #(assoc % :validate-spec validate-spec))}))

(defn job-evaluation [id job-env]
  {:component/id id
   :result {:value job-env}
   :warnings []
   :state :success})

(defn add-segments [job-env task segments]
  (reduce (fn [je seg]
            (onyx.api/new-segment je task seg))
          job-env
          segments))

(defmethod parser-mutate 'onyx/init
  [{:keys [state] :as env} key {:keys [id job input-segments]}]
  (let [job-env (if (seq input-segments)
                  (add-segments (onyx.api/init job)
                                (-> job :workflow ffirst)
                                input-segments)
                  (onyx.api/init job))]
    {:action (fn []
               (swap! state assoc-in
                      [:blueprint/evaluations id]
                      ;; todo common record with api/evaluate* result
                      (job-evaluation id job-env)))}))

(defmethod parser-mutate 'onyx/new-segment
  [{:keys [state] :as env} key {:keys [id]}]
  (let [gen-segment (get-in @state [:blueprint/components id :simulator/gen-segment])]
    {:action (fn []
               (swap! state update-in
                      [:blueprint/evaluations id :result :value]
                      #(onyx.api/new-segment % :in (gen-segment))))}))

(defmethod parser-mutate 'onyx/tick
  [{:keys [state] :as env} key {:keys [id]}]
  {:action (fn []
             (swap! state update-in
                    [:blueprint/evaluations id :result :value]
                    #(onyx.api/tick %)))})

(defmethod parser-mutate 'onyx/next-batch
  [{:keys [state] :as env} key {:keys [id]}]
  {:action (fn []
             (swap! state update-in
                    [:blueprint/evaluations id :result :value]
                    (fn [job-env]
                      (loop [jenv job-env
                             action (:next-action job-env)]
                        (if (or (keyword-identical? :lifecycle/after-batch action)
                                (onyx.api/drained? jenv))
                          jenv
                          (recur (onyx.api/tick jenv)
                                 (:next-action jenv)))))))})

(defmethod parser-mutate 'onyx/drain
  [{:keys [state] :as env} key {:keys [id]}]
  {:action (fn []
             (swap! state update-in
                    [:blueprint/evaluations id :result :value]
                    #(-> %
                         (onyx.api/drain)
                         (onyx.api/stop))))})

(defmethod parser-mutate 'ui-state/update
  [{:keys [state] :as env} key {:keys [id params]}]
  {:action (fn []
             (swap! state update-in
                    [:blueprint/ui-state id]
                    #(merge % params)))})
