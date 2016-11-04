(ns onyx-tutorial.workflow-basics
  (:require [onyx-tutorial.builders :as b]))


(def components
  [(b/header ::title
             "Workflows")
   
   (b/body ::leadin
           "In Onyx, workflows define the graph of all possible routes where data can flow through your job. You can think of this as isolating the \"structure\" of your computation.")

   (b/hiccup ::cheat-sheet-note
             [:aside "Onyx's information model is documented in the "
              [:a {:href "http://www.onyxplatform.org/docs/cheat-sheet/latest/"}
               "cheat sheet"] "."])

   (b/body ::graph-desc
           "Here's a picture of the workflow that we're crafting. Data starts at the top and flows downward in all directions. This workflow happens to be flat, so data moves in a straight line.")

   {:component/id ::graph
    :component/type :graph/workflow
    :evaluations/link {:workflow ::graph-data}}

   (b/body ::graph-data-desc
           ["The representation of a workflow is a Clojure vector of vectors. The inner vectors contain two elements: source task and destination task, respectively."
            "As you can see in the visualization above, these keywords represent nodes in the workflow graph. Each inner vector represents a directed edge from the first node to the second."])

   (b/hiccup ::task-definition
             [:aside
              [:h4 "Task"]
              [:p "A task is the smallest unit of work in Onyx. It represents an actvity of input, processing or output."]])
   
   {:component/id ::graph-data
    :component/type :editor/data-structure
    :evaluations/init :content/default-input
    :content/editable? false
    :content/default-input [[:read-input :increment-n]
                            [:increment-n :square-n]
                            [:square-n :write-output]]}

   (b/header ::in-action-header "A workflow in action")
   
   (b/body ::in-action-leadin
           ["Let's take a quick look at the workflow's output given the following input. Click the \"Process input\" button."])

   (b/hiccup ::in-action-note
             [:aside [:p "Later in the tutorial, you'll configure Onyx to do your bidding at each task in your workflow. In this example, that work has been done for you behind the scenes."]])

   {:component/id ::in-action-input-segments
    :component/type :editor/data-structure
    :evaluations/init :content/default-input
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

   {:component/id ::in-action-simulator
    :component/type :simulator/batch-outputs
    :evaluations/link {:init-data {:workflow ::graph-data
                                   :catalog ::in-action-catalog}
                       :job-env ::in-action-simulator
                       :user-fn ::in-action-fns
                       :input-segments ::in-action-input-segments}}

   (b/hiccup ::in-action-description
             [:ol
              [:li "The " [:code ":read-input"] " task conveys segments into the job."]
              [:li "Each segment is processed by the " [:code ":increment-n"] " function."]
              [:li "Each segment is processed by the " [:code ":square-n"] " function."]
              [:li "And the " [:code ":write-output"] " task displays the resulting segments."]])

   (b/hiccup ::segment-definition
             [:aside
              [:h4 "Segment"]
              [:p "A segment is the unit of data in Onyx, and it’s represented by a Clojure map. Segments represent the data flowing through the cluster. Segments are the only shape of data that Onyx allows you to emit between functions."]])

   ;; behind the scenes
   {:component/id ::in-action-catalog
    :component/type :editor/data-structure
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
    :component/type :editor/fn
    :evaluations/init :content/default-input
    :content/default-input
    "(defn ^:export increment-n [segment] (update-in segment [:n] inc))
     (defn ^:export square-n [segment] (update-in segment [:n] (partial * (:n segment))))"}

   (b/header ::implementing-1-header
             "More on implementing workflows")

   (b/body ::implementing-1-leadin
           "Time to implement your first workflow. Use the code editor below to match Graph A to Graph B.")

   (b/hiccup ::implementing-1-label-a [:em "Graph A"])
   
   {:component/id ::implementing-1-graph-a
    :component/type :graph/workflow
    :evaluations/link {:workflow ::implementing-1-data-a}}
   
   {:component/id ::implementing-1-data-a
    :component/type :editor/data-structure
    :evaluations/init :content/default-input
    :content/default-input [[:read-segments :write-segments]]}

   (b/hiccup ::implementing-1-label-b [:em "Graph B"])
   
   {:component/id ::implementing-1-graph-b
    :component/type :graph/workflow
    :evaluations/link {:workflow ::implementing-1-data-b}}
   
   {:component/id ::implementing-1-data-b
    :component/type :editor/data-structure
    :evaluations/init :content/default-input
    :content/editable? false
    :content/default-input [[:read-segments :cube-n]
                            [:cube-n :add-ten]
                            [:add-ten :multiply-by-5]
                            [:multiply-by-5 :write-segments]]}

   (b/hiccup ::implementing-2-leadin
             [:div
              [:p "Workflows are direct, acyclic graphs - meaning they can split and merge."]
              [:p "Try modeling the workflow in Graph D. The task " [:code ":cube-n"] " splits its output into two streams - into tasks " [:code ":add-ten"]", and " [:code ":add-forty"]". Both tasks get *all* the segments produced by " [:code ":cube-n"] ". Their results are sent to " [:code ":multiply-by-5"]"."]])

   (b/hiccup ::implementing-2-label-a [:em "Graph C"])
   
   {:component/id ::implementing-2-graph-a
    :component/type :graph/workflow
    :evaluations/link {:workflow ::implementing-2-data-a}}
   
   {:component/id ::implementing-2-data-a
    :component/type :editor/data-structure
    :evaluations/init :content/default-input
    :content/default-input [[:read-segments :write-segments]]}

   (b/hiccup ::implementing-2-label-b [:em "Graph D"])
   
   {:component/id ::implementing-2-graph-b
    :component/type :graph/workflow
    :evaluations/link {:workflow ::implementing-2-data-b}}
   
   {:component/id ::implementing-2-data-b
    :component/type :editor/data-structure
    :evaluations/init :content/default-input
    :content/editable? false
    :content/default-input [[:read-segments :cube-n]
                            [:cube-n :add-ten]
                            [:cube-n :add-forty]
                            [:add-ten :multiply-by-5]
                            [:add-forty :multiply-by-5]
                            [:multiply-by-5 :write-segments]]}

   
   (b/body ::implementing-3-leadin
           "Function tasks aren't the only thing in workflows that can branch. Inputs and outputs can branch, too. Let's try a more complex workflow, shown below. We use single capital letters for tasks this time for concision.")

   (b/hiccup ::implementing-3-label-a [:em "Graph E"])
   
   {:component/id ::implementing-3-graph-a
    :component/type :graph/workflow
    :evaluations/link {:workflow ::implementing-3-data-a}}
   
   {:component/id ::implementing-3-data-a
    :component/type :editor/data-structure
    :evaluations/init :content/default-input
    :content/default-input [[:A :L]]}

   (b/hiccup ::implementing-3-label-b [:em "Graph F"])
   
   {:component/id ::implementing-3-graph-b
    :component/type :graph/workflow
    :evaluations/link {:workflow ::implementing-3-data-b}}
   
   {:component/id ::implementing-3-data-b
    :component/type :editor/data-structure
    :evaluations/init :content/default-input
    :content/editable? false
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
           "Now that you have a hang of workflows, it's time to discuss how to tell Onyx what to do at each task ...")
   ])

(def section
  {:section/id :workflow-basics
   :section/layout [[::title]
                    [::leadin ::cheat-sheet-note]
                    [::graph ::graph-desc]
                    [::graph-data ::graph-data-desc ::task-definition]
                    
                    [::in-action-header]
                    [::in-action-leadin ::in-action-note]
                    [::in-action-input-segments]
                    [::in-action-simulator]
                    [::in-action-description ::segment-definition]
                    
                    [::implementing-1-header]
                    [::implementing-1-leadin]
                    [::implementing-1-data-a]
                    [::implementing-1-label-a ::implementing-1-label-b]
                    [::implementing-1-graph-a ::implementing-1-graph-b]

                    [::implementing-2-leadin]
                    [::implementing-2-data-a]
                    [::implementing-2-label-a ::implementing-2-label-b]
                    [::implementing-2-graph-a ::implementing-2-graph-b]

                    [::implementing-3-leadin]
                    [::implementing-3-data-a]
                    [::implementing-3-label-a ::implementing-3-label-b]
                    [::implementing-3-graph-a ::implementing-3-graph-b]

                    [::outro]
                    ]})
