(ns ^{:doc "Preference support"
      :author "Paul Landes"}
  zensols.cisql.pref
  (:import (com.zensols.gui.pref PrefSupport))
  (:require [zensols.actioncli.dynamic :refer (defa-)]))

(defa- pref-inst)

(definterface PrefNode (_ []))

(def ^:private driver-config-prop "driver")

(defn pref-support []
  (swap! pref-inst
         #(or % (PrefSupport. PrefNode))))

(defn driver-metas []
  (-> (pref-support)
      .getPreferences
      (.get driver-config-prop "{}")
      read-string
      eval))

(defn set-driver-metas [metas]
  (-> (pref-support)
      .getPreferences
      (.put driver-config-prop (str "(quote " (pr-str metas) ")"))))

(defn clear []
  (-> (pref-support)
      .getPreferences
      .clear))
