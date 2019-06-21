(ns ^{:doc "Configuration package"
      :author "Paul Landes"}
    zensols.cisql.conf
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [zensols.cisql.version :as ver]
            [zensols.cisql.pref :as pref]))

(def ^:private default-config
  {:gui false
   :guinotrey true
   :linesep ";"
   :loglevel "info"
   :errorlong false
   :prompt " %1$s > "
   :end-directive "exit"
   :help-directive "help"
   :sigintercept true
   :prex false
   :col 80})

(def ^:private system-properties
  {:guinotrey "apple.awt.UIElement"})

(def ^:private set-config-hooks (atom #{}))

(def ^:private config-data-inst (atom nil))

(def ^:private parse-keys
  #{:errorlong :gui})

(def ^:private key-desc
  {:gui "use a graphical window to display result sets"
   :guinotrey "use a separate application entry for GUI results (require restart)"
   :linesep "tell where to end a query and then send"
   :prompt "a format string for the promp"
   :loglevel "the logging verbosity (<error|warn|info|debug|trace>)"
   :errorlong "if true, provide more error information"
   :sigintercept "if true, intercept signals like Control-C during queries"
   :prex "print exception stack traces"})

(def ^:private help-message
  "type 'help' to see a list of commands")

(def ^:private our-ns *ns*)

;; (System/setProperty "java.awt.headless" "true")
;; (System/setProperty "apple.laf.useScreenMenuBar" "true")
(System/setProperty "com.apple.mrj.application.apple.menu.about.name" "ImageRotator")

(defn- update-system-properties [key value]
  (let [k (get system-properties key)
        k (if k (name k))
        value (str value)]
    (when k
      (log/debugf "settings prop: %s -> %s" k value)
      (System/setProperty k value))))

(defn add-set-config-hook [callback-fn]
  (swap! set-config-hooks #(conj % callback-fn)))

(add-set-config-hook update-system-properties)

(defn set-config [key value]
  (log/tracef "%s -> %s" key value)
  (if-not (contains? default-config key)
    (-> (format "no such variable: %s" (name key))
        (ex-info {:key key :value value})
        throw))
  (let [val (if (and (contains? parse-keys key)
                     (string? value))
              (read-string value)
              value)]
    (swap! config-data-inst assoc key val)
    (doseq [callback @set-config-hooks]
      ((eval callback) key value))
    (pref/set-environment @config-data-inst)
    (log/tracef "vars: %s" @config-data-inst)))

(defn- config-data []
  (swap! config-data-inst
         (fn [prefs]
           (if (nil? prefs)
             (let [prefs (pref/environment default-config)]
               (doseq [[k v] prefs]
                 (doseq [callback @set-config-hooks]
                   ((eval callback) k v)))
               prefs)
             prefs))))

(defn config [key]
  (get (config-data) key))

(defn print-key-values []
  (->> (config-data)
       (map (fn [[key val]]
              (println (format "%s: %s "(name key) val))))
       doall))

(defn print-key-desc []
  (let [dkeys (keys key-desc)
        space (->> dkeys (map #(-> % name count)) (reduce max) (max 0) (+ 2))
        fmt (str "%-" space "s %s")]
    (->> key-desc
         (map (fn [[k v]]
                (println (format fmt (str (name k)) v))))
         doall)))

(defn reset []
  (pref/clear :var 'environment)
  (reset! config-data-inst nil)
  (config-data))

(defn format-version []
  (format "v%s " ver/version))

(defn format-intro []
  (format "Clojure Interactive SQL (cisql) %s
(C) Paul Landes 2015 - 2019"
          (format-version)))

(defn print-help []
  (println (format-intro))
  (println help-message))
