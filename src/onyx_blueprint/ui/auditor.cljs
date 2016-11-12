(ns onyx-blueprint.ui.auditor
  (:require [cljs.pprint :as pprint]
            [goog.dom :as gdom]
            [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            [onyx-blueprint.extensions :as extensions]
            [onyx-blueprint.ui.helpers :as helpers]))


(defui Auditor
  static om/IQuery
  (query [this]
    [:component/id :component/type :content/label :link/evaluations :layout/hints])
  
  Object
  (render [this]
    (let [{:keys [link/evaluations] :as props} (om/props this)
          failures? (some (fn [[_ ev]] (or (-> ev :result :error)
                                           (-> ev :validation :valid? (= false))))
                          evaluations)]
      (dom/div #js {:id (helpers/component-id props)
                    :className (str (helpers/component-css-classes props)
                                    (when-not failures? " valid"))}
               (helpers/label props)
               (dom/span #js {:className "valid-mark"})
               (when failures?
                 (apply dom/ul nil
                        (map
                         (fn [[k {:keys [component/id result] :as evaluation}]]
                           (let [explain (-> evaluation :validation :explain)
                                 error (:error result)]
                             (cond
                               explain
                               (dom/li #js {:className "validation-failure"}
                                       "Failure validating " (dom/code nil (name k)) ": "
                                       (dom/span #js {:className "message"} explain))

                               error
                               (dom/li #js {:className "error"}
                                       "Error evaluating " (dom/code nil (name k)) ": "
                                       (dom/span #js {:className "message"} (.. error -cause -message))))
                             ))
                         evaluations)))))))

(def auditor (om/factory Auditor))

(defmethod extensions/component-ui :auditor/default
  [props]
  (auditor props))
