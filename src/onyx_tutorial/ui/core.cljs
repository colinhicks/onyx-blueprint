(ns onyx-tutorial.ui.core
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [goog.dom :as gdom]
            [onyx-tutorial.extensions :as extensions]
            [onyx-tutorial.ui.code-editor]
            [onyx-tutorial.ui.text]
            [onyx-tutorial.ui.graph]))

(defui TutorialComponent
    static om/Ident
    (ident [this {:keys [component/id]}]
      [:tutorial/components id])

    static om/IQuery
    (query [this]
      [:component/id :component/type :component/content
       {:component.graph/content (om/get-query onyx-tutorial.ui.graph/Graph)}])

    Object
    (render [this]
      (let [props (-> (om/props this)
                      (om/computed {:transact (fn [expr]
                                                (om/transact! this expr))}))]
        (extensions/component-ui props))))

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
      [{:tutorial/sections (om/get-query Section)}])

    Object
    (render [this]
      (let [{:keys [tutorial/sections] :as props} (om/props this)]
        (apply dom/div nil (mapv section sections)))))
