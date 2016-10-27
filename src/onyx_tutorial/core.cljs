(ns onyx-tutorial.core
  (:require [goog.dom :as gdom]
            [onyx-tutorial.api :as api]))

(enable-console-print!)

(def components
  [{:component/id ::workflow-header
    :component/type :text/header
    :component/content {:text "Workflow"}}
   
   {:component/id ::workflow-editor
    :component/type :editor/data-structure
    :component/content {:default-input "[[:a :b] [:b :c]]"}}

   {:component/id ::task-fn
    :component/type :editor/fn
    :component/content {:default-input "(defn ^:export my-inc [segment]\n  (update-in segment [:n] inc))"}}])

(def sections
  [{:section/id ::workflow
    :section/layout [[::workflow-header]
                     [::workflow-editor]]}

   {:section/id ::simple-task
    :section/layout [[::task-fn]]}])

(api/render-tutorial! components sections (gdom/getElement "app"))

(defn on-js-reload []
  )

