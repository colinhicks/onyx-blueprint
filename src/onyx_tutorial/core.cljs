(ns onyx-tutorial.core
  (:require [goog.dom :as gdom]
            [onyx-tutorial.api :as api]))

(enable-console-print!)

(def components
  [{:component/id ::workflow-header
    :component/type :text/header
    :component/content {:text "Workflow"}}

   {:component/id ::workflow-leadin
    :component/type :text/body
    :component/content
    {:text ["A workflow is the structural specification of an Onyx program. Its purpose is to articulate the paths that data flows through the cluster at runtime. It is specified via a directed, acyclic graph."
            "The workflow representation is a Clojure vector of vectors. Each inner vector contains exactly two elements, which are keywords. The keywords represent nodes in the graph, and the vector represents a directed edge from the first node to the second."]}}
   
   {:component/id ::workflow-editor
    :component/type :editor/data-structure
    :component/content {:default-input '[[:a :b] [:b :c]]}}

   {:component/id ::workflow-graph
    :component/type :graph/simple
    :component/target ::workflow-editor}

   #_{:component/id ::task-fn
    :component/type :editor/fn
    :component/content
    {:default-input "(defn ^:export my-inc [segment]\n  (update-in segment [:n] inc))"}}])

(def sections
  [{:section/id ::workflow
    :section/layout [[::workflow-header]
                     [::workflow-leadin]
                     [::workflow-editor ::workflow-graph]]}

   #_{:section/id ::simple-task
    :section/layout [[::task-fn]]}])

(api/render-tutorial! components sections (gdom/getElement "app"))

(defn on-js-reload []
  )

