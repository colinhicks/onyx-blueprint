(ns onyx-blueprint.ui.text
  (:require [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            [onyx-blueprint.extensions :as extensions]
            [onyx-blueprint.ui.helpers :as helpers]))

(defui Text
  static om/IQuery
  (query [this]
    [:component/id :component/type :content/text :layout/hints])
    
  Object
  (render [this]
    (let [{:keys [component/content component/type content/text] :as props} (om/props this)
          {:keys [element]} (om/get-computed this)
          css-classes (helpers/component-css-classes props)]
      (if (coll? text)
        (apply dom/div nil
               (map (partial element #js {:className css-classes})
                    text))
        (element #js {:className css-classes} text)))))

(def text (om/factory Text))

(defmethod extensions/component-ui :text/header [props]
  (text (om/computed props {:element dom/h2})))

(defmethod extensions/component-ui :text/body [props]
  (text (om/computed props {:element dom/p})))

