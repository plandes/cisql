(ns ^{:doc "Layer between the application and JDBC.  It also makes calls to
print and display result sets."
      :author "Paul Landes"}
    zensols.cisql.db-access
  (:import [sun.misc Signal SignalHandler]
           [java.io BufferedReader InputStreamReader StringReader]
           [java.sql SQLException]
           (com.zensols.gui.tabres ResultSetFrame))
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [clojure.pprint :refer (pprint print-table)]
            [zensols.tabres.display-results :as dis]
            [zensols.actioncli.parse :as parse]
            [zensols.cisql.conf :as conf]))

(def ^:private dbspec
  "The jdbc package database spec and contains the DB host name, port etc."
  (atom nil))

(def ^:private current-catalog
  "For catalog based systems this contains the name of the catalog."
  (atom nil))

(def ^:private db-info-data
  "Information about the current database connection (i.e. user, URL)."
  (atom nil))

(def ^:private last-frame-label
  "The last *label* (frame title) of the results frame of the last query."
  (atom nil))

(def ^:private interrupt-execute-query
  "The state of the interrupt.  See [[interrupt]]."
  (atom nil))

(def ^:private query-thread
  "The thread of the active query."
  (atom nil))

(def ^:private query-no
  "The nth query, and used for thread syncs."
  (atom 0))

