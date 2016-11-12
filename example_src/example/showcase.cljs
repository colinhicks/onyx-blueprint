(ns example.showcase
  (:require [goog.dom :as gdom]
            [onyx-blueprint.api :as api]))

(enable-console-print!)

(def components
  [{:component/id ::title
    :component/type :blueprint/html
    :content/tag :h1
    :content/text "Onyx Blueprint Showcase"}

   {:component/id ::leadin
    :component/type :blueprint/html
    :content/hiccup
    [:div
     [:p "This page displays default UI components available to Onyx Blueprint. You configure components with EDN. And you configure the layout of those components with EDN. The rendering engine does the rest."]
     [:p "Onyx Blueprint is rendering this page. For best results, follow along in the " [:code "example.showcase"] " source."]]}

   {:component/id ::text-subhead
    :component/type :blueprint/html
    :content/tag :h2
    :content/text "Text elements"}

   {:component/id ::text-intro
    :component/type :blueprint/html
    :content/tag :p
    :content/text "All Onyx Blueprint components are dynamic, including text. You'll need to configure what text to render and its relationship to html output."}

   {:component/id ::aengelberg-quote
    :component/type :blueprint/html
    :content/hiccup
    [:blockquote
     [:p [:em "This is the Clojure philosophy of how to create a library: figure out how your problem can be expressed as data, write functions to build that kind of data, and then write functions to consume that kind of data."]]
     [:p "—Alex Engelberg"]]}
   
   {:component/id ::text-string-one
    :component/type :blueprint/html
    :content/tag :p
    :content/text "You can render a string into a common html tag of your choosing."}
   
   {:component/id ::text-string-multiple
    :component/type :blueprint/html
    :content/tag :pre
    :content/text ["If you specify a vector of strings ... "
                   "Each string will be rendered into the configured tag."
                   "(The tags will be wrapped with a div.)"]}

   {:component/id ::text-hiccup
    :component/type :blueprint/html
    :content/hiccup
    [:div
     [:p "If you need to do something more " [:strong [:em [:u "complicated"]]]
      ", you can configure your component with " [:a {:href "https://github.com/weavejester/hiccup#syntax"}
                                                  "hiccup-like syntax"] "."]
     [:aside
      [:p "Nota bene: The Onyx tutorial code applies styling conventions to particular html elements, like " [:code "<aside/>"] "."]]]}
   
   {:component/id ::user-code-subhead
    :component/type :blueprint/html
    :content/tag :h2
    :content/text "Code editor"}

   {:component/id ::user-code-intro
    :component/type :blueprint/html
    :content/tag :p
    :content/text
    ["If you want to let users update code, a code editor is a good choice."
     "For now, we always assume text entered into a code editor is ClojureScript. You can specify default code for each editor."]}

   {:component/id ::user-code-ex
    :component/type :blueprint/editor
    :evaluations/validate-spec :onyx.core/workflow
    :evaluations/init :content/default-input
    :content/default-input [[:read-input :increment-n]
                            [:increment-n :square-n]
                            [:square-n :write-output]]}
   
   {:component/id ::user-code-evaluations
    :component/type :blueprint/html
    :content/hiccup
    [:div
     [:p "The code is automatically evaluated when changes occur. If you want the default code to be pre-evaluated, set the editor component's "
      [:code ":evaluations/init"] " key to " [:code ":content/default-input"] "."]
     [:p "If you want the code to be validated against a known Clojure spec, leverage the "
      [:code ":evaluations/validate-spec"] " property. " [:em "More on this to come."]]]}

   {:component/id ::user-code-read-only
    :component/type :blueprint/html
    :content/tag :p
    :content/text "If you don't want code to be edited, you can mark it read-only."}

   {:component/id ::user-code-ex-read-only
    :component/type :blueprint/editor
    :evaluations/init :content/default-input
    :content/read-only? true
    :content/default-input ";; Code editor input is evaluated in the cljs.user namespace.
;; Because they are marked for export, these functions are available to JavaScript.\n
(defn ^:export increment-n [segment]\n  (update-in segment [:n] inc))
(defn ^:export square-n [segment]\n  (update-in segment [:n] (partial * (:n segment))))"}
   
   {:component/id ::graph-subhead
    :component/type :blueprint/html
    :content/tag :h2
    :content/text "Workflow graphs"}

   {:component/id ::graph-intro
    :component/type :blueprint/html
    :content/tag :p
    :content/text "Visualize an Onyx workflow using the graph component. Each graph must be linked to a code editor that defines its graph data structure."}

   {:component/id ::graph-ex
    :component/type :blueprint/graph
    :link/evaluations {:workflow ::user-code-ex}}

   {:component/id ::simulator-subhead
    :component/type :blueprint/html
    :content/tag :h2
    :content/text "Job simulator"}

   {:component/id ::simulator-intro
    :component/type :blueprint/html
    :content/hiccup
    [:div
     [:p "Simulate the execution of a job with the simulator component. The simulator leverages "
      [:a {:href "https://github.com/onyx-platform/onyx-local-rt"} "onyx-local-rt"]
      " and empowers the user with command over the job execution lifecycle."]
     [:p "Use the " [:code ":link/evaluations"] " property to aggregate a job's configuration from other components. The " [:code ":content/controls"] " property specifies controls available to the user."]]}
   
   {:component/id ::simulator
    :component/type :blueprint/simulator
    :content/label "Job controls"
    :content/controls [:initialize :next-tick :next-batch :run-to-completion]
    :link/evaluations {:job-env ::simulator
                       :workflow ::user-code-ex
                       :catalog ::simulator-catalog
                       :input-segments ::simulator-input-segments}}

   {:component/id ::simulator-catalog
    :component/type :blueprint/editor
    :evaluations/init :content/default-input
    :evaluations/validate-spec :onyx.core/catalog
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

   {:component/id ::simulator-input-segments
    :component/type :blueprint/editor
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

   {:component/id ::inspector-subhead
    :component/type :blueprint/html
    :content/tag :h2
    :content/text "Job inspector"}

   {:component/id ::inspector-intro
    :component/type :blueprint/html
    :content/hiccup
    [:div
     [:p "Given a linked graph and simulator, the inspector displays the running job environment summary corresponding to the task selected on the linked graph."]]}

   {:component/id ::inspector
    :component/type :blueprint/job-inspector
    :content/label "Task status"
    :link/evaluations {:job-env ::simulator}
    :link/ui-state {:graph ::graph-ex}}
   
   {:component/id ::validation-subhead
    :component/type :blueprint/html
    :content/tag :h2
    :content/text "Validation results"}

   {:component/id ::validation-intro
    :component/type :blueprint/html
    :content/tag :p
    :content/text ["Display compiler errors and spec validation explanations for the linked components using the auditor component."]}

   {:component/id ::validation
    :component/type :blueprint/auditor
    :content/label "Code analysis"
    :link/evaluations {:workflow ::user-code-ex}}
   ])
   
(def sections
  [{:section/id ::top-section
    :section/layout [[::title]
                     [::leadin]]}

   {:section/id ::text-section
    :section/layout [[::text-subhead]
                     [::text-intro]
                     [::aengelberg-quote]
                     [::text-string-one]
                     [::text-string-multiple]
                     [::text-hiccup]]}

   {:section/id ::user-code-section
    :section/layout [[::user-code-subhead]
                     [::user-code-intro]
                     [::user-code-ex]
                     [::user-code-evaluations]
                     [::user-code-read-only]
                     [::user-code-ex-read-only]]}
   
   {:section/id ::graph-section
    :section/layout [[::graph-subhead]
                     [::graph-intro]
                     '[(::graph-ex {:graph-direction "LR"})]]}

   {:section/id ::simulator-section
    :section/layout [[::simulator-subhead]
                     [::simulator-intro]
                     [::simulator]]}

   {:section/id ::inspector-section
    :section/layout [[::inspector-subhead]
                     [::inspector-intro]
                     [::inspector]]}
   
   {:section/id ::validation-section
    :section/layout [[::validation-subhead]
                     [::validation-intro]
                     [::validation]]}
   ])

(api/render-tutorial! components sections (gdom/getElement "app"))
