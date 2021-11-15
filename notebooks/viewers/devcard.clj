;; # 📇 Devcard Viewer (via SCI context extension)
(ns ^:nextjournal.clerk/no-cache viewers.devcard
  (:require [nextjournal.clerk :as clerk]))

(clerk/set-viewers!
 [{:pred #(and (map? %) (contains? % :nextjournal.devcards/name))
   :render-fn (fn [{card-id :nextjournal.devcards/name}]
                (let [ns (namespace card-id)
                      card-name (name card-id)
                      card (get-in devcards/registry [ns card-name])]
                  (js/console.log :rendering-card card-id :Card card)
                  (v/html
                   [devcards/show-card (assoc card
                                              :nextjournal.devcards/title? false)])))}])

{:nextjournal.devcards/name 'nextjournal.clerk.sci-viewer/viewer-latex}

(comment
  (clerk/serve! {:watch-paths ["notebooks"]})
  (clerk/show! "notebooks/viewers/devcard.clj"))
