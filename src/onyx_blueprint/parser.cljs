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
    {:value (get-in @state [:tutorial/compilations target-id])}))

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
                                          (get query type-ns)
                                          query)]
                      (doparse parser env focused-query c))))
             (into []))]
    {:value parsed-items}))

(defmethod parser-read :section/rows
  [{:keys [state query parser sections data] :as env} key _]
  (let [rows (get data key)]
    {:value (mapv (partial doparse parser env query) rows)}))

(defmethod parser-read :tutorial/sections
  [{:keys [state query parser] :as env} key params]
  (let [st @state
        sections (get st key)]
    {:value (mapv (partial doparse parser env query) sections)}))

(defmethod parser-mutate 'editor/eval
  [{:keys [state] :as env} key {:keys [type source script-id component-id] :as params}]
  {:compile true})

(defmethod parser-mutate 'onyx/init
  [{:keys [state] :as env} key {:keys [id job]}]
  {:action (fn []
             (swap! state assoc-in
                    [:tutorial/components id :component/content :job]
                    (onyx.api/init job)))})

(defmethod parser-mutate 'onyx/new-segment
  [{:keys [state] :as env} key {:keys [id]}]
  {:action (fn []
             (swap! state update-in
                    [:tutorial/components id :component/content]
                    (fn [{:keys [gen-segment] :as content}]
                      (update-in content [:job]
                                 #(onyx.api/new-segment % :in (gen-segment))))))})


(defmethod parser-mutate 'onyx/tick
  [{:keys [state] :as env} key {:keys [id]}]
  {:action (fn []
             (swap! state update-in
                    [:tutorial/components id :component/content :job]
                    #(onyx.api/tick %)))})

(defmethod parser-mutate 'onyx/drain
  [{:keys [state] :as env} key {:keys [id]}]
  {:action (fn []
             (swap! state update-in
                    [:tutorial/components id :component/content :job]
                    #(-> %
                         (onyx.api/drain)
                         (onyx.api/stop))))})
