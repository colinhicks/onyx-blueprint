(ns onyx-tutorial.core
  (:require [goog.dom :as gdom]
            [onyx-tutorial.api :as api]))

(enable-console-print!)

(def components
  [;; Workflow
   {:component/id ::workflow-header
    :component/type :text/header
    :component/content {:text "Workflow"}}

   {:component/id ::workflow-leadin
    :component/type :text/body
    :component/content
    {:text ["A workflow is the structural specification of an Onyx program. Its purpose is to articulate the paths that data flows through the cluster at runtime. It is specified via a directed, acyclic graph."
            "The workflow representation is a Clojure vector of vectors. Each inner vector contains exactly two elements, which are keywords. The keywords represent nodes in the graph, and the vector represents a directed edge from the first node to the second."
            "[Try editing the example data structures ...]"]}}
   
   {:component/id ::workflow-ex1-data
    :component/type :editor/data-structure
    :component/content {:default-input [[:input :increment] [:increment :output]]}}

   {:component/id ::workflow-ex1-graph
    :component/type :graph/simple
    :component/target ::workflow-ex1-data}

   {:component/id ::workflow-ex2-data
    :component/type :editor/data-structure
    :component/content {:default-input [[:input :processing-1]
                                        [:input :processing-2]
                                        [:processing-1 :output-1]
                                        [:processing-2 :output-2]]}}

   {:component/id ::workflow-ex2-graph
    :component/type :graph/simple
    :component/target ::workflow-ex2-data}

   
   {:component/id ::workflow-ex3-data
    :component/type :editor/data-structure
    :component/content {:default-input [[:input :processing-1]
                                        [:input :processing-2]
                                        [:processing-1 :output]
                                        [:processing-2 :output]]}}

   {:component/id ::workflow-ex3-graph
    :component/type :graph/simple
    :component/target ::workflow-ex3-data}

   ;; Catalog
   {:component/id ::catalog-header
    :component/type :text/header
    :component/content {:text "Catalog"}}

   {:component/id ::catalog-leadin
    :component/type :text/body
    :component/content
    {:text "All inputs, outputs, and functions in a workflow must be described via a catalog. A catalog is a vector of maps, strikingly similar to Datomic’s schema. Configuration and docstrings are described in the catalog."}}

   ;; Basic job
   {:component/id ::job-ex1-header
    :component/type :text/header
    :component/content {:text "Your first job"}}

   {:component/id ::job-ex1-leadin
    :component/type :text/body
    :component/content
    {:text "[Try editing this basic setup ...]"}}

   {:component/id ::job-ex1-data
    :component/type :editor/data-structure
    :component/content
    {:default-input {:workflow [[:in :inc] [:inc :out]]
                     :catalog [{:onyx/name :in
                                :onyx/type :input
                                :onyx/batch-size 20}
                               {:onyx/name :inc
                                :onyx/type :function
                                :onyx/fn :cljs.user/my-inc
                                :onyx/batch-size 20}
                               {:onyx/name :out
                                :onyx/type :output
                                :onyx/batch-size 20}]}}}
   
   {:component/id ::job-ex1-fn
    :component/type :editor/fn
    :component/content
    {:default-input "(defn ^:export my-inc [segment]\n  (update-in segment [:n] inc))"}}

   {:component/id ::job-ex1-simulator
    :component/type :simulator/basic
    :component/content {:gen-segment (fn [] {:n (rand-int 100)})}
    :component/target ::job-ex1-data}])
   
(def sections
  [{:section/id ::workflow
    :section/layout [[::workflow-header]
                     [::workflow-leadin]
                     [::workflow-ex1-data ::workflow-ex1-graph]
                     [::workflow-ex2-data ::workflow-ex2-graph]
                     [::workflow-ex3-data ::workflow-ex3-graph]]}
   {:section/id ::catalog
    :section/layout [[::catalog-header]
                     [::catalog-leadin]]}
   {:section/id ::job-ex1
    :section/layout [[::job-ex1-header]
                     [::job-ex1-leadin]
                     [::job-ex1-data ::job-ex1-fn]
                     [::job-ex1-simulator]]}])

(api/render-tutorial! components sections (gdom/getElement "app"))

(defn on-js-reload []
  )

