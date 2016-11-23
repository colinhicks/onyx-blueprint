(ns onyx-blueprint.ui.graph.segviz
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [goog.dom :as gdom]
            [monet.canvas :as canvas]
            [tween-clj.core :as tween]
            [hexpacker-stitch-lite.core :as stitch]))

(defn can-render? [job-env]
  (->> job-env
       :tasks
       (some (fn [[_ t]] (seq (:inbox t))))
       seq))

(defn js-obj->map [x]
  (->> (js/Object.keys x)
       (map (fn [k] [(keyword k) (aget x k)]))
       (into {})))

(defn batch-unit [type source dest segments idx timestamp]
  (let [id (keyword (str (name source)
                         (when dest (str "-to-" (name dest)))
                         "-"
                         idx
                         "-"
                         timestamp))]
    (with-meta {:type type
                :source source
                :dest dest
                :segments segments}
      {:id id})))

(defn batch [job-env {:keys [tasks timestamp] :as checkpoint}]
  (into #{}
        (mapcat
         (fn [[source {:keys [inbox outputs]}]]
           (let [{:keys [event children]} (get-in job-env [:tasks source])
                 {:keys [onyx/batch-size onyx/type]} (-> event :onyx.core/task-map)]
             (->> inbox
                  (repeat)
                  (mapcat (fn [dest inbox]
                            (map (fn [idx segments]
                                   (batch-unit type source dest segments idx timestamp))
                                 (range)
                                 (partition-all batch-size inbox)))
                          (or (seq children) [::leaf]))))))
        tasks))

