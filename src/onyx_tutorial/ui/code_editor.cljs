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

(defn textarea-id [component-id]
  (str (name component-id) "-textarea"))

(defui CodeEditor
    static om/IQuery
    (query [this]
      [:component/id :component/type :component/content])
    
    Object
    (componentDidMount [this]
      (let [{:keys [component/id]} (om/props this)
            cm (editor (textarea-id id) {})]
        ;; todo debounce
        (.on cm "change"
             (fn []
               (om/transact! this `[(editor/eval {:type :onyx/fn
                                                  :source ~(.getValue cm)
                                                  :component-id ~id
                                                  :script-id "user-input"})
                                    :tutorial/sections])))))

    (render [this]
      (let [{:keys [component/id component/content] :as props} (om/props this)]
        (dom/textarea #js {:id (textarea-id id)
                           :defaultValue (:default-input content)}))))

(def code-editor (om/factory CodeEditor))

(defmethod extensions/component-ui :editor/fn
  [props]
  (code-editor props))

(defmethod extensions/component-ui :editor/data-structure
  [props]
  (code-editor props))
