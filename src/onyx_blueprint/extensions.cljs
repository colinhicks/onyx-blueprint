(ns onyx-blueprint.extensions
  (:require [cljs.pprint :as pprint]
            [om.next :as om]
            [om.dom :as dom]))

(defmulti component-ui (fn [props] (:component/type props)))

(defmethod component-ui :default [props]
  (dom/pre nil (with-out-str (pprint/pprint props))))

(defmulti parser-read om/dispatch)

(defmulti parser-mutate om/dispatch)
