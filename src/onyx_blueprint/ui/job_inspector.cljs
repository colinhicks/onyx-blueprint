(ns onyx-blueprint.ui.job-inspector
  (:require [cljs.pprint :as pprint]
            [goog.dom :as gdom]
            [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            [onyx-local-rt.api :as onyx.api]
            [onyx-blueprint.extensions :as extensions]
            [onyx-blueprint.ui.helpers :as helpers]))

(defn selected-task-status [env-summary task]
  (let [status (-> env-summary :tasks task)]
    (dom/div #js {:className "task-status"}
             (dom/code #js {:className "task-name"} (str task))
             (if ^boolean status
               (dom/pre #js {:className "code"}
                        (with-out-str (pprint/pprint status)))
               (dom/div #js {:className "advice"} "No data. Is the job initialized?")))))

(defui JobInspector
  static om/IQuery
  (query [this]
    [:component/id :component/type :content/label
     :link/evaluations :link/ui-state :layout/hints])
  
  Object
  (render [this]
    (let [{:keys [link/evaluations link/ui-state] :as props} (om/props this)
          {:keys [action tasks] :as graph-state} (:graph ui-state)
          ^boolean no-graph-selection? (or (nil? graph-state)
                                           (keyword-identical? :deselect-tasks
                                                               action))]
      (apply dom/div #js {:id (helpers/component-id props)
                    :className (str (helpers/component-css-classes props)
                                    (when-not no-graph-selection? " selected-graph"))}
             (cond-> [(helpers/label props)]
               no-graph-selection?
               (conj
                (dom/div #js {:className "advice"} "Select a task on the graph."))

               (keyword-identical? :select-tasks action)
               (into
                (map (partial selected-task-status
                              (-> evaluations
                                  :job-env
                                  :result
                                  :value
                                  (onyx.api/env-summary)))
                     tasks)))))))

(def job-inspector (om/factory JobInspector))

(defmethod extensions/component-ui :blueprint/job-inspector
  [props]
  (job-inspector props))

;; deprecated
(defmethod extensions/component-ui :job-inspector/default
  [props]
  (job-inspector props))
