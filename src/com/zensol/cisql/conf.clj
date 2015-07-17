(ns com.zensol.cisql.conf
  (:require [clojure.string :as str])
  (:require [cisql.version :as ver]))

(def ^:private config-data
  (atom {:line-terminator "go"
         :end-directive "exit"
         :prompt " %1$s > "
         :help "help"
         :error-long-format false}))

(def ^:private parse-keys
  #{:error-long-format})

(def ^:private key-desc
  {:line-terminator "tell where to end a query and then send"
   :end-directive "what to look for when finishing the program"
   :prompt "a format string for the promp"
   :error-long-format "if true provide more error information"})

(def ^:private help-message
  "type 'help' to see a list of commands")

(def ^:private help-commands
  {:cf "<variable value>  configure (set) a 'variable' to 'value' (ie 'tg gui)"
   :sh "[variable]        show 'variable', or show them all if not given"
   :tg "[variable]        toggle a boolean variable"})

(defn set-config [key value]
  (let [val (if (and (contains? parse-keys key)
                     (string? value))
              (read-string value)
              value)]
    (swap! config-data assoc key val)))

(defn config [key]
  (get @config-data key))

(defn- print-key-values []
  (dorun (map (fn [[key val]]
                (println (format "%s: %s "(name key) val)))
              @config-data)))

(defn- print-key-desc []
  (dorun (map #(println (format "%-20s%s"
                                (str (name %) ":")
                                (get key-desc %)))
              (keys key-desc))))

(defn- print-help-commands []
  (dorun (map (fn [[key val]] 
                (println (format "%s %s"
                                 (name key)
                                 val)))
              help-commands)))

(defn format-version []
  (format "v%s " ver/version))

(defn format-intro []
  (format "Clojure Interactive SQL (cisql) %s (C) Paul Landes 2015"
          (format-version)))

(defn print-help [long?]
  (println (format-intro))
  (if-not long? (println help-message))
  (when long?
    (print-help-commands)
    (println)
    (println "variables:")
    (print-key-desc)))
