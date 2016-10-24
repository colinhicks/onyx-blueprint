(ns onyx-tutorial.core
  (:require [cljs.js :as cljs]
            [cljs.pprint :as pprint]
            [onyx-local-rt.api :as api]))

(enable-console-print!)

(defonce compiler-state (cljs/empty-state))

(defn update-result [value]
  (let [result (.getElementById js/document "result")]
    (aset result "value" (with-out-str (pprint/pprint value)))))

(defn eval-str [str name cb]
  (cljs/eval-str compiler-state
                 str
                 name
                 {:eval cljs/js-eval}
                 (fn [{:keys [error value]}]
                   (if error
                     (println error)
                     (cb value)))))

(defn run-job [job]
  (-> (api/init job)
      (api/new-segment :in {:n 41})
      (api/new-segment :in {:n 84})
      (api/drain)
      (api/stop)
      (api/env-summary)))

(defn eval-input [input-id cb]
  (eval-str (.-value (.getElementById js/document input-id)) input-id cb))

(defn setup-ui []
  (.addEventListener js/document
                     "submit"
                     (fn [evt]
                       (.preventDefault evt)
                       (eval-input "fn" identity)
                       (eval-input "job" #(update-result (run-job %))))))

(setup-ui)
