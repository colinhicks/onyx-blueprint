(ns onyx-blueprint.ui.code-editor
  (:require [clojure.string :as str]
            [cljs.pprint :as pprint]
            [cljsjs.codemirror]
            [cljsjs.codemirror.addon.edit.closebrackets]
            [cljsjs.codemirror.addon.edit.matchbrackets]
            [cljsjs.codemirror.addon.runmode.colorize]
            [cljsjs.codemirror.addon.runmode.runmode]
            [cljsjs.codemirror.mode.clojure]
            [goog.dom :as gdom]
            [goog.dom.classlist]
            [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            [onyx-blueprint.extensions :as extensions]
            [onyx-blueprint.ui.helpers :as helpers]
            [onyx-local-rt.api :as api]))

(aset js/CodeMirror "keyMap" "default" "Shift-Tab" "indentLess")

(def editor-config
  {:mode "clojure"
   :autoCloseBrackets true
   :matchBrackets true})

(defn editor [input-id opts]
  (let [el (.getElementById js/document input-id)]
    (when-not (= "none" (.. el -style -display))
      (let [ed (.fromTextArea js/CodeMirror el (clj->js (merge editor-config opts)))
            wrapper (.getWrapperElement ed)]
        (set! (.-id wrapper) (str input-id "-editor"))
        ed))))

(defn textarea-id [component-id]
  (str (helpers/keyword->attr-val component-id)
       "-textarea"))

(defn format-default-input [default-input]
  (if (string? default-input)
    default-input
    (-> default-input
        pprint/pprint
        with-out-str)))

(defui CodeEditor
    static om/IQuery
    (query [this]
      [:component/id :component/type :content/label
       :content/read-only? :content/default-input :layout/hints])
    
    Object
    (componentDidMount [this]
      (let [{:keys [component/id content/read-only?] :as props} (om/props this)
            {:keys [editor/codemirror-opts]} (om/get-computed this)
            codemirror-opts (if read-only?
                              (assoc codemirror-opts :readOnly true)
                              codemirror-opts)
            cm (editor (textarea-id id) codemirror-opts)]
        ;; todo debounce
        (.on cm "change"
             (fn []
               (om/transact! this `[(evaluations/evaluate {:type :onyx/fn
                                                           :source ~(.getValue cm)
                                                           :component-id ~id
                                                           :script-id "user-input"})
                                    :blueprint/sections])))))

    (render [this]
      (let [{:keys [component/id content/default-input] :as props} (om/props this)]
        (dom/div #js {:id (helpers/component-id props)
                      :className (helpers/component-css-classes props)}
                 (helpers/label props)
                 (dom/textarea #js {:id (textarea-id id)
                                    :defaultValue (format-default-input default-input)})))))

(def code-editor (om/factory CodeEditor))

(defmethod extensions/component-ui :editor/fn
  [props]
  (code-editor props))

(defmethod extensions/component-ui :editor/data-structure
  [props]
  (code-editor props))
