(ns onyx-blueprint.ui.core
  (:require [goog.dom :as gdom]
            [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            [onyx-blueprint.extensions :as extensions]
            [onyx-blueprint.ui.code-editor :refer [CodeEditor]]
            [onyx-blueprint.ui.simulator :refer [Simulator]]
            [onyx-blueprint.ui.text :refer [Text]]
            [onyx-blueprint.ui.graph :refer [Graph]]))

(defui TutorialComponent
    static om/Ident
    (ident [this {:keys [component/id] :as props}]
      [:blueprint/components id])

    static om/IQuery
    (query [this]
      (merge {:text (om/get-query Text)
              :graph (om/get-query Graph)
              :editor (om/get-query CodeEditor)
              :simulator (om/get-query Simulator)}
             extensions/*custom-component-queries*))

    Object
    (render [this]
      (extensions/component-ui (om/props this))))

(def component (om/factory TutorialComponent))

(defui Section
    static om/IQuery
    (query [this]
      [:section/id
       {:section/rows [{:row/items (om/get-query TutorialComponent)}]}])

    Object
    (render [this]
      (let [{:keys [section/id section/rows] :as props} (om/props this)]
        (apply dom/div #js {:id (name id) :className "section"} ; todo namespace-based html id
               (mapv (fn [{:keys [row/items]}]
                       (apply dom/div #js {:className "row"} (mapv component items)))
                     rows)))))

(def section (om/factory Section))

(defui Tutorial
    static om/IQuery
    (query [this]
      [{:blueprint/sections (om/get-query Section)}])

    Object
    (render [this]
      (let [{:keys [blueprint/sections] :as props} (om/props this)]
        (apply dom/div nil (mapv section sections)))))
