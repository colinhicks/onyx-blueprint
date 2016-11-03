(ns onyx-blueprint.ui.simulator
  (:require [cljs.pprint :as pprint]
            [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            [onyx-blueprint.extensions :as extensions]
            [onyx-blueprint.ui.code-editor :as code-editor]
            [onyx-blueprint.ui.helpers :as helpers]
            [onyx-local-rt.api :as onyx.api]))

(defn button [label cb]
  (dom/input #js {:value label
                  :type "button"
                  :onClick cb}))

;; todo refactor
(defn basic-simulator [component]
  (let [{:keys [component/id evaluations/link] :as props} (om/props component)
        init-data (-> link :init-data :result :value)
        job-env (-> link :job-env :result :value)
        valid-user-fn? (= :success (-> link :user-fn :state))
        env-summary (if job-env (onyx.api/env-summary job-env))
        transact! (partial om/transact! component)]
    (apply dom/div #js {:id (name id) :className (helpers/component-css-classes props)}
           (cond-> []
             (not valid-user-fn?)
             (conj (dom/pre nil (with-out-str (pprint/pprint (-> link :user-fn :result :warnings)))))
             
             (and valid-user-fn? (nil? job-env))
             (conj (button "Initialize job"
                           (fn [evt]
                             (.preventDefault evt)
                             (transact! `[(onyx/init {:id ~id :job ~init-data})]))))
             (and valid-user-fn? job-env)
             (conj (button "Reinitialize job"
                           (fn [evt]
                             (.preventDefault evt)
                             (transact! `[(onyx/init {:id ~id :job ~init-data})])))
                   (button "Generate input"
                           (fn [evt]
                             (.preventDefault evt)
                             (transact! `[(onyx/new-segment {:id ~id})])))
                   (button ">"
                           (fn [evt]
                             (.preventDefault evt)
                             (transact! `[(onyx/tick {:id ~id})])))
                   (button ">>"
                           (fn [evt]
                             (.preventDefault evt)
                             (transact! `[(onyx/drain {:id ~id})])))                       
                   (dom/pre nil (with-out-str (pprint/pprint env-summary))))))))

(defn batch-outputs-simulator [component]
  (let [{:keys [component/id evaluations/link] :as props} (om/props component)
        ;; todo util respective of parser/resolve-evaluations
        init-data (reduce-kv (fn [m k v]
                               (assoc m k (-> v :result :value)))
                             {}
                             (:init-data link))
        job-env (-> link :job-env :result :value)
        env-summary (if job-env (onyx.api/env-summary job-env))
        input-task (-> init-data :workflow ffirst)
        input-batch (-> link :input-segments :result :value)
        outputs (-> env-summary
                     :tasks
                     (get (second (last (:workflow init-data))))
                     :outputs)
        transact! (partial om/transact! component)]
    (apply dom/div #js {:id (name id) :className (helpers/component-css-classes props)}
           (cond-> []
             (nil? job-env)
             (conj (button "Initialize job"
                           (fn [evt]
                             (.preventDefault evt)
                             (transact! `[(onyx/init {:id ~id :job ~init-data})]))))

             job-env
             (conj (button "Reset job"
                           (fn [evt]
                             (.preventDefault evt)
                             (transact! `[(onyx/init {:id ~id :job ~init-data})])))
                   (button "Process input batch"
                           (fn [evt]
                             (.preventDefault evt)
                             (transact! `[(onyx/batch-drain {:id ~id
                                                             :input-task ~input-task
                                                             :input-batch ~input-batch})])))
                   (dom/pre nil (with-out-str (pprint/pprint outputs))))))))

(defui Simulator
  static om/IQuery
  (query [this]
    [:component/id :component/type :evaluations/link])
  
  Object
  (render [this]
    (case (:component/type (om/props this))
      :simulator/basic (basic-simulator this)
      :simulator/batch-outputs (batch-outputs-simulator this))))

(def simulator (om/factory Simulator))

(defmethod extensions/component-ui :simulator/basic
  [props]
  (simulator props))

(defmethod extensions/component-ui :simulator/batch-outputs
  [props]
  (simulator props))