(def ^:private meta-query-pattern
  "The regular expression to identify a table meta data query."
  #"^\s*select\s*from\s*@@(?:(.+)\.)?meta")

(def ^:private last-frame-type
  "The state of the last type of frame used to display results."
  (atom nil))


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

(defn assert-connection []
  (let [conn? (connected?)]
    (log/debugf "connected: %s" conn?)
    (if-not conn?
      (throw (ex-info "no connection; try 'connect help'" {:connected? false})))
    conn?))

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

(defn- slurp-result-set
  "Return a lazy list of maps representing result set **rs** with table metadata
  **meta**.  If key :make-string? is non-nil all data is returned as strings."
  [rs meta & {:keys [make-string?]
              :or {make-string? true}}]
  (letfn [(make-row [col]
            {(.getColumnLabel meta col)
             (if make-string?
               (.getString rs col)
               (.getObject rs col))})
          (next-row [cols]
            (if (.next rs)
              (apply merge (map make-row (range 1 (+ 1 cols))))))]
    (let [cols (.getColumnCount meta)]
      (take-while identity (repeatedly #(next-row cols))))))

(defn result-set-to-array
  "Return result set **rs** as an array of rows and column names.

Return a map with entries:
  * **header**: the column header data
  * **rows**: a lazy list of maps each representing a row from the result set."
  [rs & opts]
  (let [meta (.getMetaData rs)
        col-count (.getColumnCount meta)
        ;; must for SQLite: get headers before the result set is used up
        header (doall (map #(.getColumnLabel meta %)
                           (range 1 (+ 1 col-count))))
        rows (apply slurp-result-set rs meta opts)]
    (log/debugf "col-count %d" col-count)
    {:header header
     :rows (if (empty? rows) [{}] rows)}))

(defn- new-frame? [new-frame-type]
  (let [[old _] (swap-vals! last-frame-type #(identity %2) new-frame-type)]
    (not (= new-frame-type old))))

(defn display-results
  "Display results either in text or as a graphical swing table.
  
  See [[zensols.cisql.db-access/display-result-set]]."
  [data header title]
  (if (conf/config :gui)
    (let [data (->> data
                    (map (fn [row]
                           (map #(get row %) header))))]
      (dis/display-results data
                           :column-names header
                           :title title
                           :new-frame? (new-frame? "default")))
    (print-table header data)))

(defn- display-result-set
  "Display result set **rs** either by printing it out to standard out or
  displaying it in a GUI table window.  The parameter **meta** is the meta data
  object from **rs**.

  See [[zensols.cisql.export/display-results]]."
  [rs meta]
  (try
    (if (conf/config :gui)
      (binding [dis/frame-factory-fn
                (fn []
                  (let [frame (ResultSetFrame. false)]
                    (.init frame)
                    frame))]
        (dis/display-results
         (fn [frame]
           (.displayResults (.getResultSetPanel frame) rs true))
         :title (db-id-format)
         :new-frame? (new-frame? "rs")))
      (let [rs-data (result-set-to-array rs)]
        (print-table (:header rs-data) (:rows rs-data))
        (count (:rows rs-data))))
    (finally
      (try
        (.close rs)
        (catch SQLException e
          (log/error (format-sql-exception e)))))))

(defn- check-interrupt
  "Throw an exception if an interrupt (signal) has occured or if queries are out
  of order."
  [curr-query-no]
  (let [ieq @interrupt-execute-query]
    (log/debugf "check interrupt: ieq=%s, query no: (%s == %s)"
                ieq curr-query-no @query-no)
    (when (or ieq (not (= curr-query-no @query-no)))
      (-> (ex-info (format "Interrupt query: %s" ieq)
                   {:curr-query-no curr-query-no
                    :interrupt-execute-query ieq})
          throw))))

(defn- table-metadata
  "Display the meta data of **table**, which includes column names and metadata."
  [table]
  (jdbc/with-db-connection [db @dbspec]
    (let [conn (resolve-connection db)
          dbmeta (.getMetaData conn)
          rs (if table
               (.getColumns dbmeta nil nil table "%")
               (.getTables dbmeta nil nil "%" nil))
          meta (.getMetaData rs)]
      rs meta)))

(defn- execute-jdbc-query
  "JDBC access to execute SQL **query** using optional **query-handler-fn**
  function with the results."
  [query query-handler-fn]
  (jdbc/with-db-connection [db @dbspec]
    (swap! query-no inc)
    (let [conn (resolve-connection db)
          stmt (.createStatement conn)
          curr-query-no @query-no]
      (log/debugf "query numbrer: %s" curr-query-no)
      (reset! query-thread (Thread/currentThread))
      (check-interrupt curr-query-no)
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
            (check-interrupt curr-query-no)
            (if (.execute stmt query)
              (let [rs (.getResultSet stmt)
                    meta (.getMetaData rs)]
                (try
                  (check-interrupt curr-query-no)
                  (pr-row-count start (query-handler-fn rs meta))
                  (finally (try
                             (.close rs)
                             (catch SQLException e
                               (log/error (format-sql-exception e)))))))
              (pr-row-count start (.getUpdateCount stmt))))
          (reset! last-frame-label query)
          (catch SQLException e
            (log/error (format-sql-exception e))
            (.cancel stmt))
          (finally
            (reset! query-thread nil)
            (.close stmt)))))))

(defn- execute-query-nowait
  "Execute SQL **query** using optional **query-handler-fn** function with the
  results."
  [query query-handler-fn]
  (let [[meta-query? table] (re-find meta-query-pattern query)]
    (if (and false meta-query?)
      (let [[rs meta] (table-metadata table)]
        (try
          (query-handler-fn rs meta)
          (finally (try
                     (.close rs)
                     (catch SQLException e
                       (log/error (format-sql-exception e)))))))
      (execute-jdbc-query query query-handler-fn))))

(defn- wait-atom-or-future
  [quit-atom fut timeout]
  (log/debugf "future done? %s" (future-done? fut))
  (letfn [(func []
            (let [sym (gensym)]
              (while (and (not @quit-atom)
                          (= sym (deref fut timeout sym)))
                (log/debugf "waiting on future...")))
            (future-cancel fut)
            (log/debugf "finished"))]
    (log/debugf "starting future: %s %s %s" quit-atom fut timeout)
    (future (func))))

(defn execute-query
  ([query]
   (execute-query query display-result-set))
  ([query query-handler-fn]
   (assert-connection)
   (if (not (conf/config :sigintercept))
     (execute-query-nowait query query-handler-fn)
     (do
       (reset! interrupt-execute-query nil)
       (let [query-fut (future (execute-query-nowait query query-handler-fn))
             wait-fut (wait-atom-or-future interrupt-execute-query query-fut 1000)]
         (deref wait-fut)
         (log/debugf "execute wait on feature complete"))))))

(defn show-table-metadata
  "Display the meta data of **table**, which includes column names and metadata."
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

(defn- interrupt
  "The signal handler will first put the state in the *interrupt* state.  If the
  program gets a second signal while in this state, the state goes to *kill*.
  In this state, the program *tries harder* (calls [[java.lang.Thread/stop]])
  to quit the current query with the database."
  [signal]
  (log/debugf "interrupting with signal: %s" signal)
  (let [ieq @interrupt-execute-query]
    (println)
    (println "Interrupt")
    (if (= ieq 'kill)
      (let [thread @query-thread]
        (print "Interrupt--kill")
        (if thread (.stop thread)))
      (swap! interrupt-execute-query
             (fn [state]
               (let [newstate (cond (nil? state) true
                                    true 'kill)]
                 (log/debugf "state: %s -> %s" state newstate)
                 newstate))))
    (log/debugf "set state to %s" @interrupt-execute-query)))

(defn configure-db-access
  "Add the JVM level signal handler and other resources."
  []
  ;; configure the signal handler
  (->> (proxy [SignalHandler] []
         (handle [s]
           (log/debugf "signal: %s" s)
           (interrupt s)))
       (Signal/handle (Signal. "INT"))))
