(ns com.zensol.cisql.db-access
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:use [clojure.pprint :only (pprint print-table)])
  (:require [clojure.java.jdbc :as jdbc])
  (:import (java.io BufferedReader InputStreamReader StringReader))
  (:import (java.sql SQLException))
  (:import (com.zensol.gui.rs ResultSetFrame))
  (:import (com.zensol.gui.pref ConfigPrefFrame PrefsListener PrefSupport))
  (:require [com.zensol.rsgui.display-results :as dis])
  (:require [com.zensol.cisql.conf :as conf]))

(def products ["mysql" "postgresql" "sqlite"])

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

(defn- db-id-format
  "Get a string formatted for the DB."
  ([]
   (db-id-format {:user? true}))
  ([{user? :user?}]
   (let [{user :user url :url} (db-info)]
     (format "%s%s"
             (if user?
               (str (second (re-find #"^(.*)@.*$" user)) "@")
               "")
             (second (re-find #"jdbc:.*?://(.*)$" url))))))

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
                                  (db-id-format {:user? false})
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

(defn- display-result-set [rs meta]
  (try
    (if (conf/config :gui)
      (binding [dis/frame-factory-fn (fn []
                                       (let [frame (ResultSetFrame. false)]
                                         (.init frame)
                                         frame))]
        (dis/display-results (fn [frame]
                               (.displayResults (.getResultSetPanel frame) rs true))
                             :title (db-id-format)))
      (let [rows (slurp-result-set rs meta)]
        (print-table (map #(.getColumnLabel meta %)
                          (range 1 (+ 1 (.getColumnCount meta))))
                     (if (empty? rows) [{}] rows))
        (count rows)))
    (finally
      (try
        (.close rs)
        (catch SQLException e
          (log/error (format-sql-exception e)))))))

(defn execute-query [query]
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
                    meta (.getMetaData rs)
                    row-count (display-result-set rs meta)]
                (when (conf/config :gui)
                  (pr-row-count start row-count)))
              (pr-row-count start (.getUpdateCount stmt))))
          (reset! last-frame-label query)
          (catch SQLException e
            (log/error (format-sql-exception e))
            (.cancel stmt))
          (finally
            (.close stmt)))))))

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
