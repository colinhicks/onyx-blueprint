(ns example.simulator
  (:require [goog.dom :as gdom]
            [onyx-blueprint.api :as api]))

(enable-console-print!)

(def components
  [{:component/id ::workflow
    :component/type :editor/data-structure
    :evaluations/init :content/default-input
    :content/label "Workflow"
    :content/default-input [[:read-input :increment-n]
                            [:increment-n :square-n]
                            [:square-n :write-output]]}
   
   {:component/id ::graph
    :component/type :graph/workflow
    :content/label "Graph"
    :link/evaluations {:workflow ::workflow
                       :simulator ::simulator}}

   {:component/id ::catalog
    :component/type :editor/data-structure
    :evaluations/init :content/default-input
    :content/label "Catalog"
    :content/default-input [{:onyx/name :read-input
                             :onyx/type :input
                             :onyx/batch-size 1}
                            {:onyx/name :increment-n
                             :onyx/type :function
                             :onyx/fn :cljs.user/increment-n
                             :onyx/batch-size 1}
                            {:onyx/name :square-n
                             :onyx/type :function
                             :onyx/fn :cljs.user/square-n
                             :onyx/batch-size 1}
                            {:onyx/name :write-output
                             :onyx/type :output
                             :onyx/batch-size 1}]}

   {:component/id ::fns
    :component/type :editor/fn
    :evaluations/init :content/default-input
    :content/label "Functions"
    :content/default-input
    "(defn ^:export increment-n [segment] (update-in segment [:n] inc))
(defn ^:export square-n [segment] (update-in segment [:n] (partial * (:n segment))))"}

   {:component/id ::input-segments
    :component/type :editor/data-structure
    :evaluations/init :content/default-input
    :content/label "Input segments"
    :content/default-input [{:n 0}
                            {:n 1}
                            {:n 2}
                            {:n 3}
                            {:n 4}
                            {:n 5}
                            {:n 6}
                            {:n 7}
                            {:n 8}
                            {:n 9}]}
   
   {:component/id ::simulator
    :component/type :simulator/default
    :content/label "Job controls"
    :content/controls [:initialize :next-tick :next-batch :run-to-completion]
    :link/evaluations {:job-env ::simulator
                       :workflow ::workflow
                       :catalog ::catalog
                       :input-segments ::input-segments}}

   {:component/id ::inspector
    :component/type :job-inspector/default
    :content/label "Task status"
    :link/evaluations {:job-env ::simulator}
    :link/ui-state {:graph ::graph}}])

(def sections
  [{:section/id ::graph-example
    :section/layout ['[::workflow (::graph {:graph-direction "LR"}) ::inspector]
                     [::simulator]
                     [::input-segments ::catalog ::fns]]}])

(api/render-tutorial! components sections (gdom/getElement "app"))
