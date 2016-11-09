(defproject org.onyxplatform/onyx-blueprint "0.1.0-SNAPSHOT"
  :description "Support for learning Onyx interactively"
  :url "https://github.com/onyx-platform/onyx-blueprint"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [org.onyxplatform/onyx-local-rt "0.9.11.0"]
                 [org.omcljs/om "1.0.0-alpha47"]
                 [cljsjs/codemirror "5.19.0-0"]
                 [sablono "0.7.5"]]

  :plugins [[lein-figwheel "0.5.8"]
            [lein-cljsbuild "1.1.4" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src" "example_src" "tutorial_src"]

  :clean-targets ^{:protect false} ["resources/public/example/js"
                                    "resources/public/tutorial/js"
                                    "target"]

  :cljsbuild {:builds
              ;; a showcase of available components
              [{:id "showcase-dev"
                :source-paths ["src" "example_src"]

                :figwheel {:open-urls ["http://localhost:3449/showcase.html"]}

                :compiler {:main example.showcase
                           :asset-path "example/js/out"
                           :output-to "resources/public/example/js/showcase.js"
                           :output-dir "resources/public/example/js/out"
                           :source-map-timestamp true
                           :preloads [devtools.preload]}}

               ;; the combined tutorial build with code-splitting
               {:id "tutorial-all"
                :source-paths ["src" "tutorial_src"]
                :compiler {:output-dir "resources/public/tutorial/js"
                           :optimizations :simple
                           :pretty-print false
                           :modules {:common {:output-to "resources/public/tutorial/js/common.js"
                                              :entries #{"onyx-tutorial.core"}}
                                     :workflow {:output-to "resources/public/tutorial/js/workflow.js"
                                                :entries #{"onyx-tutorial.workflow-basics"}
                                                :depends-on #{:common}}}}}

               ;; the "workflow" topic
               {:id "workflow-dev"
                :source-paths ["src" "tutorial_src"]

                :figwheel {:open-urls ["http://localhost:3449/workflow.html"]}

                :compiler {:main onyx-tutorial.workflow
                           :asset-path "tutorial/js/out"
                           :output-to "resources/public/tutorial/js/workflow.js"
                           :output-dir "resources/public/tutorial/js/out"
                           :source-map-timestamp true
                           :preloads [devtools.preload]}}]}

  :figwheel {:css-dirs ["resources/public/css"]}

  :profiles {:dev {:dependencies [[binaryage/devtools "0.8.2"]
                                  [figwheel-sidecar "0.5.8"]
                                  [com.cemerick/piggieback "0.2.1"]]
                   :source-paths ["src" "example_src" "dev"]
                   
                   :repl-options { :init (set! *print-length* 50)
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}})
