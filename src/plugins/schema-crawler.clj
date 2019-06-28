(ns zensols.cisql.plugin.schema-crawler
  (:import [java.nio.file Paths]
           [schemacrawler.tools.executable SchemaCrawlerExecutable]
           ;[schemacrawler.tools.databaseconnector DatabaseConnectionOptions]
           [schemacrawler.tools.options OutputOptionsBuilder]

           [us.fatehi.commandlineparser CommandLineUtility]
           [schemacrawler.tools.commandline ApplicationOptionsParser]
           [schemacrawler.tools.commandline SchemaCrawlerCommandLine])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as s]
            [clojure.java.jdbc :as jdbc]
            [clojure.reflect :as rf]
            [wall.hack :as wh]
            [zensols.cisql.db-access :as dba]
            [zensols.cisql.directive :as di]))

(defn help []
  (schemacrawler.Main/main (into-array ["-?"])))

(defn- execute-command [exec]
  (jdbc/with-db-connection [db (dba/db-spec)]
    (doto exec
      (.setConnection (jdbc/db-connection db))
      (.execute))))

(defn invoke-command [command output-format output-file]
  (log/infof "invoking %s using format %s with output %s"
             command output-format output-file)
  (let [path (Paths/get output-file (into-array java.lang.String []))
        opts (OutputOptionsBuilder/newOutputOptions output-format path)
        exec (SchemaCrawlerExecutable. command)]
    (jdbc/with-db-connection [db (dba/db-spec)]
      (log/debugf "spec: %s, opts: %s" db opts)
      (->> (doto exec
             (.setOutputOptions opts))
           execute-command))
    output-file))

(defn invoke-command-line [command-line]
  (let [cl (-> (str command-line " -url=none")
               (s/split #"\s+")
               into-array
               (CommandLineUtility/parseArgs)
               (SchemaCrawlerCommandLine.))
        clcls SchemaCrawlerCommandLine]
    (letfn [(field [fname]
              (wh/field SchemaCrawlerCommandLine fname cl))]
      (->> (doto (SchemaCrawlerExecutable. (.getCommand cl))
             (.setOutputOptions (field :outputOptions))
             (.setSchemaCrawlerOptions (field :schemaCrawlerOptions))
             (.setAdditionalConfiguration (field :config)))
           execute-command)))
  command-line)

(def dependencies
  "Schema crawler dependencies.  The last is needed for printing the help."
  (let [version "15.06.01"]
    (->> '[us.fatehi/schemacrawler
           us.fatehi/schemacrawler-api
           us.fatehi/schemacrawler-commandline]
         (map (fn [d] [d version]))
         (cons '[clj-wallhack "1.0.1"])
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
   {:name "schemacl"
    :arg-count "-"
    :usage "<command line arguments>"
    :desc "execute the SchemaCrawler library by command line"
    :fn (fn [opts [command-line]]
          (di/assert-no-query opts)
          (log/errorf "invoking with command line: '%s'" command-line)
          (invoke-command-line command-line))}
   {:name "schemausage"
    :arg-count 0
    :desc "print a command line usage for the schema crawler command line"
    :fn (fn [opts args]
          (di/assert-no-query opts)
          (help))}])
