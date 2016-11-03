(ns onyx-tutorial.workflow-basics
  (:require [onyx-tutorial.builders :as b]))


(def components
  [(b/header ::title "Workflows")
   
   (b/body ::intro "In Onyx, we represent data control flow using workflows.")

   (b/hiccup ::test [:h1 "test"])

   (b/body ::denoting "Denote a workflow using a vector of vectors.")

   (b/body ::structure-inner "For each inner vector, the first element is a source, and the second element is a destination.")

   (b/body ::structure "The roots of the graph must be input tasks, and the leaves must be output tasks.")
   ])

(def section
  {:section/id :workflow-basics
   :section/layout [[::title] [::test]
                    [::intro]]})


