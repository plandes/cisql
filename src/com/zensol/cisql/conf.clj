(ns com.zensol.cisql.conf
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

(defn set-config [key value]
  (let [val (if (and (contains? parse-keys key)
                     (string? value))
              (read-string value)
              value)]
    (swap! config-data assoc key val)))

(defn config [key]
  (get @config-data key))

(defn format-version []
  (format "v%s " ver/version))

(defn format-intro []
  (format "Clojure Interactive SQL (cisql) %s (C) Paul Landes 2015" (format-version)))

(defn print-help [long?]
  (println (format-intro))
  (println help-message)
  (when long?
    (println \newline)
    (dorun (map #(println (format "%-20s: %s" (name %) (get key-desc %)))
                (keys key-desc)))))
