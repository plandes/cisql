(ns ^{:doc "Process query at the command line from user input."
      :author "Paul Landes"}
    zensols.cisql.process-query
  (:import (java.io BufferedReader InputStreamReader StringReader))
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [zensols.actioncli.log4j2 :as lu]
            [zensols.actioncli.util :refer (trunc)]
            [zensols.actioncli.parse :as parse :refer (with-exception)]
            [zensols.cisql.conf :as conf]
            [zensols.cisql.db-access :as db]
            [zensols.cisql.read :as r]
            [zensols.cisql.directive :as di]))

(def ^:private last-query (atom nil))

(def ^:private init-file
  (-> (System/getProperty "user.home")
      (io/file ".cisql")))

(declare ^:private run-file)
(declare ^:private run-reader)

(defn- invoke
  "Invoke the directive or command-event-loop function and handle errors."
  [handle-fn query-data & args]
  (log/debugf "invoke: %s <%s>" handle-fn (pr-str query-data))
  (binding [parse/*dump-jvm-on-error* false
            parse/*rethrow-error* (conf/config :prex)
            parse/*include-program-in-errors* false]
    (with-exception
      (apply handle-fn query-data args))))

(defn- process-query
  "Process the query data, which is a query or directive processing."
  [dir-fn query-data directive directives]
  (cond dir-fn
        (invoke dir-fn query-data)
        (map? directive)
        (let [{:keys [name args]} directive
              {:keys [fn]} (get directives (clojure.core/name name))]
          (log/tracef "name %s -> fn %s (%s)" name fn directive)
          (if-not fn
            (-> (pr-str directive)
                (#(format "no function defined for directive %s: %s"
                          name %))
                (ex-info {:directive directive
                          :query-data query-data})
                throw))
          (let [context (assoc query-data :last-query @last-query)]
            (log/debugf "context: <%s>" (pr-str context))
            (invoke fn context args)))
        true
        (-> (format "unknown query--probably missing query terminator (%s): %s"
                     :linesep
                     (trunc (pr-str query-data)))
            (ex-info {:query-data (trunc query-data)})
            throw)))

(defn process-queries
  "Process a line of user input and use callback functions **dir-fns**, which
  is a map of functions that are called by key based on the following actions:

  * **:end-query** called when the user is completed her input and wants to send
  it to be processed
  * **:end-session** the user has given the exit command
  * **:end-file** the user has hit the end of file sequence (CTRL-D)"
  ([dir-fns]
   (process-queries dir-fns nil))
  ([dir-fns query-data]
   (log/debugf "entry query-data: %s" query-data)
   (let [one-shot? (not (nil? query-data))]
     (loop [query-data (or query-data (r/read-query))]
       (log/debugf "query data: %s" query-data)
       (let [{:keys [directive]} query-data
             dir-fn (get dir-fns directive)
             directives (di/directives-by-name)]
         (log/tracef "directive: %s" directive)
         (let [res (process-query dir-fn query-data directive directives)]
           (when (map? res)
             (cond (contains? res :eval)
                   (doseq [user-input (:eval res)]
                     (with-open [reader (-> (StringReader. user-input)
                                            (BufferedReader.))]
                       (run-reader reader)))
                   (contains? res :run)
                   (doseq [file (:run res)]
                     (run-file file)))))
         (when (and (not one-shot?) (= :end-of-query directive))
           (log/debug "while read")
           (recur (r/read-query))))))
   (log/debugf "leave query-data: %s" query-data)))

(defn- init-thread-exception-handler
  "Configure a default exception handler so we swallow exceptions in forked
  threads."
  []
  (Thread/setDefaultUncaughtExceptionHandler
   (reify Thread$UncaughtExceptionHandler
     (uncaughtException [_ thread ex]
       (log/error ex "Uncaught exception on" (.getName thread))))))

(defn- run-reader [reader]
  (binding [r/*std-in* reader
            r/*print-prompt* false]
    (let [loop-again (atom true)]
      (letfn [(stop-loop [_]
                (reset! loop-again false))]
        (while @loop-again
          (process-queries
           {:end-of-query (fn [{:keys [query]}]
                            (db/assert-connection)
                            (db/execute-query query))
            :end-of-session stop-loop
            :end-file stop-loop}))))))

(defn- run-file [file]
  (with-open [reader (io/reader file)]
    (run-reader reader)))

(defn start-event-loop
  "Start the command event loop using standard in/out."
  []
  (init-thread-exception-handler)
  (di/init-grammer)
  (db/configure-db-access)
  (try
    (if (.exists init-file)
      (run-file init-file))
    (catch Exception e
      (log/error e)))
  (while true
    (try
      (binding [r/*std-in* (BufferedReader. (InputStreamReader. System/in))]
        (process-queries
         {:end-of-query (fn [{:keys [query]}]
                          (db/assert-connection)
                          (db/execute-query query)
                          (if query
                            (reset! last-query query)))
          :end-of-session (fn [_]
                            (println "exiting...")
                            (System/exit 0))
          :end-file (fn [_]
                      (System/exit 0))}))
      (catch Exception e
        (log/error e)))))
