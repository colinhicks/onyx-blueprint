(ns onyx-tutorial.workflow-basics
  (:require [onyx-tutorial.builders :as b]))


(def components
  [(b/header ::title
             "Workflows")
   
   (b/body ::intro
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
           "The representation of a workflow is a Clojure vector of vectors. The inner vectors contain two elements: source and destination, respectively.")
   
   {:component/id ::graph-data
    :component/type :editor/data-structure
    :evaluations/init :content/default-input
    :content/default-input [[:read-segments :increment-n]
                            [:increment-n :square-n]
                            [:square-n :write-segments]]}

   (b/header ::in-action-header "A workflow in action")
   
   (b/body ::in-action-intro
           ["Later in the tutorial, you'll learn how to tell Onyx to do your bidding at each step in the workflow. For the moment, this has been done for you behind the scenes."
            "Let's take a look at the workflow's output given the following input ..."])

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

   {:component/id ::in-action-catalog
    :component/type :editor/data-structure
    :evaluations/init :content/default-input
    :content/default-input [{:onyx/name :read-segments
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
                            {:onyx/name :write-segments
                             :onyx/type :output
                             :onyx/batch-size 1}]}

   {:component/id ::in-action-fns
    :component/type :editor/fn
    :evaluations/init :content/default-input
    :content/default-input
    "(defn ^:export increment-n [segment] (update-in segment [:n] inc))
     (defn ^:export square-n [segment] (update-in segment [:n] (partial * (:n segment))))"}
   ])

(def section
  {:section/id :workflow-basics
   :section/layout [[::title]
                    [::intro ::cheat-sheet-note]
                    [::graph ::graph-desc]
                    [::graph-data ::graph-data-desc]
                    [::in-action-header]
                    [::in-action-intro]
                    [::in-action-input-segments]
                    [::in-action-simulator]]})


