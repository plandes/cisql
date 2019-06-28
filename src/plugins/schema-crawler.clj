(ns zensols.cisql.plugin.schema-crawler
  (:import [java.nio.file Paths]
           [schemacrawler Main]
           [schemacrawler.tools.executable SchemaCrawlerExecutable]
           [schemacrawler.tools.databaseconnector DatabaseConnectionOptions]
           [schemacrawler.tools.options OutputOptionsBuilder])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as s]
            [clojure.java.jdbc :as jdbc]
            [zensols.cisql.db-access :as dba]
            [zensols.cisql.directive :as di]))

(defn help []
  (schemacrawler.Main/main (into-array ["-?"])))

(defn invoke-command [command output-format output-file]
  (log/infof "invoking %s using format %s with output %s"
             command output-format output-file)
  (let [path (Paths/get output-file (into-array java.lang.String []))
        opts (OutputOptionsBuilder/newOutputOptions output-format path)
        exec (SchemaCrawlerExecutable. command)]
    (jdbc/with-db-connection [db (dba/db-spec)]
      (log/infof "spec: %s, opts: %s" db opts)
      (doto exec
        (.setOutputOptions opts)
        (.setConnection (jdbc/db-connection db))
        (.execute)))
    output-file))

(def dependencies
  "Schema crawler dependencies.  The last is needed for printing the help."
  (let [version "15.06.01"]
    (->> '[us.fatehi/schemacrawler
           us.fatehi/schemacrawler-api
           us.fatehi/schemacrawler-commandline]
         (map (fn [d] [d version]))
         (array-map :coordinates))))

(def directives
  [{:name "schema"
    :arg-count "+"
    :usage "<out file> [format]"
    :desc "create a schema diagram of the database"
    :fn (fn [opts args]
          (di/assert-no-query opts)
          (if (> (count args) 2)
            (throw (ex-info "unexpected arguments found" {})))
          (let [output-file (first args)
                output-format (if (> (count args) 1)
                                (second args)
                                (-> (re-find #"^(.+)\.(.+?)$" (first args))
                                    (nth 2)
                                    (or (first args))))]
            (log/infof "creating schema format %s to %s"
                       output-format output-file)
            (invoke-command "schema" output-format output-file)))}
   {:name "schemausage"
    :arg-count 0
    :desc "print a command line usage for the schema crawler command line"
    :fn (fn [& args]
          (help))}])
