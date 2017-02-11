(ns ^{:doc "Command line entry point."
      :author "Paul Landes"}
    zensols.cisql.core
  (:require [clojure.string :as s])
  (:require [zensols.actioncli.log4j2 :as lu]
            [zensols.actioncli.parse :as parse])
  (:require [zensols.cisql.conf :as conf]
            [zensols.cisql.spec :as spec])
  (:require [zensols.cisql.version :as ver])
  (:gen-class :main true))

(defn- print-help [summary]
  (with-out-str
    (println)
    (println (conf/format-intro))
    (println)
    (println summary)
    (println)
    (print "Database subprotocols include:"
           (s/join ", " (spec/registered-names)))
    (flush)))

(defn- version-info []
  (println (format "%s (%s)" ver/version ver/gitref)))

(defn- create-action-context []
  (parse/multi-action-context
   '((:interactive zensols.cisql.interactive interactive-command)
     (:describe zensols.cisql.spec driver-describe-command)
     (:add zensols.cisql.spec driver-add-command)
     (:purge zensols.cisql.spec driver-user-registry-purge-command))
   :action-print-order [:interactive :describe :add :purge]
   :version-option (parse/version-option version-info)
   :print-help-fn print-help))

(defn -main [& args]
  ;(lu/configure "cisql-log4j2.xml")
  (parse/set-program-name "cisql")
  (-> (create-action-context)
      (parse/process-arguments args)))
