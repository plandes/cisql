(ns ^{:doc "Command line entry point."
      :author "Paul Landes"}
    zensols.cisql.core
  (:require [clojure.string :as s])
  (:require [zensols.actioncli.log4j2 :as lu]
            [zensols.actioncli.parse :as parse])
  (:require [zensols.cisql.conf :as conf]
            [zensols.cisql.spec :as spec])
  (:require [cisql.version])
  (:gen-class :main true))

(defn- print-help [summary]
  (with-out-str
    (println (conf/format-intro))
    (println)
    (println summary)
    (println)
    (println "Database subprotocols include:"
             (s/join ", " (spec/registered-names)))))

(def ^:private version-info-command
  {:description "Get the version of the application."
   :options [["-g" "--gitref"]]
   :app (fn [{refp :gitref} & args]
          (println cisql.version/version)
          (if refp (println cisql.version/gitref)))})

(defn create-command-context []
  {:command-defs '((:interactive zensols.cisql interactive interactive-command))
   :single-commands {:version version-info-command}
   :command-print-order [:interactive :version]
   :print-help-fn print-help})

(defn -main [& args]
  (lu/configure "cisql-log4j2.xml")
  (parse/set-program-name "cisql")
  (let [command-context (create-command-context)]
    (apply parse/process-arguments command-context args)))
