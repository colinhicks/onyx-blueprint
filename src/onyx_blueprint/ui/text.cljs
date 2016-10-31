(ns onyx-blueprint.ui.text
  (:require [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            [onyx-blueprint.extensions :as extensions]))

(defui Text
  static om/IQuery
  (query [this]
    [:component/id :component/type :content/text])
    
  Object
  (render [this]
    (let [{:keys [component/content component/type content/text]} (om/props this)
          {:keys [element]} (om/get-computed this)]
      (if (coll? text)
        (apply dom/div nil
               (map (partial element #js {:className (name type)})
                    text))
        (element #js {:className (str (name type) "component col")} text)))))

(def text (om/factory Text))

(defmethod extensions/component-ui :text/header [props]
  (text (om/computed props {:element dom/h2})))

(defmethod extensions/component-ui :text/body [props]
  (text (om/computed props {:element dom/p})))

