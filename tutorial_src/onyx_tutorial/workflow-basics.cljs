(ns onyx-tutorial.workflow-basics)

(defn text
  ([id txt] (text :text/body id text))
  ([type id txt]
   {:component/id id
    :component/type type
    :content/text txt}))

(def components
  [{:component/id ::title
    :component/type :text/header
    :content/text "Workflows"}

   (text ::intro "In Onyx, we represent data control flow using workflows.")

   (text ::denoting "Denote a workflow using a vector of vectors.")

   (text ::structure-inner "For each inner vector, the first element is a source, and the second element is a destination.")

   (text ::structure "The roots of the graph must be input tasks, and the leaves must be output tasks.")
   ])

(def section
  {:section/id :workflow-basics
   :section/layout [[::title]
                    [::intro]]})


