(ns ^{:doc "Preference support"
      :author "Paul Landes"}
  zensols.cisql.pref
  (:import (com.zensols.gui.pref PrefSupport))
  (:require [clojure.tools.logging :as log]))

(definterface PrefNode (_ []))

(def ^:private pref-inst (atom nil))

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
  (log/debugf "set driver metas: <%s>" (pr-str metas))
  (doto (-> (pref-support)
            .getPreferences)
    (.put driver-config-prop (str "(quote " (pr-str metas) ")"))
    (.sync)))

(defn clear []
  (-> (pref-support)
      .getPreferences
      .clear))
