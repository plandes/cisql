(ns com.zensol.cisql.event-loop
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:require [clojure.java.jdbc :as jdbc])
  (:import (java.sql SQLException))
  (:import (java.io BufferedReader InputStreamReader))
  (:require [com.zensol.cisql.conf :as c]
            [com.zensol.cisql.db-access :as db]
            [com.zensol.cisql.process-query :as query]))

(defn start [dbspec]
  (log/debug "staring loop")
  (binding [query/*std-in* (BufferedReader. (InputStreamReader. System/in))]
    (query/process-queries
     {:end-query #(do (db/execute-query (:query %) dbspec))
      :end-session (fn [_] (println "exiting..."))
      :end-file (constantly true)})))
