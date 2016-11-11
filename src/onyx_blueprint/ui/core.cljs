(ns onyx-blueprint.ui.core
  (:require [cljs.pprint :as pprint]
            [goog.dom :as gdom]
            [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            [onyx-blueprint.extensions :as extensions]
            [onyx-blueprint.ui.helpers :as helpers]
            [onyx-blueprint.ui.code-editor :refer [CodeEditor]]
            [onyx-blueprint.ui.graph :refer [Graph]]
            [onyx-blueprint.ui.html :refer [Html]]
            [onyx-blueprint.ui.simulator :refer [Simulator]]
            [onyx-blueprint.ui.job-inspector :refer [JobInspector]]))

(defui TutorialComponent
    static om/Ident
    (ident [this {:keys [component/id] :as props}]
      [:blueprint/components id])

    static om/IQuery
    (query [this]
      (merge {:html (om/get-query Html)
              :graph (om/get-query Graph)
              :editor (om/get-query CodeEditor)
              :simulator (om/get-query Simulator)
              :job-inspector (om/get-query JobInspector)}
             extensions/*custom-component-queries*))

    Object
    (render [this]
      (extensions/component-ui (om/props this))))

(def component (om/factory TutorialComponent))

(defmethod extensions/component-ui :default [props]
  (dom/pre nil (with-out-str (pprint/pprint props))))

(defui Section
    static om/IQuery
    (query [this]
      [:section/id
       {:section/rows [{:row/items (om/get-query TutorialComponent)}]}])

    Object
    (render [this]
      (let [{:keys [section/id section/rows] :as props} (om/props this)]
        (apply dom/div #js {:id (helpers/keyword->attr-val id) :className "section"}
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

