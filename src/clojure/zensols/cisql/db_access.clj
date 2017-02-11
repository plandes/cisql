(ns zensols.cisql.db-access
  (:import [sun.misc Signal SignalHandler])
  (:import [java.io BufferedReader InputStreamReader StringReader]
           [java.sql SQLException])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [clojure.pprint :refer (pprint print-table)])
  (:import (com.zensols.gui.tabres ResultSetFrame))
  (:require [zensols.tabres.display-results :as dis])
  (:require [zensols.cisql.conf :as conf]))

(def ^:private dbspec (atom nil))

(def ^:private current-catalog (atom nil))

(def ^:private db-info-data (atom nil))

(def ^:private last-frame-label (atom nil))

(defn- resolve-connection [db]
  (let [conn (jdbc/db-connection db)
        catalog @current-catalog]
    (if catalog (.setCatalog conn catalog))
    conn))

(defn set-catalog [catalog]
  (reset! current-catalog catalog)
  (reset! dbspec nil)
  (log/infof "set catalog: %s" catalog))

;; db metadata
(defn- db-info
  "Get the information about the DB."
  []
  (swap! db-info-data
         (fn [data]
           (if data
             data
             (jdbc/with-db-connection [db @dbspec]
               (log/debugf "getting db metadata on %s" @dbspec)
               (let [conn (resolve-connection db)
                     dbmeta (.getMetaData conn)]
                 {:user (.getUserName dbmeta)
                  :url (.getURL dbmeta)}))))))

(defn db-id-format
  "Get a string formatted for the DB."
  [& {:keys [user? only-url?]
      :or {user? true}}]
  (let [{:keys [user url]} (db-info)]
    (cond only-url? url
          true
          (format "%s%s"
                  (if (and user? user)
                    (str (second (re-find #"^(.*)@.*$" user)) "@")
                    "")
                  (or (second (re-find #"jdbc:.*?://?(.*?)$" url)) url)))))

(defn connected? []
  (not (nil? @dbspec)))

(defn set-db-spec [spec]
  (reset! dbspec spec))

;; gui
(defn orphan-frame
  ([]
   (orphan-frame nil))
  ([label]
   (binding [dis/frame-config-fn
             (fn [frame title orphaned?]
               (.setTitle frame
                          (format "%s: %s"
                                  (db-id-format :user? false)
                                  (or title @last-frame-label))))]
     (dis/orphan-frame label))))

(defn- format-sql-exception [sqlex]
  (when sqlex
    (if-not (conf/config :errorlong)
      (format "Error: %s" (.getMessage sqlex))
      (format "Error: %s
  state: %s
  code: %s
  class: %s"
              (.getMessage sqlex)
              (.getSQLState sqlex)
              (.getErrorCode sqlex)
              (.getName (.getClass sqlex))))))

;; error
(defn- print-sql-exception [sqlex]
  (if sqlex (println (format-sql-exception sqlex))))

(defn- slurp-result-set [rs meta]
  (letfn [(make-row [col]
            {(.getColumnLabel meta col) (.getString rs col)})]
    (let [cols (.getColumnCount meta)]
      (loop [result []]
        (if (.next rs)
          (recur (conj result
                       (apply merge (map make-row (range 1 (+ 1 cols))))))
          result)))))

(defn result-set-to-array [rs]
  (let [meta (.getMetaData rs)
        col-count (.getColumnCount meta)
        ;; must for SQLite: get headers before the result set is used up
        header (doall (map #(.getColumnLabel meta %)
                           (range 1 (+ 1 col-count))))
        rows (slurp-result-set rs meta)]
    (log/debugf "col-count %d" col-count)
    {:header header
     :rows (if (empty? rows) [{}] rows)}))

(defn- display-result-set [rs meta]
  (try
    (if (conf/config :gui)
      (binding [dis/frame-factory-fn
                (fn []
                  (let [frame (ResultSetFrame. false)]
                    (.init frame)
                    frame))]
        (dis/display-results
         (fn [frame]
           (.displayResults (.getResultSetPanel frame)
                            rs true))
                             :title (db-id-format)))
      (let [rs-data (result-set-to-array rs)]
        (print-table (:header rs-data) (:rows rs-data))
        (count (:rows rs-data))))
    (finally
      (try
        (.close rs)
        (catch SQLException e
          (log/error (format-sql-exception e)))))))

;; (def ^:private executing-thread (atom nil))

;; (defn- interrupt [signal]
;;   (log/infof "interruptwing with signal: %s" signal)
;;   (.interrupt @executing-thread))

;; (->> (proxy [SignalHandler] []
;;        (handle [s]
;;          (interrupt s)))
;;      (Signal/handle (Signal. "INT")))

(defn execute-query
  ([query]
   (execute-query query nil))
  ([query query-handler-fn]
   (reset! executing-thread (Thread/currentThread))
   (jdbc/with-db-connection [db @dbspec]
     (let [conn (resolve-connection db)
           stmt (.createStatement conn)]
       (letfn [(pr-row-count [start rows]
                 (let [end (System/currentTimeMillis)
                       wait-time (double (/ (- end start) 1000))]
                   (print-sql-exception (.getWarnings stmt))
                   (.clearWarnings stmt)
                   (if (< 0 rows)
                     (println (format "%d row(s) affected (%ss)"
                                      rows wait-time)))))]
         (print-sql-exception (.getWarnings conn))
         (.clearWarnings conn)
         (try
           (log/infof "executing: %s" query)
           (let [start (System/currentTimeMillis)]
             (if (.execute stmt query)
               (let [rs (.getResultSet stmt)
                     meta (.getMetaData rs)]
                 (if query-handler-fn
                   (pr-row-count start (query-handler-fn rs))
                   (let [row-count (display-result-set rs meta)]
                     (when (conf/config :gui)
                       (pr-row-count start row-count)))))
               (pr-row-count start (.getUpdateCount stmt))))
           (reset! last-frame-label query)
           (catch SQLException e
             (log/error (format-sql-exception e))
             (.cancel stmt))
           (finally
             (.close stmt))))))))

(defn show-table-metadata
  ([]
   (show-table-metadata nil))
  ([table]
   (jdbc/with-db-connection [db @dbspec]
     (let [conn (resolve-connection db)
           dbmeta (.getMetaData conn)
           rs (if table
                (.getColumns dbmeta nil nil table "%")
                (.getTables dbmeta nil nil "%" nil))
           meta (.getMetaData rs)]
       (display-result-set rs meta)
       (reset! last-frame-label (format "table: %s" table))))))
