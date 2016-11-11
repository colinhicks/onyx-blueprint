(ns onyx-blueprint.ui.simulator
  (:require [cljs.pprint :as pprint]
            [goog.dom :as gdom]
            [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            [onyx-blueprint.extensions :as extensions]
            [onyx-blueprint.ui.helpers :as helpers]))


(defn button [label css-class cb]
  (dom/input #js {:value label
                  :type "button"
                  :onClick cb
                  :className css-class}))

;; todo handle malformed data
(defn init-data [{:keys [workflow catalog job]}]
  (if job
    (-> job :result :value)
    {:workflow (-> workflow :result :value)
     :catalog (-> catalog :result :value)}))

;; todo handle malformed data
(defn input-segments [evaluations]
  (-> evaluations
      :input-segments
      :result
      :value))

(defui Simulator
  static om/IQuery
  (query [this]
    [:component/id :component/type :content/label
     :content/controls :link/evaluations :layout/hints])
  
  Object
  (render [this]
    (let [{:keys [component/id content/controls link/evaluations] :as props} (om/props this)
          ^boolean initialized? (:job-env evaluations)]
      (dom/div #js {:id (helpers/component-id props)
                    :className (helpers/component-css-classes props)}
               (helpers/label props)
               (apply dom/div #js {:className "controls-container"}
                      (cond-> []
                        (not initialized?)
                        (conj (button "Initialize"
                                      "initialize"
                                      (fn []
                                        (om/transact! this
                                                      `[(onyx/init {:id ~id
                                                                    :job ~(init-data evaluations)
                                                                    :input-segments ~(input-segments evaluations)})
                                                        :blueprint/sections]))))
                        initialized?
                        (into
                         (map (fn [control]
                                (case control
                                  :initialize
                                  (button "Reinitialize"
                                          "reinitialize"
                                          (fn []
                                            (om/transact! this
                                                          `[(onyx/init {:id ~id
                                                                        :job ~(init-data evaluations)
                                                                        :input-segments ~(input-segments evaluations)})
                                                            :blueprint/sections])))
                                  :next-tick
                                  (button "Next tick"
                                          "next-tick"
                                          (fn []
                                            (om/transact! this
                                                          `[(onyx/tick {:id ~id})
                                                            :blueprint/sections])))
                                  
                                  :next-batch
                                  (button "Next batch"
                                          "next-batch"
                                          (fn []
                                            (om/transact! this
                                                          `[(onyx/next-batch {:id ~id})
                                                            :blueprint/sections])))

                                  :run-to-completion
                                  (button "Run to completion"
                                          "run-to-completion"
                                          (fn []
                                            (om/transact! this
                                                          `[(onyx/drain {:id ~id})
                                                            :blueprint/sections])))))
                              controls))))))))

(def simulator (om/factory Simulator))

(defmethod extensions/component-ui :simulator/default
  [props]
  (simulator props))
