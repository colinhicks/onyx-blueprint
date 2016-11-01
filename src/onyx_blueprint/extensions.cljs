(ns onyx-blueprint.extensions
  (:require [om.next :as om]))

(def ^:dynamic *custom-component-queries* {})

(defmulti component-ui (fn [props] (:component/type props)))

(defmulti parser-read om/dispatch)

(defmulti parser-mutate om/dispatch)

(defmulti validate (fn [spec value] spec))
