(ns ^{:doc "Preference support using the Java Preferences system."
      :author "Paul Landes"}
  zensols.cisql.pref
  (:import (com.zensols.gui.pref PrefSupport))
  (:require [clojure.tools.logging :as log]))

(definterface PrefNode (_ []))

(def ^:private pref-inst (atom nil))

(def ^:private pref-ns *ns*)

(def driver-config-prop "driver")
(def environment-config-prop "env")

(defn pref-support []
  (swap! pref-inst
         #(or % (PrefSupport. PrefNode))))

(defn- prefs []
  (.getPreferences (pref-support)))

(defn- set-var [name data]
  (log/debugf "set %s: <%s>" name (pr-str data))
  (doto (prefs)
    (.put name (str "(quote " (pr-str data) ")"))
    (.sync)))

(defn- get-var [name & {:keys [default]
                       :or {default {}}}]
  (-> (prefs)
      (.get name (pr-str default))
      read-string
      eval))

(defn driver-metas []
  (get-var driver-config-prop))

(defn set-driver-metas [metas]
  (set-var driver-config-prop metas))

(defn environment [default-value]
  (get-var environment-config-prop :default default-value))

(defn set-environment [env]
  (set-var environment-config-prop env))

(defn clear [& {:keys [var]
                :or {var :all}}]
  (if (= var :all)
    (.clear (prefs))
    (let [name (->> (str (name var) "-config-prop")
                    (symbol (name (ns-name pref-ns)))
                    eval)]
      (if (nil? name)
        (throw (ex-info (format "No defined variable: %s" var) {:name name})))
      (doto (prefs)
        (.remove name)))))
