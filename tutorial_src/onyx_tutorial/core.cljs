(ns onyx-tutorial.core
  (:require [goog.dom :as gdom]
            [onyx-blueprint.api :as api]
            [onyx-tutorial.workflow-basics :as workflow-basics]))


(def components
  (concat
        workflow-basics/components))

(def sections
  [workflow-basics/section])

(api/render-tutorial! components sections (gdom/getElement "app"))
