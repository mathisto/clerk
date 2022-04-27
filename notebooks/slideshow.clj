;; # 🎠 Clerk Slideshow
;; ---
^{:nextjournal.clerk/visibility :hide-ns}
(ns slideshow
  (:require [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as v]))

;; With two custom viewers and a helper function, we can show a Clerk notebooks as Slideshow.
;; First, there's the `side-viewer`:
^{::clerk/viewer :hide-result}
(def slide-viewer
  {:fetch-fn v/fetch-all
   :transform-fn (fn [fragment]
                   (v/html [:div.flex.flex-col.justify-center
                            {:style {:min-block-size "100vh"}}
                            (into [:div.text-xl.p-20 {:class ["prose max-w-none prose-h1:mb-0 prose-h2:mb-8 rose-h3:mb-8 prose-h4:mb-8"
                                                              "prose-h1:text-6xl prose-h2:text-5xl prose-h3:text-3xl prose-h4:text-2xl"]}]
                                  (map (fn [block] (if (:type block) (v/with-md-viewer block) (v/with-viewer :clerk/result block))))
                                  fragment)]))})

;; ---
;; The `doc->slides` helper function takes a Clerk notebook and partitions its blocks into slides by occurrences of markdown rulers.
^{::clerk/viewer :hide-result}
(defn doc->slides [{:as doc :keys [blocks]}]
  (let [->slide (partial v/with-viewer slide-viewer)]
    (transduce (mapcat (partial v/with-block-viewer doc))
               (fn
                 ([] {:slides [] :open-fragment []})        ;; init
                 ([{:keys [slides open-fragment]}]          ;; complete
                  (conj slides (->slide open-fragment)))
                 ([acc {:nextjournal/keys [viewer value]}]
                  (if (not= :clerk/markdown-block viewer)
                    (update acc :open-fragment conj value)
                    (loop [[first-fragment & tail] (partition-by (comp #{:ruler} :type) (-> value :doc :content))
                           {:as acc :keys [open-fragment]} acc]
                      (cond
                        (= :ruler (-> first-fragment first :type))
                        (recur tail (cond-> acc
                                      (seq open-fragment)
                                      (-> (update :slides conj (->slide open-fragment))
                                          (assoc :open-fragment []))))
                        (empty? tail)
                        (update acc :open-fragment into first-fragment)
                        'else
                        (recur tail (-> acc
                                        (update :slides conj (->slide (into open-fragment first-fragment)))
                                        (assoc :open-fragment []))))))))
               blocks)))

;; ---
;; Lastly, the `slideshow-viewer` overrides
^{::clerk/viewer :hide-result}
(def slideshow-viewer
  {:name :clerk/notebook
   :transform-fn doc->slides
   :fetch-fn v/fetch-all
   :render-fn '(fn [slides]
                 (v/html
                  (reagent/with-let [!state (reagent/atom {:current-slide 0
                                                           :grid? false
                                                           :viewport-width js/innerWidth
                                                           :viewport-height js/innerHeight})
                                     ref-fn (fn [el]
                                              (when el
                                                (swap! !state assoc :stage-el el)
                                                (js/addEventListener "resize"
                                                                     #(swap! !state assoc
                                                                             :viewport-width js/innerWidth
                                                                             :viewport-height js/innerHeight))
                                                (js/document.addEventListener "keydown"
                                                                              (fn [e]
                                                                                (case (.-key e)
                                                                                  "Escape" (swap! !state update :grid? not)
                                                                                  "ArrowRight" (when-not (:grid? !state)
                                                                                                 (swap! !state update :current-slide #(min (dec (count slides)) (inc %))))
                                                                                  "ArrowLeft" (when-not (:grid? !state)
                                                                                                (swap! !state update :current-slide #(max 0 (dec %))))
                                                                                  nil)))))
                                     default-transition {:type :spring :duration 0.4 :bounce 0.1}]
                                    (let [{:keys [grid? current-slide viewport-width viewport-height]} @!state]
                                      [:div.overflow-hidden.relative.bg-slate-50
                                       {:ref ref-fn :id "stage" :style {:width viewport-width :height viewport-height}}
                                       (into [:> (.. framer-motion -motion -div)
                                              {:style {:width (if grid? viewport-width (* (count slides) viewport-width))}
                                               :initial false
                                               :animate {:x (if grid? 0 (* -1 current-slide viewport-width))}
                                               :transition default-transition}]
                                             (map-indexed
                                              (fn [i slide]
                                                (let [width 250
                                                      height 150
                                                      gap 40
                                                      slides-per-row (int (/ viewport-width (+ gap width)))
                                                      col (mod i slides-per-row)
                                                      row (int (/ i slides-per-row))]
                                                  [:> (.. framer-motion -motion -div)
                                                   {:initial false
                                                    :class ["absolute left-0 top-0 overflow-x-hidden bg-white"
                                                            (when grid?
                                                              "rounded-lg shadow-lg overflow-y-hidden cursor-pointer ring-1 ring-slate-200 hover:ring hover:ring-blue-500/50 active:ring-blue-500")]
                                                    :animate {:width (if grid? width viewport-width)
                                                              :height (if grid? height viewport-height)
                                                              :x (if grid? (+ gap (* (+ gap width) col)) (* i viewport-width))
                                                              :y (if grid? (+ gap (* (+ gap height) row)) 0)}
                                                    :transition default-transition
                                                    :on-click #(when grid? (swap! !state assoc :current-slide i :grid? false))}
                                                   [:> (.. framer-motion -motion -div)
                                                    {:style {:width viewport-width
                                                             :height viewport-height
                                                             :transformOrigin "left top"}
                                                     :initial false
                                                     :animate {:scale (if grid? (/ width viewport-width) 1)}
                                                     :transition default-transition}
                                                    slide]]))
                                              slides))]))))})

^{::clerk/viewer :hide-result}
(clerk/set-viewers! [slide-viewer slideshow-viewer])

;; ---

;; ## 📊 Plotly
^{::clerk/visibility :hide}
(clerk/plotly {:data [{:z [[1 2 3] [3 2 1]] :type "surface"}]})

;; ---

;; ## 📈 Vega Lite
^{::clerk/visibility :hide}
(clerk/vl {:width 650 :height 400 :data {:url "https://vega.github.io/vega-datasets/data/us-10m.json"
                                         :format {:type "topojson" :feature "counties"}}
           :transform [{:lookup "id" :from {:data {:url "https://vega.github.io/vega-datasets/data/unemployment.tsv"}
                                            :key "id" :fields ["rate"]}}]
           :projection {:type "albersUsa"} :mark "geoshape" :encoding {:color {:field "rate" :type "quantitative"}}})

;; ---

;; # 👋🏻 Fin.
