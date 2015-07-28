(ns com.zensol.cisql.db-access
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:use [clojure.pprint :only (pprint print-table)])
  (:require [clojure.java.jdbc :as jdbc])
  (:import (java.io BufferedReader InputStreamReader StringReader))
  (:import (com.zensol.rsgui ResultSetFrame))
  (:import (java.sql SQLException))
  (:require [com.zensol.cisql.conf :as conf]))

(def products ["mysql" "postgresql" "sqlite"])

(def ^:private result-frame-data (atom nil))

(def ^:private db-info-data (atom nil))

(defn- db-info
  "Get the information about the DB.
  Assume the same spec is passed every time."
  [dbspec]
  (swap! db-info-data
         (fn [data]
           (if data
             data
             (jdbc/with-db-connection [db dbspec]
               (log/debugf "getting db metadata on %s" dbspec)
               (let [conn (jdbc/db-connection db)
                     dbmeta (.getMetaData conn)]
                 {:user (.getUserName dbmeta)
                  :url (.getURL dbmeta)}))))))

(defn- db-id-format
  "Get a string formatted for the DB.
  Assume we'll get the same DB spec every time."
  [dbspec]
  (let [{user :user url :url} (db-info dbspec)]
    (format "%s@%s"
            (second (re-find #"^(.*)@.*$" user))
            (second (re-find #"jdbc:.*?://(.*)$" url)))))

(defn- result-frame []
  (swap! result-frame-data
         #(or % (ResultSetFrame. false))))

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

(defn- print-sql-exception [sqlex]
  (if sqlex (println (format-sql-exception sqlex))))

(defn- format-header [meta]
  (letfn [(fmtlb [strs wd]
            (format (format "%%-%ds" wd) (or strs "NULL")))
          (fmtsep [wd]
            (format (format "%%-%ds" wd) "NULL"))]
    (let [cols (.getColumnCount meta)]
      (doseq [col (range 1 (+ 1 cols))]
        (let [type (.getColumnTypeName meta col)
              size (.getColumnDisplaySize meta col)]
          (log/debugf "type: %s, size: %s" type size)
          (print (fmtlb type size))
          (print (fmtsep size)))))
    (println)))

(defn- slurp-result-set [rs meta]
  (letfn [(make-row [col]
            {(.getColumnName meta col) (.getString rs col)})]
    (let [cols (.getColumnCount meta)]
      (loop [result []]
        (if (.next rs)
          (recur (conj result
                       (apply merge (map make-row (range 1 (+ 1 cols))))))
          result)))))

(defn display-result-set [rs meta dbspec]
  (try
   (if (conf/config :gui)
     (let [frame (result-frame)
           row-count (.displayResults (.getResultSetPanel frame) rs true)]
       (.pack frame)
       (if-not (.isVisible frame)
         (.setTitle frame (db-id-format dbspec)))
       (.setVisible frame true)
       row-count)
     (let [rows (slurp-result-set rs meta)]
       (print-table (map #(.getColumnName meta %)
                         (range 1 (+ 1 (.getColumnCount meta))))
                    rows)
       (count rows)))
   (finally
     (try
       (.close rs)
       (catch SQLException e
         (log/error (format-sql-exception e)))))))

(defn execute-query [query dbspec]
  (jdbc/with-db-connection [db dbspec]
    (let [conn (jdbc/db-connection db)
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
                    meta (.getMetaData rs)
                    row-count (display-result-set rs meta dbspec)]
                (when (conf/config :gui)
                  (pr-row-count start row-count)))
              (pr-row-count start (.getUpdateCount stmt))))
          (catch SQLException e
            (log/error (format-sql-exception e))
            (.cancel stmt))
          (finally
            (.close stmt)))))))

(defn show-table-metadata
  ([dbspec]
   (show-table-metadata nil dbspec))
  ([table dbspec]
   (jdbc/with-db-connection [db dbspec]
     (let [conn (jdbc/db-connection db)
           dbmeta (.getMetaData conn)
           rs (if table
                (.getColumns dbmeta nil nil table "%")
                (.getTables dbmeta nil nil "%" nil))
           meta (.getMetaData rs)]
       (display-result-set rs meta dbspec)))))
