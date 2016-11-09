(ns example.showcase
  (:require [goog.dom :as gdom]
            [onyx-blueprint.api :as api]))

(enable-console-print!)

(def components
  [{:component/id ::title
    :component/type :html/element
    :content/tag :h1
    :content/text "Onyx Blueprint Showcase"}

   {:component/id ::leadin
    :component/type :html/hiccup
    :content/hiccup
    [:div
     [:p "This page displays default UI components available to Onyx Blueprint. You configure components with EDN. And you configure the layout of those components with EDN. The rendering engine does the rest."]
     [:p "Onyx Blueprint is rendering this page. For best results, follow along in the " [:code "example.showcase"] " source."]]}

   {:component/id ::text-subhead
    :component/type :html/element
    :content/tag :h2
    :content/text "Text elements"}

   {:component/id ::text-intro
    :component/type :html/element
    :content/tag :p
    :content/text "All Onyx Blueprint components are dynamic, including text. You'll need to configure what text to render and its relationship to html output."}

   {:component/id ::aengelberg-quote
    :component/type :html/hiccup
    :content/hiccup
    [:blockquote
     [:p [:em "This is the Clojure philosophy of how to create a library: figure out how your problem can be expressed as data, write functions to build that kind of data, and then write functions to consume that kind of data."]]
     [:p "—Alex Engelberg"]]}
   
   {:component/id ::text-string-one
    :component/type :html/element
    :content/tag :p
    :content/text "You can render a string into a common html tag of your choosing."}
   
   {:component/id ::text-string-multiple
    :component/type :html/element
    :content/tag :pre
    :content/text ["If you specify a vector of strings ... "
                   "Each string will be rendered into the configured tag."
                   "(The tags will be wrapped with a div.)"]}

   {:component/id ::text-hiccup
    :component/type :html/hiccup
    :content/hiccup
    [:div
     [:p "If you need to do something more " [:strong [:em [:u "complicated"]]]
      ", you can configure your component with " [:a {:href "https://github.com/weavejester/hiccup#syntax"}
                                                  "hiccup-like syntax"] "."]
     [:aside
      [:p "Nota bene: The Onyx tutorial code applies styling conventions to particular html elements, like " [:code "<aside/>"] "."]]]}
   
   {:component/id ::user-code-subhead
    :component/type :html/element
    :content/tag :h2
    :content/text "Code editor"}

   {:component/id ::user-code-intro
    :component/type :html/element
    :content/tag :p
    :content/text
    ["If you want to let users update code, a code editor is a good choice."
     "For now, we always assume text entered into a code editor is ClojureScript. You can specify default code for each editor."]}

   {:component/id ::user-code-ex
    :component/type :editor/data-structure
    :evaluations/validate-spec :onyx.core/workflow
    :evaluations/init :content/default-input
    :content/default-input [[:a :b] [:b :c]]}
   
   {:component/id ::user-code-evaluations
    :component/type :html/hiccup
    :content/hiccup
    [:div
     [:p "The code is automatically evaluated when changes occur. If you want the default code to be pre-evaluated, set the editor component's "
      [:code ":evaluations/init"] " key to " [:code ":content/default-input"] "."]
     [:p "If you want the code to be validated against a known Clojure spec, leverage the "
      [:code ":evaluations/validate-spec"] " property. " [:em "More on this to come."]]]}

   {:component/id ::user-code-read-only
    :component/type :html/element
    :content/tag :p
    :content/text "If you don't want code to be edited, you can mark it read-only."}

   {:component/id ::user-code-ex-read-only
    :component/type :editor/data-structure
    :evaluations/init :content/default-input
    :content/read-only? true
    :content/default-input ";; Code editor input is evaluated in the cljs.user namespace.
;; Because it's marked for export, this fn is available to JavaScript as cljs.user.foo()).\n
(defn ^:export foo []
  (println \"foo\"))"}
   
   {:component/id ::graph-subhead
    :component/type :html/element
    :content/tag :h2
    :content/text "Workflow graphs"}

   {:component/id ::graph-intro
    :component/type :html/element
    :content/tag :p
    :content/text "Visualize an Onyx workflow using the graph component. Each graph must be linked to a code editor that defines its graph data structure."}

   {:component/id ::graph-ex
    :component/type :graph/workflow
    :evaluations/link {:workflow ::user-code-ex}}

   {:component/id ::simulator-subhead
    :component/type :html/element
    :content/tag :h2
    :content/text "Job simulator"}

   {:component/id ::simulator-intro
    :component/type :html/element
    :content/tag :p
    :content/text ["Simulate the execution of a job with the simulator component. Uses onyx-local-rt."
                   "Further description to come."]}

   ;; todo simulator ex

   {:component/id ::validation-subhead
    :component/type :html/element
    :content/tag :h2
    :content/text "Validation results"}

   {:component/id ::validation-intro
    :component/type :html/element
    :content/tag :p
    :content/text ["Display compiler errors and spec validation explanations for the linked code editor using the validation component."
                   "Further description to come."]}
   ])
   
(def sections
  [{:section/id ::top
    :section/layout [[::title]
                     [::leadin]]}

   {:section/id ::text
    :section/layout [[::text-subhead]
                     [::text-intro]
                     [::aengelberg-quote]
                     [::text-string-one]
                     [::text-string-multiple]
                     [::text-hiccup]]}

   {:section/id ::user-code
    :section/layout [[::user-code-subhead]
                     [::user-code-intro]
                     [::user-code-ex]
                     [::user-code-evaluations]
                     [::user-code-read-only]
                     [::user-code-ex-read-only]]}
   
   {:section/id ::graph
    :section/layout [[::graph-subhead]
                     [::graph-intro]
                     [::graph-ex]]}

   {:section/id ::simulator
    :section/layout [[::simulator-subhead]
                     [::simulator-intro]]}
   
   {:section/id ::validation
    :section/layout [[::validation-subhead]
                     [::validation-intro]]}
   ])

(api/render-tutorial! components sections (gdom/getElement "app"))
