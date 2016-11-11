(ns onyx-blueprint.ui.simulator
  (:require [cljs.pprint :as pprint]
            [goog.dom :as gdom]
            [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            [onyx-blueprint.extensions :as extensions]
            [onyx-blueprint.ui.helpers :as helpers]))


(defn button [label cb]
  (dom/input #js {:value label
                  :type "button"
                  :onClick cb}))

;; todo handle malformed data
(defn init-data [{:keys [workflow catalog job]}]
  (if job
    (-> job :result :value)
    {:workflow (-> workflow :result :value)
     :catalog (-> catalog :result :value)}))

;; todo handle malformed data
(defn input-segments [link]
  (-> link
      :input-segments
      :result
      :value))

(defui Simulator
  static om/IQuery
  (query [this]
    [:component/id :component/type :content/controls :evaluations/link :layout/hints])
  
  Object
  (render [this]
    (let [{:keys [component/id content/controls evaluations/link] :as props} (om/props this)
          ^boolean initialized? (:job-env link)]
      (apply dom/div #js {:id (helpers/component-id props)
                          :className (helpers/component-css-classes props)}
             (if-not initialized?
               [(button "Initialize"
                        (fn []
                          (om/transact! this `[(onyx/init {:id ~id
                                                           :job ~(init-data link)
                                                           :input-segments ~(input-segments link)})])))]
               (map (fn [control]
                      (case control
                        :initialize
                        (button "Reinitialize"
                                (fn []
                                  (om/transact! this `[(onyx/init {:id ~id
                                                                   :job ~(init-data link)
                                                                   :input-segments ~(input-segments link)})])))
                        :next-tick
                        (button "Next tick"
                                (fn []
                                  (om/transact! this `[(onyx/tick {:id ~id})])))
                        
                        :next-batch
                        (button "Next batch"
                                (fn []
                                  (om/transact! this `[(onyx/next-batch {:id ~id})])))

                        :run-to-completion
                        (button "Run to completion"
                                (fn []
                                  (om/transact! this `[(onyx/drain {:id ~id})])))))
                    controls)))
      
      )))

(def simulator (om/factory Simulator))

(defmethod extensions/component-ui :simulator/default
  [props]
  (simulator props))
