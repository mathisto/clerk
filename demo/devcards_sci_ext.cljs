(ns devcards-sci-ext
  (:require [nextjournal.clerk.sci-viewer :as sv]
            [nextjournal.devcards :as dc]
            [nextjournal.devcards-ui :as dc-ui]
            [sci.core :as sci]))

(def devcards-namespace
  {'show-card dc-ui/show-card
   'show-card* dc-ui/show-card*
   'registry @dc/registry})

(sci/merge-opts @sv/!sci-ctx {:namespaces {'devcards devcards-namespace}})




