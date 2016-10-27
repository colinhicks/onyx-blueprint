(ns onyx-tutorial.ui.code-editor
  (:require [cljs.pprint :as pprint]
            [onyx-local-rt.api :as api]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [goog.dom :as gdom]
            [onyx-tutorial.extensions :as extensions]
            [goog.dom.classlist]
            [cljsjs.codemirror]
            [cljsjs.codemirror.addon.edit.closebrackets]
            [cljsjs.codemirror.addon.edit.matchbrackets]
            [cljsjs.codemirror.addon.runmode.runmode]
            [cljsjs.codemirror.addon.runmode.colorize]
            [cljsjs.codemirror.mode.clojure]))

(aset js/CodeMirror "keyMap" "default" "Shift-Tab" "indentLess")

(def editor-config
  {:mode "clojure"
   :autoCloseBrackets true
   :matchBrackets true})

(defn editor [input-id opts & css-classes]
  (let [el (.getElementById js/document input-id)]
    (when-not (= "none" (.. el -style -display))
      (let [ed (.fromTextArea js/CodeMirror el (clj->js (merge editor-config opts)))
            wrapper (.getWrapperElement ed)]
        (set! (.-id wrapper) (str input-id "-editor"))
        (js/goog.dom.classlist.addAll wrapper (clj->js (conj css-classes "editor")))
        ed))))

(defui CodeEditor
    Object
    (componentDidMount [this]
      (let [{:keys [component/id]} (om/props this)
            {:keys [handle-change]} (om/get-computed this)
            textarea-id (str id "-textarea")
            cm (editor textarea-id {})]
        (.on cm "change"
             (fn []
               (handle-change id (.getValue cm))))
        ;; sync initial value
        (handle-change id (.getValue cm))))

    (render [this]
      (let [{:keys [component/id component/content]} (om/props this)
            textarea-id (str id "-textarea")
            _ (println "cer" (keys content))]
        (dom/textarea #js {:id textarea-id
                           :defaultValue (:default-input content)}))))

(def code-editor (om/factory CodeEditor))

(defn handle-eval-fn [transact]
  (fn [id new-value]
    (transact `[(editor/eval {:type :onyx/fn
                              :source ~new-value
                              :component-id ~id
                              :script-id "user-input"})])))

(defmethod extensions/component-ui :editor/fn
  [props]
  (let [{:keys [transact]} (om/get-computed props)]
    (code-editor (om/computed props {:handle-change (handle-eval-fn transact)}))))

(defmethod extensions/component-ui :editor/data-structure
  [props]
  (let [{:keys [transact]} (om/get-computed props)]
    (code-editor (om/computed props {:handle-change (handle-eval-fn transact)}))))
