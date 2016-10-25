(ns onyx-tutorial.core
  (:require [cljs.js :as cljs]
            [cljs.pprint :as pprint]
            [onyx-local-rt.api :as api]
            [goog.dom.classlist]
            [cljsjs.codemirror]
            [cljsjs.codemirror.addon.edit.closebrackets]
            [cljsjs.codemirror.addon.edit.matchbrackets]
            [cljsjs.codemirror.addon.runmode.runmode]
            [cljsjs.codemirror.addon.runmode.colorize]
            [cljsjs.codemirror.mode.clojure]))

(enable-console-print!)

(set! (.-user js/cljs) #js {})

(defonce compiler-state (cljs/empty-state))

(defn eval-str [str name cb]
  (cljs/eval-str compiler-state
                 str
                 name
                 {:eval cljs/js-eval}
                 (fn [{:keys [error value]}]
                   (if error
                     (println error)
                     (cb value)))))

(def editor-config
  {:mode "clojure"
   :autoCloseBrackets true
   :matchBrackets true})

(aset js/CodeMirror "keyMap" "default" "Shift-Tab" "indentLess")

(defn editor [input-id opts & css-classes]
  (let [el (.getElementById js/document input-id)]
    (when-not (= "none" (.. el -style -display))
      (let [ed (.fromTextArea js/CodeMirror el (clj->js (merge editor-config opts)))
            wrapper (.getWrapperElement ed)]
        (set! (.-id wrapper) (str input-id "-editor"))
        (js/goog.dom.classlist.addAll wrapper (clj->js (conj css-classes "editor")))
        ed))))

(defn eval-editor [editor cb]
  (eval-str (.getValue editor)
            (str "user-defined-" (.. editor getTextArea -id))
            cb))

(defn run-job [job]
  (-> (api/init job)
      (api/new-segment :in {:n 41})
      (api/new-segment :in {:n 84})
      (api/drain)
      (api/stop)
      (api/env-summary)))

(defn setup-ui []
  (let [fn-editor (editor "fn" {} "enabled-editor")
        job-editor (editor "job" {} "enabled-editor")
        result-viewer (editor "result" {:readOnly true} "result-viewer")]
    
    (.addEventListener js/document
                       "submit"
                       (fn [evt]
                         (.preventDefault evt)
                         (eval-editor fn-editor identity)
                         (eval-editor job-editor #(->> (run-job %)
                                                       (pprint/pprint)
                                                       (with-out-str)
                                                       (.setValue result-viewer)))))))

(defn on-js-reload []
  )

(setup-ui)
