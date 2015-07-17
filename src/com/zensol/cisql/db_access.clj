(ns com.zensol.cisql.db-access
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:use [clojure.pprint :only (pprint print-table)])
  (:require [clojure.java.jdbc :as jdbc])
  (:import (java.io BufferedReader InputStreamReader StringReader))
  (:import (java.sql SQLException))
  (:require [com.zensol.cisql.conf :as c]))

(defn- format-sql-exception [sqlex]
  (when sqlex
    (if-not (c/config :error-long-format)
      (format "Error: %s" (.getMessage sqlex))
      (format "Error: %s
  state: %s
  code: %s
  class: %s"
              (.getMessage sqlex)
              (.getSQLState sqlex)
              (.getErrorCode sqlex)
              (.getName (.getClass sqlex))))))

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

(defn execute-query [query dbspec]
 (jdbc/with-db-connection [db dbspec]
   (let [conn (jdbc/db-connection db)
         stmt (.createStatement conn)]
     (format-sql-exception (.getWarnings conn))
     (.clearWarnings conn)
     (try
       (log/infof "executing: <<%s>>" query)
       (if (.execute stmt query)
         (let [rs (.getResultSet stmt)
               meta (.getMetaData rs)]
           (print-table (map #(.getColumnName meta %)
                             (range 1 (+ 1 (.getColumnCount meta))))
                        (slurp-result-set rs meta))))
       (catch SQLException e
         (log/error (format-sql-exception e))
         (.cancel stmt))
       (finally
         (.close stmt))))))
