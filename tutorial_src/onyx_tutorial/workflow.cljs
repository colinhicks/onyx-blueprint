(ns onyx-tutorial.workflow
  (:require [cljs.pprint :as pprint]
            [goog.dom :as gdom]
            [onyx-blueprint.api :as api]
            [onyx-tutorial.builders :as b]))

(def components
  [(b/header ::title
             "Workflows")
   
   (b/hiccup ::graph-desc
             [:div
              [:p "In Onyx, workflows define the graph of all possible routes where data can flow through your job. You can think of this as isolating the \"structure\" of your computation."]
              [:p "Here's a picture of the workflow that we're crafting. Data starts at the top and flows downward in all directions. This workflow happens to be flat, so data moves in a straight line."]
              [:p "The representation of a workflow is a Clojure vector of vectors. The inner vectors contain two elements: source task and destination task, respectively."]
              [:pre "[[:read-input :increment-n]
 [:increment-n :square-n]
 [:square-n :write-output]]"]
              [:p "As you can see in the visualization, these keywords represent nodes in the workflow graph. Each inner vector represents a directed edge from the first node to the second."]])

   {:component/id ::graph
    :component/type :blueprint/graph
    :ui-state/initial {:action :select-tasks
                       :tasks [:read-input :write-output]}
    :link/evaluations {:workflow ::graph-data}}

   ;; not displayed must match :pre above
   {:component/id ::graph-data
    :component/type :blueprint/editor
    :evaluations/init :content/default-input
    :content/read-only? true
    :content/default-input [[:read-input :increment-n]
                            [:increment-n :square-n]
                            [:square-n :write-output]]}

   (b/hiccup ::cheat-sheet-note-and-task-definition
             [:aside
              [:p "Onyx's " [:strong "information model"] " is documented in the "
               [:a {:href "http://www.onyxplatform.org/docs/cheat-sheet/latest/"}
                "cheat sheet"] "." ]
              [:p "A " [:strong "task"] " is the smallest unit of work in Onyx. It represents an actvity of input, processing or output."]])

   (b/header ::in-action-header "Example: A workflow in action")
   
   (b/body ::in-action-leadin
           ["Throughout the tutorial, you'll be able to check your understanding by interacting with examples and challenges."
            "Let's take a quick look at the output of the above workflow, given the following input. Click the \"Process input\" button. You can also try editing the input."])

   (b/hiccup ::in-action-note
             [:aside [:p "Later in the tutorial, you'll configure Onyx to do your bidding at each task in your workflow. In this example, that's been done for you behind the scenes."]])

   {:component/id ::in-action-input-segments
    :component/type :blueprint/editor
    :evaluations/init :content/default-input
    :content/label "Input data"
    :content/default-input (with-out-str
                             (binding [pprint/*print-right-margin* 15]
                               (pprint/pprint [{:n 0}
                                               {:n 1}
                                               {:n 2}
                                               {:n 3}
                                               {:n 4}
                                               {:n 5}
                                               {:n 6}
                                               {:n 7}
                                               {:n 8}
                                               {:n 9}])))}

   {:component/id ::in-action-simulator
    :component/type :blueprint/simulator
    :content/label "Job controls"
    :content/controls [:initialize :run-to-completion]
    :link/evaluations {:workflow ::graph-data
                       :catalog ::in-action-catalog
                       :job-env ::in-action-simulator
                       :user-fn ::in-action-fns
                       :input-segments ::in-action-input-segments}}

   {:component/id ::in-action-inspector
    :component/type :blueprint/job-inspector
    :content/label "Task status"
    :link/evaluations {:job-env ::in-action-simulator}
    :link/ui-state {:graph ::graph}}

   (b/hiccup ::in-action-description
             [:div
              [:p "What's going on?"]
              [:ol
               [:li "The " [:code ":read-input"] " task conveys segments into the job."]
               [:li "Each segment is processed by the " [:code ":increment-n"] " function."]
               [:li "Each segment is processed by the " [:code ":square-n"] " function."]
               [:li "And the " [:code ":write-output"] " task displays the resulting segments."]]])

   (b/hiccup ::segment-definition
             [:aside
              [:p "A " [:strong "segment"] " is the unit of data in Onyx, and it’s represented by a Clojure map. Segments represent the data flowing through the cluster. Segments are the only shape of data that Onyx allows you to emit between functions."]])

   ;; behind the scenes
   {:component/id ::in-action-catalog
    :component/type :blueprint/editor
    :evaluations/init :content/default-input
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

   {:component/id ::in-action-fns
    :component/type :blueprint/editor
    :evaluations/init :content/default-input
    :content/default-input
    "(defn ^:export increment-n [segment] (update-in segment [:n] inc))
     (defn ^:export square-n [segment] (update-in segment [:n] (partial * (:n segment))))"}

   (b/header ::implementing-1-header
             "Challenge: Implementing workflows")

   (b/body ::implementing-1-leadin
           "Time to implement your first workflow. Use the code editor below to match Graph A to Graph B.")

   (b/hiccup ::implementing-label-editor [:em "Workflow Editor"])
   
   (b/hiccup ::implementing-1-label-a [:em "Graph A"])
   
   {:component/id ::implementing-1-graph-a
    :component/type :blueprint/graph
    :content/label "Graph A"
    :link/evaluations {:workflow ::implementing-1-data-a}}
   
   {:component/id ::implementing-1-data-a
    :component/type :blueprint/editor
    :evaluations/validate-spec :onyx.core/workflow
    :evaluations/init :content/default-input
    :content/label "Workflow A"
    :content/default-input [[:read-segments :write-segments]]}

   {:component/id ::implementing-1-graph-b
    :component/type :blueprint/graph
    :content/label "Graph B"
    :link/evaluations {:workflow ::implementing-1-data-b}}
   
   {:component/id ::implementing-1-data-b
    :component/type :blueprint/editor
    :evaluations/init :content/default-input
    :content/read-only? true
    :content/default-input [[:read-segments :cube-n]
                            [:cube-n :add-ten]
                            [:add-ten :multiply-by-5]
                            [:multiply-by-5 :write-segments]]}

   (b/hiccup ::implementing-2-leadin
             [:div
              [:p "Workflows are direct, acyclic graphs - meaning they can split and merge."]
              [:p "Try modeling the workflow in Graph D. The task " [:code ":cube-n"] " splits its output into two streams - into tasks " [:code ":add-ten"]", and " [:code ":add-forty"]". Both tasks get " [:em "all"] " the segments produced by " [:code ":cube-n"] ". Their results are sent to " [:code ":multiply-by-5"]"."]])

   {:component/id ::implementing-2-graph-a
    :component/type :blueprint/graph
    :content/label "Graph C"
    :link/evaluations {:workflow ::implementing-2-data-a}}
   
   {:component/id ::implementing-2-data-a
    :component/type :blueprint/editor
    :evaluations/validate-spec :onyx.core/workflow
    :evaluations/init :content/default-input
    :content/label "Workflow C"
    :content/default-input [[:read-segments :write-segments]]}

   {:component/id ::implementing-2-graph-b
    :component/type :blueprint/graph
    :content/label "Graph D"
    :link/evaluations {:workflow ::implementing-2-data-b}}
   
   {:component/id ::implementing-2-data-b
    :component/type :blueprint/editor
    :evaluations/init :content/default-input
    :content/read-only? true
    :content/default-input [[:read-segments :cube-n]
                            [:cube-n :add-ten]
                            [:cube-n :add-forty]
                            [:add-ten :multiply-by-5]
                            [:add-forty :multiply-by-5]
                            [:multiply-by-5 :write-segments]]}

   (b/body ::implementing-3-leadin
           "Function tasks aren't the only thing in workflows that can branch. Inputs and outputs can branch, too. Let's try a more complex workflow, shown below. We use single capital letters for tasks this time for concision.")

   {:component/id ::implementing-3-graph-a
    :component/type :blueprint/graph
    :content/label "Graph E"
    :link/evaluations {:workflow ::implementing-3-data-a}}
   
   {:component/id ::implementing-3-data-a
    :component/type :blueprint/editor
    :content/label "Workflow E"
    :evaluations/validate-spec :onyx.core/workflow
    :evaluations/init :content/default-input
    :content/default-input [[:A :L]]}

   {:component/id ::implementing-3-graph-b
    :component/type :blueprint/graph
    :content/label "Graph F"
    :link/evaluations {:workflow ::implementing-3-data-b}}
   
   {:component/id ::implementing-3-data-b
    :component/type :blueprint/editor
    :evaluations/init :content/default-input
    :content/read-only? true
    :content/default-input [[:A :D]
                            [:B :D]
                            [:D :F]
                            [:F :J]
                            [:F :K]
                            [:C :E]
                            [:E :G]
                            [:E :H]
                            [:E :I]
                            [:G :L]
                            [:H :L]
                            [:I :L]
                            [:D :G]]}

   (b/body ::outro
           "Now that you have the hang of workflows, it's time to discuss how to tell Onyx what to actually do at each task ...")

   {:component/id ::auditor
    :component/type :blueprint/auditor
    :content/label "Code status"
    :link/evaluations {:in-action-input-segments ::in-action-input-segments
                       :implementing-1-data-a ::implementing-1-data-a
                       :implementing-2-data-a ::implementing-2-data-a
                       :implementing-3-data-a ::implementing-3-data-a}}
   ])

(def sections
  [{:section/id :workflow-basics
    :section/layout [[::title]
                     '[::graph-desc
                       (::graph {:className "col-shrink-2"})
                       ::cheat-sheet-note-and-task-definition]
                     
                     [::in-action-header]
                     [::in-action-leadin ::in-action-note]
                     [::in-action-input-segments
                       ::in-action-simulator
                       ::in-action-inspector]
                     [::in-action-description ::segment-definition]
                     
                     [::implementing-1-header]
                     [::implementing-1-leadin]
                     '[::implementing-1-data-a
                       (::implementing-1-graph-a {:className "col-center"})
                       (::implementing-1-graph-b {:className "col-center col-height-med"})]

                     [::implementing-2-leadin]
                     '[::implementing-2-data-a
                       (::implementing-2-graph-a {:className "col-center"})
                       (::implementing-2-graph-b {:className "col-center col-height-med"})]

                     [::implementing-3-leadin]                   
                     '[::implementing-3-data-a
                       (::implementing-3-graph-a {:className "col-center"})
                       (::implementing-3-graph-b {:className "col-center col-height-med"})]

                     [::outro]

                     '[(::auditor {:className "fixed-bottom"})]
                     ]}])

(api/render-tutorial! components sections (gdom/getElement "app"))
