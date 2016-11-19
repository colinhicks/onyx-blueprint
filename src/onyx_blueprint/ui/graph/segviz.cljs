(ns onyx-blueprint.ui.graph.segviz
  (:require [clojure.set :as set]
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
                 {:keys [onyx/batch-size onyx/type]} (-> event :onyx.core/task-map)
                 inbox-batches
                 (->> inbox
                      (repeat)
                      (mapcat (fn [dest inbox]
                                (map (fn [idx segments]
                                       (batch-unit type source dest segments idx timestamp))
                                     (range)
                                     (partition-all batch-size inbox)))
                              children))]
             (into inbox-batches
                   (map-indexed (fn [idx output]
                                  (batch-unit :output source nil [output] idx timestamp)))
                   outputs))))
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
  (let [n (count units)
        nlayers (js/Math.ceil (inc (/ (dec n) 6)))
        r2 (min (/ r1 2)
                (/ r1 (dec (* 2 nlayers))))
        coords (stitch/pack-circle r1 r2 {:x 0 :y 0})]
    (map (fn [unit {:keys [x y]}]
           (assoc unit targetk {:r r2 :dx x :dy y}))
         units
         coords)))

(defn apply-coordinate [targetk graph-scene units]
  (->> units
       (group-by targetk)
       (mapcat (fn [[task g]]
                 (coordinate g targetk
                             (get-in graph-scene [:tasks task :r]))))))

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
               {:keys [source dest]}]
  (let [{:keys [dx dy r]} (get-in coordinates [id targetk])
        {:keys [x y]} (get-in graph-scene [:tasks source])]
    {:x (+ x dx) :y (+ y dy) :r r}))

(defn dot-init-val [state id batch-unit]
  (dot-val :source @state id batch-unit))

(defn dot-update-fn [state id batch-unit done-cb]
  (fn [val elapsed]
    (let [{:keys [adding removing] :as st} @state
          source-dot (dot-val :source st id batch-unit)
          dest-dot (dot-val :dest st id batch-unit)]
      
      #_(cond
        (adding batch-unit)
        (js/console.log "adding" id)
        
        (removing batch-unit)
        (js/console.log "removing" id))

      source-dot
      )))

(defn dot-draw [ctx val]
  (-> ctx
      (canvas/fill-style "rgba(100,150,255,0.5)")
      (canvas/circle val)
      (canvas/fill)))

;; todo transition between phases

(defn render-next-batch! [{:keys [state] :as segviz}]
  (let [{:keys [queue rendering]} @state]
    (when-let [next-batch (peek queue)]
      (let [adding (set/difference next-batch rendering)
            removing (set/difference rendering next-batch)]
        (swap! state update :queue pop)
        (swap! state update :rendering into next-batch)
        (swap! state assoc
               :adding adding
               :removing removing)
        (recoordinate! segviz)
        
        (js/console.log "adding" adding)
        (js/console.log "removing" removing)
        
        ;; temporary
        ;;(canvas/clear! segviz)
        
        
        
        (loop [xs next-batch
               i 0
               done-cb (fn next! [] (render-next-batch! segviz))]
          (when-let [x (first xs)]
            (let [id (:id (meta x))]
              (canvas/add-entity segviz id
                                 (canvas/entity (dot-init-val state id x)
                                                (dot-update-fn state id x done-cb)
                                                dot-draw))
              (recur (rest xs)
                     (inc i)
                     identity))))))))

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

(defn sync-job-env! [segviz job-env]
  (enqueue-batches! segviz job-env)
  (render-next-batch! segviz))

(defn create! [job-env graph]
  (let [graph-canvas (.. graph -canvas -frame -canvas)
        segviz-canvas (doto (js/document.createElement "canvas")
                        (gdom/setProperties #js {:class "segviz"
                                                 :width (.-clientWidth graph-canvas)
                                                 :height (.-clientHeight graph-canvas)})
                        (gdom/insertSiblingBefore graph-canvas))
        segviz (-> (canvas/init segviz-canvas)
                   (assoc :state (atom {:last-checkpoint-timestamp 0
                                        :rendering #{}
                                        :adding #{}
                                        :removing #{}
                                        :coordinates {}
                                        :queue cljs.core/PersistentQueue.EMPTY
                                        :graph-scene (graph-scene graph)})))]
    ;;(js/console.log "init-env" job-env)
    ;;(js/console.log "graph" graph)
    ;;(js/console.log "scene" (:graph-scene @(:state segviz)))
    (enqueue-batches! segviz job-env)
    (render-next-batch! segviz)
    
    segviz))
