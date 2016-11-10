(ns onyx-blueprint.ui.simulator
  (:require [cljs.pprint :as pprint]
            [goog.dom :as gdom]
            [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            [onyx-blueprint.extensions :as extensions]
            [onyx-blueprint.ui.helpers :as helpers]))



(defui Simulator
  static om/IQuery
  (query [this]
    [:component/id :component/type :evaluations/link :content/controls])
  
  Object
  (render [this]
    (let [{:keys [content/controls] :as props} (om/props this)]
      ;; output controls...
      
      )))

(def simulator (om/factory Simulator))

(defmethod extensions/component-ui :simulator/default
  [props]
  (simulator props))
