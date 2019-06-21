(ns ^{:doc "This package manages DB specs.  It also class loads and optionally
downloads the JDBC drivers."
      :author "Paul Landes"}
    zensols.cisql.interactive
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [zensols.actioncli.parse :refer (with-exception) :as parse]
            [zensols.actioncli.repl :as repl]
            [zensols.actioncli.log4j2 :as lu]
            [zensols.cisql.cider-repl :as cr]
            [zensols.cisql.process-query :as query]
            [zensols.cisql.spec :as spec]
            [zensols.cisql.conf :as conf]
            [zensols.cisql.db-access :as db]))

(defn create-db-spec [{:keys [name] :as opts}]
  (log/debugf "creating db spec with opts: %s" opts)
  (spec/db-spec (select-keys opts [:name :user :password 
                                   :host :port :database])))

(defn- configure [conf]
  (doseq [[k v] conf]
    (if (nil? v)
      (-> (format "Missing configuration value for %s" k)
          (ex-info {:key k})
          throw))
    (log/debugf "%s -> %s" (keyword k) v)
    (conf/set-config (keyword k) v)))

(def interactive-directive
  "CLI command to start an interactive session."
  {:description "Connect to a database"
   :options
   [["-u" "--user <string>" "login name"
     :required "<string>"]
    ["-p" "--password <string>" "login password"
     :required "<string>"]
    ["-h" "--host <string>" "database host name"
     :default "localhost"]
    ["-d" "--database <string>" "database name"]
    [nil "--port <number>" "database port"
     :parse-fn #(Integer/parseInt %)
     :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]]
   :app (fn [opts & [name]]
          (log/debugf "connecting to %s" name)
          (binding [parse/*rethrow-error* (conf/config :prex)]
            (with-exception
              (if-not name
                (-> "no driver name given"
                    (ex-info {})
                    throw))
              (let [dbspec (create-db-spec (assoc opts :name name))]
                (log/debugf "setting db spec: %s" (pr-str dbspec))
                (if dbspec (db/set-db-spec dbspec))))))})

(def interactive-command
  "CLI command to start an interactive session."
  {:description (:description interactive-directive)
   :options
   (->> (concat [(spec/name-option false)
                 (lu/log-level-set-option)]
                (:options interactive-directive)
                [["-c" "--config <key/values>" "set session configuration"
                  :required "<k1=v1>[,k2=v2]"
                  :parse-fn (fn [op]
                              (map #(s/split % #"=") (s/split op #"\s*,\s*")))]])
        vec)
   :app (fn [{:keys [config name] :as opts} & args]
          (with-exception
            (let [dbspec (if name (create-db-spec opts))]
              (and config (configure config))
              (conf/print-help)
              (if dbspec
                (log/infof "connecting to %s..." (:subname dbspec)))
              (log/debugf "dbspec: %s" dbspec)
              (if dbspec (db/set-db-spec dbspec))
              (log/debugf "setting db spec: %s" (pr-str dbspec))
              (query/start-event-loop))))})
