^{:nextjournal.clerk/visibility :hide}
(ns ^:nextjournal.clerk/no-cache nextjournal.clerk.tap
  (:require [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as v]
            [clojure.core :as core]))
#_
^{::clerk/viewer {:transform-fn (fn [{::clerk/keys [var-from-def]}]
                                  {:var-name (symbol var-from-def) :value @@var-from-def})
                  :commands []
                  :fetch-fn (fn [_ x] x)
                  :render-fn '(fn [{:keys [var-name value]}]
                                (v/html
                                 (let [choices [:stream :latest]]
                                   [:div.flex.justify-between.items-center
                                    (into [:div.flex.items-center.font-sans.text-xs.mb-3 [:span.text-slate-500.mr-2 "View-as:"]]
                                          (map (fn [choice]
                                                 [:button.px-3.py-1.font-medium.hover:bg-indigo-50.rounded-full.hover:text-indigo-600.transition
                                                  {:class (if (= value choice) "bg-indigo-100 text-indigo-600" "text-slate-500")
                                                   :on-click #(v/clerk-eval `(reset! ~var-name ~choice))}
                                                  choice]) choices))
                                    [:button.text-xs.rounded-full.px-3.py-1.border-2.font-sans.hover:bg-slate-100.cursor-pointer {:on-click #(v/clerk-eval `(reset! !taps ()))} "Clear"]])))}}
(defonce !view (atom :stream))


^{::clerk/viewer clerk/hide-result}
(defonce !taps (atom ()))

#_
^{::clerk/viewer (if (= :latest @!view)
                   {:transform-fn first}
                   {:render-fn '#(v/html
                                  #_
                                  [:div.flex.flex-col
                                   (map-indexed (fn [idx x]
                                                  (with-meta (v/inspect (update %2 :path conj idx) x)
                                                    {:key (gensym)}))
                                                %1)]
                                  (into [:div.flex.flex-col]
                                        (map-indexed (fn [idx x]
                                                       (v/inspect (update %2 :path conj idx) x)))
                                        %1))})}
@!taps





^{::clerk/viewer {:render-fn '(fn [taps opts]
                                (js/console.log :taps taps)
                                (v/html [:div.flex.flex-col
                                         (map (fn [tap] (let [{:keys [value key]} (:nextjournal/value tap)]
                                                         (with-meta [v/inspect value] {:key key})))
                                              taps)]))
                  :transform-fn (fn [taps]
                                  (mapv (fn [tap] (clerk/with-viewer {:render-fn '(fn [{:keys [key value]}]
                                                                                   (js/console.log :value value :key key)
                                                                                   [:div "hi"])
                                                                     :fetch-fn (fn [{:as opts :keys [describe-fn]} x]
                                                                                 (prn :X x)
                                                                                 (update x :value describe-fn #_#_(assoc opts :current-path []) []))}
                                                   tap)) taps))}}
[#_{:key (str (gensym)) :value 42}
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}]



#_
^{::clerk/viewer {:render-fn '#(v/html [:div.flex.flex-col %1]
                                       #_
                                       (into [:div.flex.flex-col]
                                             (map-indexed (fn [idx x]
                                                            (js/console.log :x x)
                                                            (v/inspect (update %2 :path conj idx) x)))
                                             %1))
                  :transform-fn (fn [xs]
                                  (prn :xs xs)
                                  (mapv #(clerk/with-viewer :tap %) xs))}
  ::clerk/viewers [{:name :tap
                    :render-fn '(fn [{:keys [key value]}]
                                  (js/console.log :value value :key key)
                                  value)
                    :fetch-fn (fn [{:as opts :keys [describe-fn]} x]
                                (prn :X x)
                                (update x :value describe-fn #_#_(assoc opts :current-path []) []))}]}
[#_{:key (str (gensym)) :value 42}
 {:key (str (gensym)) :value (range 40)}
 #_#_#_#_#_#_#_#_
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}
 {:key (str (gensym)) :value (range 40)}]

#_(reset! !taps ())

#_
^{::clerk/viewer clerk/hide-result}
(defn tapped [x]
  (swap! !taps conj x #_{:val x #_#_:inst (java.time.Instant/now) :key (str (gensym))})
  (binding [*ns* (find-ns 'tap)]
    (clerk/recompute!)))

#_(tapped (rand-int 1000))

#_(reset! @(find-var 'clojure.core/tapset) #{})




^{::clerk/viewer clerk/hide-result}
(comment
  (Thread/sleep 4000)
  
  (tap> (rand-int 1000))
  (tap> (shuffle (range 10)))
  (tap> (javax.imageio.ImageIO/read (java.net.URL. "https://images.freeimages.com/images/large-previews/773/koldalen-4-1384902.jpg")))
  (tap> (clerk/vl {:width 650 :height 400 :data {:url "https://vega.github.io/vega-datasets/data/us-10m.json"
                                                 :format {:type "topojson" :feature "counties"}}
                   :transform [{:lookup "id" :from {:data {:url "https://vega.github.io/vega-datasets/data/unemployment.tsv"}
                                                    :key "id" :fields ["rate"]}}]
                   :projection {:type "albersUsa"} :mark "geoshape" :encoding {:color {:field "rate" :type "quantitative"}}}))

  )
