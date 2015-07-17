(ns com.zensol.cisql.event-loop
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:require [clojure.java.jdbc :as jdbc])
  (:import (java.sql SQLException))
  (:import (java.io BufferedReader InputStreamReader))
  (:require [com.zensol.cisql.conf :as c]
            [com.zensol.cisql.db-access :as db]
            [com.zensol.cisql.process-query :as query]))

(def dbspec {:subprotocol "mysql"
             :subname "//clihost:3306/taskmatch"
             :user "taskmatch"
             :password "taskmatch"
             :useUnicode "yes"
             :characterEncoding "UTF-8"})

(defn start [dbspec]
  (log/debug "staring loop")
  (binding [query/*std-in* (BufferedReader. (InputStreamReader. System/in))]
    (query/process-queries
     {:prompt-for-input (fn [] (print " > ") (flush))
      :end-query #(do (db/execute-query (:query %) dbspec))
      :end-session (fn [_] (println "bye"))
      :end-file (fn [_] (println "hit end of file"))})))
