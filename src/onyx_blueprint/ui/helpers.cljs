(ns onyx-blueprint.ui.helpers)

(defn component-css-classes [{:keys [component/type layout/hints]}]
  (let [ns (namespace type)]
    (str "col component"
         " component-" ns
         " component-" ns "-" (name type)
         " " (:className hints))))
