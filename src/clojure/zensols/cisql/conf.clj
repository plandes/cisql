(ns zensols.cisql.conf
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:require [cisql.version :as ver]))

(def ^:private config-data
  (atom {:gui false
         :linesep "go"
         :errorlong false
         :prompt " %1$s > "
         :end-directive "exit"
         :help-directive "help"}))

(def ^:private parse-keys
  #{:errorlong :gui})

(def ^:private key-desc
  {:gui "whether or not to use a graphical window to display result sets"
   :linesep "tell where to end a query and then send"
   :prompt "a format string for the promp"
   :errorlong "if true provide more error information"
   :loglev "log level of the program (error, warn, info (default), debug, trace)"})

(def ^:private help-message
  "type 'help' to see a list of commands")

(def ^:private help-commands
  {:cf "<variable value>  configure (set) a 'variable' to 'value' (ie 'tg gui')"
   :sh "[variable]        show 'variable', or show them all if not given"
   :tg "<variable>        toggle a boolean variable"
   :shtab "[table]        show table metdata or all if no table given"
   :orph "[label]         orphan (spawn new next) window in GUI mode"
   :cfcat "<catalog>      configure (set) the database (like 'use <db name>')"
   :export "<filename>    export the last query as a CSV file"})

(defn set-config [key value]
  (log/tracef "%s -> %s" key value)
  (let [val (if (and (contains? parse-keys key)
                     (string? value))
              (read-string value)
              value)]
    (swap! config-data assoc key val)
    (log/tracef "vars: %s" @config-data)))

(defn config [key]
  (get @config-data key))

(defn print-key-values []
  (dorun (map (fn [[key val]]
                (println (format "%s: %s "(name key) val)))
              @config-data)))

(defn print-key-desc []
  (dorun (map #(println (format "%-20s%s"
                                (str (name %) ":")
                                (get key-desc %)))
              (keys key-desc))))

(defn print-help-commands []
  (dorun (map (fn [[key val]] 
                (println (format "%s %s"
                                 (name key)
                                 val)))
              help-commands)))

(defn format-version []
  (format "v%s " ver/version))

(defn format-intro []
  (format "Clojure Interactive SQL (cisql) %s
(C) Paul Landes 2015 - 2017"
          (format-version)))

(defn print-help [long?]
  (println (format-intro))
  (if-not long? (println help-message))
  (when long?
    (println "commands:")
    (print-help-commands)
    (println)
    (println "variables:")
    (print-key-desc)))
