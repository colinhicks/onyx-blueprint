(ns onyx-blueprint.ui.graph.segviz
  (:require [goog.dom :as gdom]
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
    [id
     {:type type
      :source source
      :dest dest
      :segments segments}]))

(defn batch [job-env {:keys [tasks timestamp] :as checkpoint}]
  (into {}
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
        checkpoints (into [] (filter #(> (:timestamp %) last-stamp)) (:checkpoints job-env))
        batches (keep #(seq (batch job-env %)) checkpoints)]
    (js/console.log "enqueue batches" batches)
    (swap! state assoc :last-checkpoint-timestamp (-> job-env :checkpoints last :timestamp))
    (swap! state update :queue into batches)))

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
                              :bounding-box (js-obj->map (.-boundingBox shape))
                              :label-size (js-obj->map (.. shape -labelModule -size))}])))))]
    {:tasks tasks
     :view-scale view-scale
     :view-position (js-obj->map (.getViewPosition graph))
     :target-translation (js-obj->map (.. graph -view -targetTranslation))}))

(defn coordinate [units targetk r1]  
  (let [n (count units)
        nlayers (js/Math.ceil (inc (/ (dec n) 6)))
        r2 (min (/ r1 2)
                (/ r1 (dec (* 2 nlayers))))
        coords (stitch/pack-circle r1 r2 {:x 0 :y 0})]
    (map (fn [[k unit] {:keys [x y]}]
           [k (assoc-in unit [:coords targetk] {:r r2
                                                :dx x
                                                :dy y})])
         units
         coords)))

(defn recoordinate! [{:keys [state] :as segviz}]
  (let [{:keys [graph-scene]} @state]
    (swap! state update :rendering
           #(->> %
                 (group-by (comp :source second))
                 (into {}
                       (mapcat (fn [[task units]]
                                 (coordinate units :source
                                             (get-in graph-scene [:tasks task :r])))))))))

(defn dot-val [{:keys [rendering graph-scene]} id]
  (let [{:keys [source dest coords]} (id rendering)
        {:keys [dx dy r]} (:source coords)
        {:keys [x y]} (get-in graph-scene [:tasks source])]
    {:x (+ x dx) :y (+ y dy) :r r}))

(defn dot-init-val [state id]
  (dot-val @state id))

(defn dot-update-fn [state id done-cb]
  (fn [val elapsed]
    (dot-val @state id)))

(defn dot-draw [ctx val]
  (-> ctx
      (canvas/fill-style "rgba(100,150,255,0.5)")
      (canvas/circle val)
      (canvas/fill)))

;; todo transition between phases

(defn render-next-batch! [{:keys [state] :as segviz}]
  (when-let [next-batch (-> @state :queue peek)]
    (swap! state update :queue pop)

    #_(swap! state update :rendering merge next-batch)
    ;; temporary
    (canvas/clear! segviz)
    (swap! state assoc :rendering next-batch)

    
    (loop [xs next-batch
           i 0
           done-cb (fn next! [] (render-next-batch! segviz))]
      (when-let [[id _] (first xs)]
        (canvas/add-entity segviz id
                           (canvas/entity (dot-init-val state id)
                                          (dot-update-fn state id done-cb)
                                          dot-draw))
        (recur (rest xs)
               (inc i)
               identity)))
    (recoordinate! segviz)))

(defn sync-graph! [segviz graph]
  (js/console.log "updated-scene" (graph-scene graph))
  (swap! (:state segviz) assoc :graph-scene (graph-scene graph)))

(defn sync-job-env! [segviz job-env]
  (enqueue-batches! segviz job-env)
  (render-next-batch! segviz))

(defn create! [job-env graph]
  (let [ graph-canvas (.. graph -canvas -frame -canvas)
        segviz-canvas (doto (js/document.createElement "canvas")
                        (gdom/setProperties #js {:class "segviz"
                                                 :width (.-clientWidth graph-canvas)
                                                 :height (.-clientHeight graph-canvas)})
                        (gdom/insertSiblingBefore graph-canvas))
        segviz (-> (canvas/init segviz-canvas)
                   (assoc :state (atom {:last-checkpoint-timestamp 0
                                        :rendering {}
                                        :queue cljs.core/PersistentQueue.EMPTY
                                        :graph-scene (graph-scene graph)})))]
    (js/console.log "init-env" job-env)
    (js/console.log "graph" graph)
    (js/console.log "scene" (:graph-scene @(:state segviz)))
    (enqueue-batches! segviz job-env)
    (render-next-batch! segviz)
    
    segviz))
