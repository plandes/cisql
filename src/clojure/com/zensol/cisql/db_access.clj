(ns com.zensol.cisql.db-access
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:use [clojure.pprint :only (pprint print-table)])
  (:require [clojure.java.jdbc :as jdbc])
  (:import (java.io BufferedReader InputStreamReader StringReader))
  (:import (java.sql SQLException))
  (:import (com.zensol.rsgui ResultSetFrame))
  (:import (com.zensol.gui.pref ConfigPrefFrame PrefsListener PrefSupport))
  (:require [com.zensol.cisql.conf :as conf]))

(def products ["mysql" "postgresql" "sqlite"])

(def ^:private dbspec (atom nil))

(def ^:private result-frame-data (atom nil))

(def ^:private db-info-data (atom nil))

(def ^:private last-frame-label (atom nil))


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
               (let [conn (jdbc/db-connection db)
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
(defn- new-frame []
  (swap! result-frame-data
         (fn [frame]
           (when frame
             (.dispose frame))
           (let [frame (ResultSetFrame. false)]
             (.init frame)
             (.pack frame)
             frame))))

(defn- result-frame []
  (swap! result-frame-data #(or % (new-frame))))

(defn orphan-frame
  ([]
   (orphan-frame nil))
  ([label]
   (swap! result-frame-data
          (fn [frame]
            (when frame
              (.setDefaultCloseOperation
               frame javax.swing.WindowConstants/DISPOSE_ON_CLOSE)
              (.setEnabled (.getPrefSupport frame) false)
              (.setTitle frame (format "%s: %s"
                                       (db-id-format {:user? false})
                                       (or label @last-frame-label))))
            nil))))

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
            {(.getColumnName meta col) (.getString rs col)})]
    (let [cols (.getColumnCount meta)]
      (loop [result []]
        (if (.next rs)
          (recur (conj result
                       (apply merge (map make-row (range 1 (+ 1 cols))))))
          result)))))

(defn- display-result-set [rs meta]
  (try
   (if (conf/config :gui)
     (let [frame (result-frame)
           row-count (.displayResults (.getResultSetPanel frame) rs true)]
       (.pack frame)
       (if-not (.isVisible frame)
         (.setTitle frame (db-id-format)))
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

(defn execute-query [query]
  (jdbc/with-db-connection [db @dbspec]
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
     (let [conn (jdbc/db-connection db)
           dbmeta (.getMetaData conn)
           rs (if table
                (.getColumns dbmeta nil nil table "%")
                (.getTables dbmeta nil nil "%" nil))
           meta (.getMetaData rs)]
       (display-result-set rs meta)
       (reset! last-frame-label (format "table: %s" table))))))