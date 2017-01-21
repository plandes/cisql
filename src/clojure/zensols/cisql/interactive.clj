(ns ^{:doc "This package manages DB specs.  It also class loads and optionally
downloads the JDBC drivers."
      :author "Paul Landes"}
    zensols.cisql.interactive
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as s])
  (:require [zensols.actioncli.parse :refer (with-exception)]
            [zensols.actioncli.repl :as repl])
  (:require [zensols.cisql.process-query :as query]
            [zensols.cisql.spec :as spec]
            [zensols.cisql.conf :as conf]))

(defn create-db-spec
  [opts]
  (if-not (:name opts)
    (throw (ex-info "Missing -n parameter." {})))
  (if-not (:database opts)
    (throw (ex-info "Missing -d parameter." {})))
  (spec/db-spec (select-keys opts [:name :user :password :host :database])))

(defn- configure [conf]
  (doseq [[k v] conf]
    (log/debugf "%s -> %s" (keyword k) v)
    (conf/set-config (keyword k) v)))

(def interactive-command
  "CLI command to start an interactive session."
  {:description "Start an interactive session"
   :options
   [["-n" "--name <vender>" "DB implementation name"
     :validate [#(contains? (set (spec/registered-names)) %)
                (str "Must be one of: "
                     (s/join ", " (spec/registered-names)))]]
    ["-u" "--user <string>" "login name"]
    ["-p" "--password <string>" "login password"]
    ["-h" "--host <string>" "database host name"
     :default "localhost"]
    ["-d" "--database <string>" "database name"]
    [nil "--port <number>" "database port"
     :parse-fn #(Integer/parseInt %)
     :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
    ["-c" "--config <k1=v1>[,k2=v2] ..." "set session configuration"
     :parse-fn (fn [op]
                 (map #(s/split % #"=") (s/split op #"\s*,\s*")))]
    (repl/repl-port-set-option nil "--repl")]
   :app (fn [{:keys [repl config] :as opts} & args]
          (with-exception
            (let [dbspec (create-db-spec opts)]
              (if repl
                (future (repl/run-server {:port 12345})))
              (and config (configure config))
              (conf/print-help nil)
              (log/infof "connecting to %s..." (:subname dbspec))
              (log/debugf "dbspec: %s" dbspec)
              (query/start-event-loop dbspec))))})
