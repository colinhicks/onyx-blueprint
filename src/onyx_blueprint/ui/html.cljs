(ns onyx-blueprint.ui.html
  (:require [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            [onyx-blueprint.extensions :as extensions]
            [onyx-blueprint.ui.helpers :as helpers]
            [sablono.core :as sablono :refer-macros [html]]))


(defn render-hiccup [{:keys [content/hiccup] :as props}]
  ;; todo css classes
  (html hiccup))

(defn render-element [{:keys [content/tag content/text] :as props}]
  (let [el
        ;; no support for block elements
        (case tag
          :b dom/b
          :blockquote dom/blockquote
          :cite dom/cite
          :code dom/code
          :em dom/em
          :h1 dom/h1
          :h2 dom/h2
          :h3 dom/h3
          :h4 dom/h4
          :h5 dom/h5
          :h6 dom/h6
          :i dom/i
          :p dom/p
          :pre dom/pre
          :q dom/q
          :s dom/s
          :small dom/small
          :span dom/span
          :strong dom/strong
          :sub dom/sub
          :sup dom/sup
          :time dom/time
          :u dom/u
          (throw (js/Error (str "Html tag: " tag " is not supported."))))
        css-classes (helpers/component-css-classes props)]
    
    (if (coll? text)
      (apply dom/div nil
             (map (partial el #js {:className css-classes})
                  text))
      (el #js {:className css-classes} text))))


(defui Html
  static om/IQuery
  (query [this]
    [:component/id :component/type :content/tag
     :content/text :content/hiccup :layout/hints])
    
  Object
  (render [this]
    (let [props (om/props this)]
      (if (:content/hiccup props)
        (render-hiccup props)
        (render-element props)))))

(def html-component (om/factory Html))

(defmethod extensions/component-ui :html/element [props]
  (html-component props))

(defmethod extensions/component-ui :html/hiccup [props]
  (html-component props))

;; deprecated
(defmethod extensions/component-ui :html/header [props]
  (html-component (merge props {:content/tag :h2})))

(defmethod extensions/component-ui :html/sub-header [props]
  (html-component (merge props {:content/tag :h4})))

(defmethod extensions/component-ui :html/body [props]
  (html-component (merge props {:content/tag :p})))

