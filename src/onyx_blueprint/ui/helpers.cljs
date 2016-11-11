(ns onyx-blueprint.ui.helpers
  (:require [clojure.string :as str]))

(defn keyword->attr-val [kw]
  (-> kw
      (str)
      (subs 1)
      (str/replace #"/" "--")
      (str/replace #"\." "_")))

(defn component-id [{:keys [component/id layout/hints]}]
  (or (:id hints)
      (keyword->attr-val id)))

(defn component-css-classes [{:keys [component/type layout/hints]}]
  (let [ns (namespace type)]
    (str "col component"
         " component-" ns
         " component-" ns "-" (name type)
         " " (:className hints))))
