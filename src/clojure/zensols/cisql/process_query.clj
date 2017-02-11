(ns ^{:doc "Process query at the command line from user input."
      :author "Paul Landes"}
    zensols.cisql.process-query
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:import (java.io BufferedReader InputStreamReader StringReader))
  (:require [zensols.actioncli.log4j2 :as lu]
            [zensols.actioncli.parse :as parse :refer (with-exception)])
  (:require [zensols.cisql.conf :as conf]
            [zensols.cisql.db-access :as db]
            [zensols.cisql.table-export :as te]
            [zensols.cisql.read :as r]
            [zensols.cisql.directive :as di]))

(def ^:private last-query (atom nil))

(defn- invoke
  "Invoke the directive or command-event-loop function and handle errors."
  [handle-fn query-data & args]
  (log/debugf "invoke: %s <%s>" handle-fn (pr-str query-data))
  (binding [parse/*dump-jvm-on-error* false
            parse/*rethrow-error* false
            parse/*include-program-in-errors* false]
    (with-exception
      (apply handle-fn query-data args))))

(defn process-queries
  "Process a line of user input and use callback functions **dir-fns**, which
  is a map of functions that are called by key based on the following actions:

* **:end-query** called when the user is completed her input and wants to send
  it to be processed
* **:end-session** the user has given the exit command
* **:end-file** the user has hit the end of file sequence (CTRL-D)"
  [dir-fns]
  (loop [query-data (r/read-query)]
    (log/tracef "query data: %s" query-data)
    (log/debugf "query: <%s>" (:query query-data))
    (let [{:keys [directive]} query-data
          dir-fn (get dir-fns directive)
          directives (di/directives-by-name)]
      (log/tracef "directive: %s" directive)
      (cond dir-fn
            (dir-fn query-data)
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
            (-> (str "don't know what to do with query data: "
                     (pr-str query-data))
                (ex-info {:query-data query-data})
                throw))
      (when (= :end-of-query directive)
        (recur (r/read-query))))))

(defn- assert-connection []
  (let [conn? (db/connected?)]
    (log/debugf "connected: %s" conn?)
    (if-not conn?
      (binding [parse/*include-program-in-errors* false]
        (->> ["no connection; 'connect help'"]
             parse/print-error-message)))
    conn?))

(defn start-event-loop
  "Start the command event loop using standard in/out."
  []
  (di/init-grammer)
  (while true
    (try
      (binding [r/*std-in* (BufferedReader. (InputStreamReader. System/in))]
        (process-queries
         {:end-of-query (fn [{:keys [query]}]
                          (when (assert-connection)
                            (db/execute-query query)
                            (if query (reset! last-query query))))
          :end-of-session (fn [_]
                         (println "exiting...")
                         (System/exit 0))
          :end-file (fn [_] (System/exit 0))}))
      (catch Exception e
        (log/error e)))))
