(ns onyx-blueprint.parser
  (:require [cljs.pprint :as pprint]
            [goog.date]
            [om.next :as om]
            [onyx-blueprint.extensions :refer [parser-mutate parser-read]]
            [onyx-local-rt.api :as onyx.api])
  (:import [goog.date UtcDateTime]))

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

(defmethod parser-read :ui-state/shared
  [{:keys [state query parser data] :as env} key _]
  {:value (get-in @state [:blueprint/ui-state (:component/id data)])})

(defmethod parser-read :row/items
  [{:keys [state query parser data] :as env} key _]
  (let [st @state
        parsed-items
        (->> (get data key)
             ;; resolve
             (map (partial get-in st))
             ;; parse
             (map (fn [{:keys [component/type] :as c}]
                    (let [focused-query (if (map? query)
                                          (get query type [:component/id :component/type])
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

(defn stamp [m]
  (assoc m :timestamp
         (.getTime (goog.date.UtcDateTime.))))

(defn stamped-summary [job-env]
  (stamp (onyx.api/env-summary job-env)))

(defmethod parser-mutate 'onyx/init
  [{:keys [state] :as env} key {:keys [id job input-segments]}]
  (let [checkpoints? (get-in @state [:blueprint/components id :simulator/checkpoints?])
        input-segments? (seq input-segments)
        job-env (cond-> (onyx.api/init job)
                  checkpoints?
                  (as-> x (assoc x
                                 :uuid (random-uuid)
                                 :checkpoints [(stamped-summary x)]))
                  
                  input-segments?
                  (as-> x (add-segments x (-> x :sorted-tasks first) input-segments))

                  (and input-segments? checkpoints?)
                  (as-> x (update x :checkpoints conj (stamped-summary x))))]
    {:action (fn []
               (swap! state assoc-in
                      [:blueprint/evaluations id]
                      ;; todo common record with api/evaluate* result
                      (job-evaluation id (stamp job-env))))}))

(defn tick-summarizing-batches [{:keys [next-action checkpoints] :as job-env}]
  (let [drained? (onyx.api/drained? job-env)
        job-env' (assoc job-env :drained? drained?)]
    (if (and (seq checkpoints)
             (keyword-identical? :lifecycle/after-batch next-action)
             (not drained?))
      (onyx.api/tick (update-in job-env' [:checkpoints]
                                conj (stamped-summary job-env)))
      (onyx.api/tick job-env'))))

(defn drain-summarizing-batches [job-env max-ticks]
  (loop [env job-env
         i 0]
    (cond
      (> i max-ticks)
      (throw (ex-info
              (str
               "Ticked " max-ticks " times and never drained, runtime will not proceed with further execution.")
              {}))

      (onyx.api/drained? env)
      env

      :else
      (recur (tick-summarizing-batches env) (inc i)))))

(defmethod parser-mutate 'onyx/tick
  [{:keys [state] :as env} key {:keys [id]}]
  {:action (fn []
             (swap! state update-in
                    [:blueprint/evaluations id :result :value]
                    tick-summarizing-batches))})

(defmethod parser-mutate 'onyx/next-batch
  [{:keys [state] :as env} key {:keys [id]}]
  {:action (fn []
             (swap! state update-in
                    [:blueprint/evaluations id :result :value]
                    (fn [job-env]
                      (loop [jenv job-env
                             action (:next-action job-env)]
                        (if (or (keyword-identical? :lifecycle/after-batch action)
                                (:drained? jenv))
                          (stamp jenv)
                          (recur (tick-summarizing-batches jenv)
                                 (:next-action jenv)))))))})

(defmethod parser-mutate 'onyx/drain
  [{:keys [state] :as env} key {:keys [id]}]
  {:action (fn []
             (swap! state update-in
                    [:blueprint/evaluations id :result :value]
                    #(-> %
                         (drain-summarizing-batches 10000)
                         (onyx.api/stop)
                         (stamp))))})

(defmethod parser-mutate 'ui-state/update
  [{:keys [state] :as env} key {:keys [id params]}]
  {:action (fn []
             (swap! state update-in
                    [:blueprint/ui-state id]
                    #(merge % params)))})
