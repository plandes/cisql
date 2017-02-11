(ns ^{:doc "Configuration package"
      :author "Paul Landes"}
    zensols.cisql.conf
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:require [zensols.cisql.version :as ver]))

(def ^:private config-data
  (atom {:gui false
         :linesep ";"
         :errorlong false
         :prompt " %1$s > "
         :end-directive "exit"
         :help-directive "help"
         :col 80}))

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
  (->> @config-data
       (map (fn [[key val]]
              (println (format "%s: %s "(name key) val))))
       doall))

(defn print-key-desc []
  (let [dkeys (keys key-desc)
        space (->> dkeys (map #(-> % name count)) (reduce max) (max 0) (+ 2))
        fmt (str "%-" space "s %s")]
    (->> key-desc
         (map (fn [[k v]]
                (println (format fmt (str (name k) ":") v))))
         doall)))

(defn format-version []
  (format "v%s " ver/version))

(defn format-intro []
  (format "Clojure Interactive SQL (cisql) %s
(C) Paul Landes 2015 - 2017"
          (format-version)))

(defn print-help []
  (println (format-intro))
  (println help-message))

