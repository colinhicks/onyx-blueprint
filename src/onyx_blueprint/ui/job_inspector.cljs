(ns onyx-blueprint.ui.job-inspector
  (:require [cljs.pprint :as pprint]
            [goog.dom :as gdom]
            [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            [onyx-local-rt.api :as onyx.api]
            [onyx-blueprint.extensions :as extensions]
            [onyx-blueprint.ui.helpers :as helpers]))

(defn selected-task-status [graph-state {:keys [job-env]}]
  (let [task (-> graph-state :tasks first) ;; todo: support multiple selection?
        status (-> job-env :result :value (onyx.api/env-summary) :tasks task)]
    (dom/div #js {:className "task-status"}
             (dom/code #js {:className "task-name"} (str task))
             (if ^boolean status
               (dom/pre #js {:className "code"}
                        (with-out-str (pprint/pprint status)))
               (dom/span #js {:className "advice"} "No data. Is the job initialized?")))))

(defui JobInspector
  static om/IQuery
  (query [this]
    [:component/id :component/type :link/evaluations :link/ui-state :layout/hints])
  
  Object
  (render [this]
    (let [{:keys [link/evaluations link/ui-state] :as props} (om/props this)
          graph-state (:graph ui-state)
          ^boolean no-graph-selection? (or (nil? graph-state)
                                           (keyword-identical? :deselect-tasks
                                                               (:action graph-state)))]
      (dom/div #js {:id (helpers/component-id props)
                    :className (str (helpers/component-css-classes props)
                                    (when-not no-graph-selection? " selected-graph"))}
               (case (:action graph-state)
                 (nil :deselect-tasks)
                 (dom/span #js {:className "advice"} "Select a task on the graph.")

                 :select-tasks
                 (selected-task-status graph-state evaluations)
                 )))))

(def job-inspector (om/factory JobInspector))

(defmethod extensions/component-ui :job-inspector/default
  [props]
  (job-inspector props))
