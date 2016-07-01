(ns com.zensols.cisql.process-query
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:import (java.io BufferedReader InputStreamReader StringReader))
  (:require [com.zensols.cisql.conf :as conf]
            [com.zensols.cisql.db-access :as db]
            [com.zensols.cisql.table-export :as te]
            [com.zensols.cisql.log4j-util :as lu]))

(def ^:dynamic *std-in* nil)

(def ^{:private true
       :dynamic true}
  *query* nil)

(def ^:private last-query (atom nil))

(def ^:private input-rules
  '([has-end-tok :linesep false :end-query]
    [has-end-tok :end-directive true :end-session]
    [has-end-tok :help-directive true :help]
    [config-setting]
    [show-setting]
    [toggle-setting]
    [show-tables]
    [orphan-frame]
    [config-catalog]
    [export-csv]
    (fn [line]
      (with-query (println line))
      {:dir :continue})))

(defmacro with-query [& body]
  `(.append *query* (with-out-str ~@body)))

(defn- parse-keyword [line key]
  (str/trim
   (subs line 0 (- (count line)
                   (count (conf/config key))))))

(defn- has-end-tok [line key entire-line? directive]
  (let [conform (str/lower-case (str/trim line))
        val (conf/config key)
        found? (if entire-line?
                 (= conform val)
                 (.endsWith conform val))]
    (when found?
      (with-query
        (print (parse-keyword line key)))
      {:dir directive})))

(defn- config-setting [line]
  (let [[_ key val] (re-find #"^\s*cf\s+([^\s]+)\s(.+?)$" line)]
    (if key
      {:dir :setting
       :settings [(keyword key) val]})))

(defn- show-setting [line]
  (let [[_ key] (re-find #"^\s*sh(?:\s+([^ ]+))?\s*$" line)]
    (if _
     {:dir :show
      :key (if key
             (keyword key)
             'show)})))

(defn- toggle-setting [line]
  (let [[_ key] (re-find #"^\s*tg\s+([^\s]+)\s*$" line)]
    (if key
     {:dir :toggle
      :key (keyword key)})))

(defn- show-tables [line]
  (let [[_ table] (re-find #"^\s*shtab(?:\s+([^\s]+))?\s*$" line)]
    (if _
     {:dir :show-table
      :key (if table
             table
             (if _ 'show-all))})))

(defn- orphan-frame [line]
  (let [[_ table] (re-find #"^\s*orph(?:\s+([^\s]+))?\s*$" line)]
    (if _
      {:dir :orphan-frame
       :key (if table
              table
              (if _ 'default-label))})))

(defn- config-catalog [line]
  (let [[_ catalog] (re-find #"^\s*cfcat\s+([^\s]+)\s*$" line)]
    (if _
     {:dir :config-catalog
      :catalog catalog})))

(defn- export-csv [line]
  (let [[_ filename] (re-find #"^\s*export\s+([^\s]+)\s*$" line)]
    (if _
     {:dir :export-csv
      :filename filename})))

(defn- add-line [line]
  ;; we have to set the namespace so resolve works in the REPL (namespace
  ;; is `user' otherwise)
  (binding [*ns* (find-ns 'com.zensols.cisql.process-query)]
    (some (fn [entry]
            (if (and (seq? entry)
                     (= 'fn (first entry)))
              (eval (list entry line))
              (let [directive-fn (resolve (first entry))
                    params (cons line (rest entry))]
                (apply directive-fn params))))
          input-rules)))

(defn- maybe-set-log-level []
  (let [level (conf/config :loglev)]
    (when level
      (try
        (lu/change-log-level level)
        (catch Exception e
          (log/errorf "Can't set log level: %s" (.getMessage e)))))))

(defn- read-query [prompt-fn set-var-fn show-var-fn toggle-var-fn]
  (binding [*query* (StringBuilder.)]
    (let [lines-left (atom 60)
          directive (atom nil)]
      (letfn [(end [dir]
                (log/debugf "query: %s" *query*)
                (reset! lines-left 0)
                (reset! directive dir))]
        (while (> @lines-left 0)
          (log/tracef "lines left: %d" @lines-left)
          (prompt-fn false)
          (let [user-input (.readLine *std-in*)]
            (log/debugf "line: %s" user-input)
            (if-not user-input
              (do
                (log/debugf "read EOF from input")
                (end :end-file))
              (let [{dir :dir :as ui} (add-line user-input)]
                (log/tracef "dir: %s" dir)
                (log/tracef "query so far: %s" *query*)
                (if-not (= :continue dir)
                  (prompt-fn true))
                (case dir
                  :help (conf/print-help true)
                  :end-query (end dir)
                  :end-session (end dir)
                  :setting (set-var-fn (:settings ui))
                  :show (show-var-fn (:key ui))
                  :show-table (let [table (if (string? (:key ui)) (:key ui))]
                                (db/show-table-metadata table))
                  :orphan-frame (let [label (if (string? (:key ui)) (:key ui))]
                                   (db/orphan-frame label))
                  :config-catalog (db/set-catalog (:catalog ui))
                  :export-csv (te/export-table-csv @last-query (:filename ui))
                  :toggle (toggle-var-fn (:key ui))
                  :continue (log/tracef "continue...")
                  :default (throw (IllegalStateException.
                                   (str "unknown directive: " dir)))))))
          (swap! lines-left dec)))
      (let [query-str (str/trim (.toString *query*))]
        (if (not (empty? query-str))
          (reset! last-query query-str))
        {:query query-str
         :directive @directive}))))

(defn- gen-prompt-fn []
  (let [line-no (atom 0)]
    (fn [reset]
      (if reset
        (reset! line-no 0)
        (do
          (print (format (conf/config :prompt) (swap! line-no inc)))
          (flush))))))

(defn process-queries [dir-fns]
  (let [prompt-fn (or (get dir-fns :prompt-for-input)
                      (gen-prompt-fn))]
    (letfn [(set-var [[key newval]]
              (let [oldval (conf/config key)]
                (conf/set-config key newval)
                (println (format "%s: %s -> %s" (name key) oldval newval))))
            (show [key]
              (if (= key 'show)
                (conf/print-key-values)
                (let [val (conf/config key)]
                  (println (format "%s: %s" (name key) val))
                  ;(println (format "Error: no such variable: %s" (name key)))
                  )))
            (toggle [key]
              (let [oldval (conf/config key)
                    nextval (not oldval)]
                (conf/set-config key nextval)
                (println (format "%s: %s -> %s"
                                 (name key) oldval nextval))))]
      (loop [query-data (read-query prompt-fn set-var show toggle)]
        (log/tracef "query data: %s" query-data)
        (log/debugf "query: <%s>" (:query query-data))
        (maybe-set-log-level)
        (if-let [dir-fn (get dir-fns (:directive query-data))]
          (try
            (dir-fn query-data)
            (prompt-fn true)
            (catch Exception e
              (let [handler (:exception-handler dir-fns)]
                (if handler
                  (handler e)
                  (log/error e "Error: " (.toString e))))))
          (throw (IllegalArgumentException.
                  (str "no mapping for directive: "
                       (:directive query-data)))))
        (when (= :end-query (:directive query-data))
          (recur (read-query prompt-fn set-var show toggle)))))))

(defn process-query-string [query-string dir-fns]
  (binding [*std-in* (BufferedReader. (StringReader. query-string))]
    (process-queries dir-fns)))
