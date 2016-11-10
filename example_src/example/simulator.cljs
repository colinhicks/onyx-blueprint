(ns example.simulator
  (:require [goog.dom :as gdom]
            [onyx-blueprint.api :as api]))

(enable-console-print!)

(def components
  [{:component/id ::workflow
    :component/type :editor/data-structure
    :evaluations/init :content/default-input
    :content/default-input [[:read-input :increment-n]
                            [:increment-n :square-n]
                            [:square-n :write-output]]}
   
   {:component/id ::graph
    :component/type :graph/workflow
    :content/graph-direction "LR"
    :evaluations/link {:workflow ::workflow
                       :simulator ::simulator}}

   {:component/id ::catalog
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

   {:component/id ::fns
    :component/type :editor/fn
    :evaluations/init :content/default-input
    :content/default-input
    "(defn ^:export increment-n [segment] (update-in segment [:n] inc))
(defn ^:export square-n [segment] (update-in segment [:n] (partial * (:n segment))))"}
   
   {:component/id ::simulator
    :component/type :simulator/default
    :evaluations/link {:workflow ::workflow
                       :catalog ::catalog}

    
    }])

(def sections
  [{:section/id ::graph-example
    :section/layout [[::graph]
                     [::workflow ::catalog ::fns]
                     [::simulator]]}])

(api/render-tutorial! components sections (gdom/getElement "app"))
