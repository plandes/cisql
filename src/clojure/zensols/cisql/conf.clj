(ns ^{:doc "Manages application level configuration using the Java Prefernces
system."
      :author "Paul Landes"}
    zensols.cisql.conf
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [clojure.set :refer (difference)]
            [zensols.cisql.version :as ver]
            [zensols.cisql.pref :as pref]))

(def ^:private var-meta
  [[:gui false "use a graphical window to display result sets"]
   [:guinotrey true "use a separate application entry for GUI results (require restart)"]
   [:linesep ";" "tell where to end a query and then send"]
   [:loglevel "info" "the logging verbosity (<error|warn|info|debug|trace>)"]
   [:errorlong false "if true, provide more SQL level error information"]
   [:prex false "print exception stack traces"]
   [:prompt " %1$s > " "a format string for the promp"]
   [:sigintercept true "if true, intercept signals like Control-C during queries"]
   [:strict true "if true, do not allow setting of free variables"]
   [:end-directive "exit" "string used to exit the program"]
   [:help-directive "help" "string used to print help"]])

(def ^:private default-config
  (->> var-meta
       (map #(array-map (first %) (second %)))
       (apply merge)))

(def ^:private system-properties
  {:guinotrey "apple.awt.UIElement"})

(def ^:private set-config-hooks (atom #{}))

(def ^:private config-data-inst (atom nil))

(def ^:private parse-keys
  #{:errorlong :gui})

(def ^:private help-message
  "type 'help' to see a list of commands")

(def ^:private our-ns *ns*)

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

(defn config
  ([]
   (config nil))
  ([key & {:keys [expect?]
           :or {expect? nil}}]
   (if (nil? key)
     (config-data)
     (let [val (get (config-data) key)]
       (if (and (nil? val) expect?)
         (-> (format "no such variable: %s" (name key))
             (ex-info {:key key})
             throw))
       val))))

(defn set-config [key value]
  (log/tracef "%s -> %s" key value)
  (config)
  (if (and @config-data-inst (config :strict)
           (not (contains? default-config key)))
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

(defn remove-config [key]
  (if (contains? default-config key)
    (-> (format "can not delete built in variable: %s" key)
        (ex-info {:key key})
        throw))
  (if (not (contains? (config) key))
    (-> (format "no such user variable: %s" key)
        (ex-info {:key key})
        throw))
  (swap! config-data-inst dissoc key)
  (pref/set-environment @config-data-inst))

(defn- user-variable-names []
  (->> (keys default-config)
       set
       (difference (set (keys (config))))))

(defn print-key-values []
  (let [conf (config)]
    (letfn [(pr-conf [key]
              (println (format "  %s: %s "(name key) (get conf key))))]
      (println "built in variables:")
      (->> (map first var-meta)
           (map pr-conf)
           doall)
      (println "user variables:")
      (->> (user-variable-names)
           (map pr-conf)
           doall))))

(defn print-key-desc []
  (let [space (->> (map first var-meta)
                   (map #(-> % name count))
                   (reduce max)
                   (max 0)
                   (+ 2))
        fmt (str "%-" space "s %s")]
    (->> var-meta
         (map (fn [[k d v]]
                (println (format fmt (str (name k)) v))))
         doall)))

(defn reset []
  (pref/clear :var 'environment)
  (reset! config-data-inst nil)
  (config))

(defn format-version []
  (format "v%s " ver/version))

(defn format-intro []
  (format "Clojure Interactive SQL (cisql) %s
(C) Paul Landes 2015 - 2019"
          (format-version)))

(defn print-help []
  (println (format-intro))
  (println help-message))
