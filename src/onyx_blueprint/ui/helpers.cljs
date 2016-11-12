(ns onyx-blueprint.ui.helpers
  (:require [clojure.string :as str]
            [om.dom :as dom]))

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
  (str "col component"
       " component-" (namespace type) "-" (name type)
       (when (:className hints) (str " " (:className hints)))))

(defn label [{:keys [content/label]}]
  (when label
    (dom/span #js {:className "label"} label)))
