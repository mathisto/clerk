;; # Table (encore)

;; Leaning into the viewer api to make the table viewer better.

^{:nextjournal.clerk/visibility :hide}
(ns ^:nextjournal.clerk/no-cache table-viewer
  (:require [nextjournal.clerk.viewer :refer :all]))

^{:nextjournal.clerk/viewer :hide-result}
(defn update-table-viewers' [viewers]
  (-> viewers
      (update-viewers {(comp #{:elision} :name) #(assoc % :render-fn '(fn [_] (v/html "…")))
                       (comp #{string?} :pred) #(assoc % :render-fn (quote v/string-viewer))
                       (comp #{number?} :pred) #(assoc % :render-fn '(fn [x] (v/html [:span.tabular-nums (if (js/Number.isNaN x) "NaN" (str x))])))})
      (add-viewers [{:pred #{:nextjournal/missing} :render-fn '(fn [x] (v/html [:<>]))}
                    {:name :table-col ;; TODO: describe cells
                     :render-fn '(fn [col] (v/html [:td col]))}
                    {:name :table-row
                     :transform-fn (update-value (partial map (partial with-viewer :table-col)))
                     :render-fn '(fn [col opts] (v/html (into [:tr] (v/inspect-children opts) col)))}])))

^{:nextjournal.clerk/viewer :hide-result}
(def my-table
  (partial with-viewer {:transform-fn (fn [{:as wrapped-value :nextjournal/keys [viewers] :keys [offset path current-path]}]
                                        (if-let [{:keys [head rows]} (normalize-table-data (->value wrapped-value))]
                                          (-> wrapped-value
                                              (assoc :nextjournal/value (map (partial with-viewer :table-row) rows))
                                              (assoc :nextjournal/viewer {:render-fn '(fn [rows opts] (v/html (into [:table.text-xs.sans-serif.text-gray-900.dark:text-white.not-prose]
                                                                                                                    (v/inspect-children opts)
                                                                                                                    rows)))})
                                              (update :nextjournal/viewers update-table-viewers'))
                                          (-> wrapped-value
                                              assoc-reduced
                                              (assoc :nextjournal/value [(describe wrapped-value)])
                                              (assoc :nextjournal/viewer {:render-fn 'v/table-error}))))}))


;; ## The simplest example, no header.
(my-table [[1 2] [3 4]])


;; ## An error
(my-table #{1 2 3})

#_(describe (my-table [[1 2] [3 4]]))

