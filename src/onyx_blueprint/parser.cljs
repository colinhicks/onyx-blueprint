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

(defmethod parser-read :component/target
  [{:keys [state query parser data] :as env} key _]
  (let [target-id (get data key)]
    {:value (get-in @state [:blueprint/evaluations target-id])}))

(defn resolve-evaluations [link state]
  (reduce-kv (fn [m k linkref]
               (if (map? linkref)
                 (assoc m k (resolve-evaluations linkref state))
                 (assoc m k (get-in state [:blueprint/evaluations linkref]))))
             {}
             link))

(defmethod parser-read :evaluations/link
  [{:keys [state query parser data] :as env} key _]
  (let [link (get data key)
        st @state]
    {:value (resolve-evaluations link @state)}))

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

(defmethod parser-mutate 'onyx/init
  [{:keys [state] :as env} key {:keys [id job]}]
  (let [job-env (onyx.api/init job)]
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

(defmethod parser-mutate 'onyx/drain
  [{:keys [state] :as env} key {:keys [id]}]
  {:action (fn []
             (swap! state update-in
                    [:blueprint/evaluations id :result :value]
                    #(-> %
                         (onyx.api/drain)
                         (onyx.api/stop))))})

(defmethod parser-mutate 'onyx/init+batch+drain
  [{:keys [state] :as env} key {:keys [id job input-batch]}]
  (let [input-task (-> job :workflow ffirst)
        hydrated-env (reduce (fn [j seg]
                               (onyx.api/new-segment j input-task seg))
                             (onyx.api/init job)
                             input-batch)
        job-env (-> hydrated-env
                    (onyx.api/drain)
                    (onyx.api/stop))]
    {:action (fn []
               (swap! state assoc-in
                      [:blueprint/evaluations id]
                      (job-evaluation id job-env)))}))
