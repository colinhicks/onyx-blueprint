(ns onyx-tutorial.builders)

(defn html
  [tag id text]
  {:component/id id
   :component/type :blueprint/html
   :content/tag tag
   :content/text text})

(defn hiccup [id edn]
  {:component/id id
   :component/type :blueprint/html
   :content/hiccup edn})

(def header (partial html :h2))
(def body (partial html :p))

