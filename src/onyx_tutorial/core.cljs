(ns onyx-tutorial.core
  (:require [cljs.js :as cljs]
            [cljs.analyzer :as analyzer]
            [cljs.pprint :as pprint]
            [onyx-local-rt.api :as api]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [goog.dom :as gdom]
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
                                            (distinct (map convert-warning @warnings)))]
                         (cb result')))))))

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

(defn into-tree [components layouts]
  (mapv (fn [{:keys [section/id section/layout]}]
          {:section/id id
           :section/rows
           (mapv (fn [items]
                   {:row/items (mapv
                            (fn [cid]
                              (let [component (some #(when (= cid (:component/id %)) %) components)]
                                (assoc component :section/id id)))
                            items)})
                 layout)})
        layouts))

(def components
  [{:component/id ::workflow-header
    :component/type :text/header
    :component/content {:text "Workflow"}}
   
   {:component/id ::workflow-editor
    :component/type :editor/data-structure
    :component/content {:default-input "[[:a :b] [:b :c]]"}}

   {:component/id ::task-fn
    :component/type :editor/fn
    :component/content {:default-input "(defn ^:export my-inc [segment]\n  (update-in segment [:n] inc))"}}])

(def sections
  [{:section/id ::workflow
    :section/layout [[::workflow-header]
                     [::workflow-editor]]}

   {:section/id ::simple-task
    :section/layout [[::task-fn]]}])

(def init-data
  {:tutorial/sections (into-tree components sections)})

(defmulti read om/dispatch)

(defmethod read :default
  [{:keys [state query parser] :as env} key params]
  (let [st @state
        v (get st key)
        _ (println "--read" key query)
        ]
    (if v
      {:value v}
      {:value :not-found})))

(defn denormalize-section [st section]
  (update-in section [:section/rows]
             (fn [rows]
               (mapv (fn [row]
                       (update-in row [:row/items]
                                  (fn [items]
                                    (mapv (fn [item-ref]
                                            (get-in st item-ref))
                                          items))))
                     rows))))

(defmethod read :tutorial/sections
  [{:keys [state query parser target] :as env} key params]
  (let [st @state
        sections (mapv (partial denormalize-section st) (key st))]
    {:value sections}))

(defmulti mutate om/dispatch)

(defmethod mutate 'editor/eval
  [{:keys [state] :as env} key {:keys [type source script-id component-id] :as params}]
  {:value {:keys []}
   :compile true
   :action #(swap! state assoc-in
                   [:tutorial/components
                    component-id
                    :component/content
                    :compile-state]
                   :compiling)})

(defmulti component-ui (fn [props] (:component/type props)))

(defmethod component-ui :default [props]
  (dom/pre nil (with-out-str (pprint/pprint props))))

(defui Component
    static om/Ident
    (ident [this {:keys [component/id]}]
      [:tutorial/components id])

    static om/IQuery
    (query [this]
      [:component/id :component/type :component/content])

    Object
    (render [this]
      (let [props (-> (om/props this)
                      (om/computed {:transact (fn [expr]
                                                (om/transact! this expr))}))]
        (component-ui props))))

(def component (om/factory Component))

(defui Section
    static om/IQuery
    (query [this]
      [:section/id
       {:section/rows [{:row/items (om/get-query Component)}]}])

    Object
    (render [this]
      (let [{:keys [section/id section/rows] :as props} (om/props this)]
        (apply dom/div #js {:id (name id) :className "section"} ; todo namespace-based html id
               (mapv (fn [{:keys [row/items]}]
                       (apply dom/div #js {:className "row"} (mapv component items)))
                     rows)))))

(def section (om/factory Section))

(defui Tutorial
    static om/IQuery
    (query [this]
      [{:tutorial/sections (om/get-query Section)}])

    Object
    (render [this]
      (let [{:keys [tutorial/sections] :as props} (om/props this)]
        (apply dom/div nil (mapv section sections)))))

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

(defmethod component-ui :editor/fn
  [props]
  (let [{:keys [transact]} (om/get-computed props)]
    (code-editor (om/computed props {:handle-change (handle-eval-fn transact)}))))

(defmethod component-ui :editor/data-structure
  [props]
  (let [{:keys [transact]} (om/get-computed props)]
    (code-editor (om/computed props {:handle-change (handle-eval-fn transact)}))))

(defui CodeOutput
    static om/IQuery
    (query [this]
      [:editor/output])
    
    Object
    (render [this]
      (let [{:keys [editor/output]} (om/props this)]
        (dom/pre nil (str output)))))

(def code-output (om/factory CodeOutput))

(defn io-compile [cb {:keys [source script-id component-id]}]
  (eval-str source script-id
            (fn [result]
              ;; todo: is there a better way to do this?
              (let [merge-payload
                    {:onyx-tutorial/merge-in
                     {:content {:compile-result result
                                :compile-state (if (seq (:warnings result))
                                                  :compiled-error
                                                  :compiled-success)}
                      :keypath [:tutorial/components
                                component-id
                                :component/content]}}]
                  (cb merge-payload)))))

(defn io [{:keys [compile]} cb]
  (when compile
    (->> compile
         (om/query->ast)
         :children
         (map :params)
         (run! (partial io-compile cb)))))

(defn merge-tree [st {:keys [onyx-tutorial/merge-in] :as novelty}]
  (if merge-in
    (update-in st (:keypath merge-in)
               #(merge % (:content merge-in)))
    (om/default-merge-tree st novelty)))

(def reconciler
  (om/reconciler
   {:state (atom (om/tree->db Tutorial init-data true))
    :parser (om/parser {:read read :mutate mutate})
    :send io
    :merge-tree merge-tree
    :remotes [:compile]}))

(om/add-root! reconciler Tutorial (gdom/getElement "app"))

;;(pprint/pprint (om/tree->db Tutorial init-data true))

;;(pprint/pprint @reconciler)


(defn on-js-reload []
  )

