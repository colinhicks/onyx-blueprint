(ns onyx-tutorial.self-host
  (:require [cljs.js :as cljs]
            [cljs.analyzer :as analyzer]))


(set! (.-user js/cljs) #js {})

(defonce compiler-state (cljs/empty-state))

;; from Quil
(defn convert-warning [warning]
  (let [{:keys [type env extra]} warning]
    {:message (analyzer/error-message type extra)
     :type :warning
     :line (:line env)
     :column (:column env)}))

(defn eval-str [str name cb]
  (let [warnings (atom [])]
    (binding [analyzer/*cljs-warning-handlers*
              [(fn [type env extra]
                 (swap! warnings conj {:type type :env env :extra extra}))]]
      (cljs/eval-str compiler-state
                     str
                     name
                     {:eval cljs/js-eval}
                     (fn [result]
                       (let [result' (assoc result :warnings
                                            (into [] (distinct (map convert-warning @warnings))))]
                         (cb result')))))))