(defn enqueue-batches! [{:keys [state] :as segviz} job-env]
  (let [last-stamp (:last-checkpoint-timestamp @state)
        checkpoints (into []
                          (filter #(> (:timestamp %) last-stamp))
                          (:checkpoints job-env))
        batches (->> checkpoints
                     (map #(batch job-env %))
                     (filter seq))]
    (swap! state assoc :last-checkpoint-timestamp
           (-> job-env :checkpoints last :timestamp))
    (swap! state update :queue into batches)))

(defn coordinate [units targetk r1]  
  (let [n (max 2 (count units)) ; guarantee two-layer size
        nlayers (js/Math.ceil (inc (/ (dec n) 6)))
        r2 (/ r1 (dec (* 2 nlayers)))
        coords (stitch/pack-circle r1 r2 {:x 0 :y 0})]
    (map (fn [unit {:keys [x y]}]
           (assoc unit targetk {:r r2 :dx x :dy y}))
         units
         coords)))

(defn apply-coordinate [targetk graph-scene units]
  (->> units
       (group-by targetk)
       (mapcat (fn [[task g]]
                 (if-let [target-radius (get-in graph-scene [:tasks task :r])]
                   (coordinate g targetk target-radius)
                   g)))))

(defn recoordinate! [{:keys [state] :as segviz}]
  (let [{:keys [graph-scene rendering]} @state]
    (swap! state assoc :coordinates
           (->> rendering
                (apply-coordinate :source graph-scene)
                (apply-coordinate :dest graph-scene)
                (map (fn [unit]
                       [(:id (meta unit))
                        (select-keys unit [:source :dest])]))
                (into {})))))

(defn dot-val [targetk
               {:keys [rendering graph-scene coordinates]}
               id
               unit]
  (let [{:keys [dx dy r]} (get-in coordinates [id targetk])
        {:keys [x y]} (get-in graph-scene [:tasks (get unit targetk)])]
    {:x (+ x dx)
     :y (+ y dy)
     :r r
     :red 255
     :green 100
     :blue 100
     :alpha 1}))

(defn dot-init-val [state id batch-unit]
  (assoc (dot-val :source @state id batch-unit)
         :alpha 1))

(defn animate! [val label props easingf duration donef]
  (let [now (js/performance.now)
        start (or (-> val meta :anim-start label)
                  now)
        elapsed (- now start)
        end (+ start duration)]
    (if-not (pos? (- duration elapsed))
      (do (donef) val)
      (let [p (easingf (tween/range-to-p start end (+ start elapsed)))]
        (reduce-kv (fn [val' k [startv endv]]
                     (assoc val' k (+ startv (* p (- endv startv)))))
                   (vary-meta val assoc-in [:anim-start label] start)
                   props)))))

(def ease-out-elastic
  (partial tween/ease-out tween/transition-elastic))

(def ease-out-expo
  (partial tween/ease-out tween/transition-expo))

(defn dot-update-fn [state id batch-unit]
  (fn [val elapsed]
    (let [{:keys [phases] :as st} @state
          {:keys [phase-name offset done-cb]} (get phases id)
          source-dot (dot-val :source st id batch-unit)
          dest-dot (dot-val :dest st id batch-unit)]
      (case phase-name
        :adding
        (animate! val :adding
                  {:r [0 (:r source-dot)]
                   :y [(+ (:y source-dot) (* 0.075 (:y source-dot)))
                       (:y source-dot)]}
                  ease-out-elastic
                  700
                  done-cb)
        
        :removing
        (animate! val :removing
                  {:x [(:x source-dot) (:x dest-dot)]
                   :y [(:y source-dot) (:y dest-dot)]}
                  ease-out-expo
                  1000
                  done-cb)
        
        source-dot))))

(defn dot-draw [ctx {:keys [red green blue alpha] :as val}]
  (-> ctx
      (canvas/fill-style (str "rgba(" (str/join "," [red green blue alpha]) ")"))
      (canvas/circle val)
      (canvas/fill)))

(defn phase-fn [name units cb]
  (fn [state]
    ;;(println name)
    (if-not (seq units)
      (do (swap! state assoc :phases {})
          (cb))
      (swap! state assoc :phases
             (loop [xs units
                    i 0
                    acc {}]
               (let [x (first xs)
                     id (:id (meta x))]
                 (if-let [nxs (seq (rest xs))]
                   (recur nxs (inc i) (assoc acc id {:phase-name name :offset i :done-cb identity}))
                   (assoc acc id {:phase-name name :offset i :done-cb cb}))))))))

(defn render-next-batch! [{:keys [state] :as segviz}]
  (let [{:keys [queue rendering]} @state]
    (when-let [next-batch (peek queue)]
      (swap! state update :queue pop)
      (swap! state assoc :lock-rendering? true)
      (let [adding (set/difference next-batch rendering)
            removing (remove #(keyword-identical? ::leaf (:dest %))
                             (set/difference rendering next-batch))
            after-adding-phase!
            (fn []
              ;;(console.log "after-adding" removing)
              ;; -canvas removing
              (->> removing
                   (map #(:id (meta %)))
                   (run! #(canvas/remove-entity segviz %)))
              
              (swap! state assoc
                     :lock-rendering? false
                     :phases {})
              (render-next-batch! segviz))
            after-removing-phase!
            (fn []
              ;;(js/console.log "after-removing" removing adding)
              ;; update rendering set
              (swap! state update :rendering
                     #(into (apply disj % removing) adding))
              (swap! state assoc :phases {})
              ;; recalculate coordinates
              (recoordinate! segviz)
              ;; +canvas adding
              (run! (fn [unit]
                      (let [id (:id (meta unit))]
                        (canvas/add-entity segviz id
                                           (canvas/entity (dot-init-val state id unit)
                                                          (dot-update-fn state id unit)
                                                          dot-draw))))
                    adding)
              ;; enter adding transition
              (let [removing-groups (group-by :dest removing)
                    adding-groups (group-by :source adding)
                    ;;_ (js/console.log "ag rg" adding-groups removing-groups)
                    difference-groups (reduce-kv (fn [g k rmvs]
                                                   (update g k #(drop (count rmvs) %)))
                                                 adding-groups
                                                 removing-groups)
                    ;;_ (js/console.log "dg" difference-groups)
                    adding-phase! (phase-fn :adding
                                            (mapcat second difference-groups)
                                            after-adding-phase!)]
                (adding-phase! state)))
            
            removing-phase! (phase-fn :removing removing after-removing-phase!)]
        (removing-phase! state)))))

(defn graph-scene [graph]
  (let [view-scale (.getScale graph)
        nodes (.. graph -body -nodes)
        tasks
        (->> (js/Object.keys nodes)
             (into {}
                   (map (fn [name]
                          (let [node (aget nodes name)
                                shape (.-shape node)
                                dom-pos (.canvasToDOM graph #js {:x (.-x node)
                                                                 :y (.-y node)})]
                            [(keyword name)
                             {:x (.-x dom-pos)
                              :y (.-y dom-pos)
                              :r (dec (* view-scale (.-radius shape))) ; todo handle stroke width
                              }])))))]
    {:tasks tasks
     :view-scale view-scale}))

(defn sync-graph! [segviz graph]
  ;;(js/console.log "updated-scene" (graph-scene graph))
  (swap! (:state segviz) assoc :graph-scene (graph-scene graph)))

(def default-state
  {:last-checkpoint-timestamp 0
   :rendering #{}
   :phases {}
   :coordinates {}
   :lock-rendering? false
   :queue cljs.core/PersistentQueue.EMPTY})

(defn sync-job-env! [{:keys [state] :as segviz} job-env]
  (when (not= (:job-uuid @state) (:uuid job-env))
    (canvas/clear! segviz)
    (swap! state merge default-state))
  (enqueue-batches! segviz job-env)
  (when-not (:lock-rendering? segviz)
    (render-next-batch! segviz)))

(defn create! [job-env graph]
  (let [graph-canvas (.. graph -canvas -frame -canvas)
        segviz-canvas (doto (js/document.createElement "canvas")
                        (gdom/setProperties #js {:class "segviz"
                                                 :width (.-clientWidth graph-canvas)
                                                 :height (.-clientHeight graph-canvas)})
                        (gdom/insertSiblingBefore graph-canvas))
        segviz (-> (canvas/init segviz-canvas)
                   (assoc :state (atom (assoc default-state
                                              :job-uuid (:uuid job-env)
                                              :graph-scene (graph-scene graph)))))]
    ;;(js/console.log "init-env" job-env)
    ;;(js/console.log "graph" graph)
    ;;(js/console.log "scene" (:graph-scene @(:state segviz)))
    (enqueue-batches! segviz job-env)
    (render-next-batch! segviz)
    
    segviz))
