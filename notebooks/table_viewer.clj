;; # Table (encore)

;; Leaning into the viewer api to make the table viewer better.

^{:nextjournal.clerk/visibility :hide}
(ns ^:nextjournal.clerk/no-cache table-viewer
  (:require [nextjournal.clerk.viewer :refer :all]))


#_[:table.text-xs.sans-serif.text-gray-900.dark:text-white.not-prose
   (when head
     [:thead.border-b.border-gray-300.dark:border-slate-700
      (into [:tr]
            (map-indexed (fn [i k]
                           [:th.relative.pl-6.pr-2.py-1.align-bottom.font-medium
                            {:class (if (number? (get-in rows [0 i])) "text-right" "text-left")
                             :title (if (or (string? k) (keyword? k)) (name k) (str k))}
                            [:div.flex.items-center
                             (if (or (string? k) (keyword? k)) (name k) [inspect k])
                             (when (= sort-index i)
                               [:span.inline-flex.justify-center.items-center.relative
                                {:style {:font-size 20 :width 10 :height 10 :top -2}}
                                (if (= sort-order :asc) "▴" "▾")])]]) head))])
   (into [:tbody]
         (map-indexed (fn [i row]
                        (let [row (viewer/->value row)]
                          (into
                           [:tr.hover:bg-gray-200.dark:hover:bg-slate-700
                            {:class (if (even? i) "bg-black/5 dark:bg-gray-800" "bg-white dark:bg-gray-900")}]
                           (map-indexed (fn [j d]
                                          [:td.pl-6.pr-2.py-1
                                           {:class [(when (number? d) "text-right")
                                                    (when (= j sort-index) "bg-black/5 dark:bg-gray-800")]}
                                           [inspect (update opts :path conj i j) d]]) row)))) (viewer/->value rows)))]


^{:nextjournal.clerk/viewer :hide-result}
(defn update-table-viewers' [viewers]
  (-> viewers
      (update-viewers {(comp #{:elision} :name) #(assoc % :render-fn '(fn [{:as fetch-opts :keys [total offset unbounded?]}]
                                                                        (v/html
                                                                         [v/consume-view-context :fetch-fn (fn [fetch-fn]
                                                                                                             [:tr.border-t.dark:border-slate-700
                                                                                                              [:td.text-center.py-1
                                                                                                               {:col-span #_#_num-cols FIXME 2
                                                                                                                :class (if (fn? fetch-fn)
                                                                                                                         "bg-indigo-50 hover:bg-indigo-100 dark:bg-gray-800 dark:hover:bg-slate-700 cursor-pointer"
                                                                                                                         "text-gray-400 text-slate-500")
                                                                                                                :on-click #(when (fn? fetch-fn)
                                                                                                                             (fetch-fn fetch-opts))}
                                                                                                               (- total offset) (when unbounded? "+") (if (fn? fetch-fn) " more…" " more elided")]])])))
                       (comp #{string?} :pred) #(assoc % :render-fn (quote v/string-viewer))
                       (comp #{number?} :pred) #(assoc % :render-fn '(fn [x] (v/html [:span.tabular-nums (if (js/Number.isNaN x) "NaN" (str x))])))})
      (add-viewers [{:name :table-markup :fetch-opts {:n 5} :render-fn '(fn [rows opts] (v/html [:table.text-xs.sans-serif.text-gray-900.dark:text-white.not-prose
                                                                                                 (into [:tbody] (map-indexed (fn [idx row] (v/inspect (update opts :path conj idx) row))) rows)]))}
                    {:name :tr :render-fn '(fn [row {:as opts :keys [path]}]
                                             (v/html (into [:tr.hover:bg-gray-200.dark:hover:bg-slate-700
                                                            {:class (if (even? (peek path)) "bg-black/5 dark:bg-gray-800" "bg-white dark:bg-gray-900")}]
                                                           (map (fn [cell] [:td.pl-6.pr-2.py-1 (v/inspect opts cell)])) row)))}
                    {:pred #{:nextjournal/missing} :render-fn '(fn [x] (v/html [:<>]))}])))


^{:nextjournal.clerk/viewer :hide-result}
(def my-table
  (partial with-viewer {:transform-fn (fn [{:as wrapped-value :nextjournal/keys [viewers] :keys [offset path current-path]}]
                                        (if-let [{:keys [head rows]} (normalize-table-data (->value wrapped-value))]
                                          (let [viewers (update-table-viewers' viewers)]
                                            (-> wrapped-value
                                                (assoc :nextjournal/viewers viewers)
                                                (assoc :nextjournal/value (map #(->> % (ensure-wrapped-with-viewers viewers) (with-viewer :tr)) rows))
                                                (assoc :nextjournal/viewer :table-markup)))
                                          (-> wrapped-value
                                              assoc-reduced
                                              (assoc :nextjournal/value [(describe wrapped-value)])
                                              (assoc :nextjournal/viewer {:render-fn 'v/table-error}))))}))


;; ## The simplest example, no header.
(my-table (map-indexed #(vector (inc %1) %2) (->> "/usr/share/dict/words" slurp clojure.string/split-lines (take 30))))

;; ## Table Inside a Table
(my-table [[1 2] [3 (my-table [[4 5] [6 7]])]])

;; ## Table with an Image in it
(my-table [["an image"]
           [(javax.imageio.ImageIO/read (java.net.URL. "https://etc.usf.edu/clipart/36600/36667/thermos_36667_sm.gif"))]])

#_(do (prn :===================)
      (describe (my-table [[1 2]])))

;; ## An error
#_(my-table #{1 2 3})

#_(describe (my-table [[1 2] [3 4]]))

#_(def d (comp count-viewers describe))
