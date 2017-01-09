(ns ^{:doc "Command line entry point namesakes."
      :author "Paul Landes"}
  zensols.cisql.core
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as s])
  (:require [zensols.actioncli.repl :as repl]
            [zensols.actioncli.log4j2 :as lu]
            [zensols.actioncli.parse :as zp])
  (:require [zensols.cisql.process-query :as query]
            [zensols.cisql.db-meta :as dbm]
            [zensols.cisql.conf :as conf])
  (:import (clojure.lang ExceptionInfo))
  (:gen-class :main true))

(def ^:private product-list
  (s/join ", " dbm/products))

(def ^:private cli-options
  [["-s" "--subprotocol <vender>" "DB implementation"
    :validate [#(contains? (set dbm/products) %)
               (str "Must be one of: " product-list)]]
   ["-u" "--user <string>" "login name"]
   ["-p" "--password <string>" "login password"]
   ["-h" "--host <string>" "database host name"
    :default "localhost"]
   ["-d" "--database <string>" "database name"]
   [nil "--port <number>" "database port"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-c" "--config <key1=val1>[,key2=val2] ..."
    :parse-fn (fn [op] (map #(s/split % #"=") (s/split op #"\s*,\s*")))]
   [nil "--repl" "start the REPL"]
   ["-v" "--version"]
   [nil "--help"]])

(defn create-db-spec
  [opts]
  (if-not (:database opts)
    (throw (ex-info "Missing -d parameter." {})))
  (if-not (:subprotocol opts)
    (throw (ex-info "Missing -s parameter." {})))
  (let [spec
        (apply merge
               (cons {:subname
                      (apply dbm/map-subproto
                             (map #(get opts %)
                                  [:subprotocol :host :port :database]))}
                     (map (fn [key]
                            (let [mval (get opts key)]
                              (if mval {key mval})))
                          [:subprotocol :user :password])))]
    (case (:subprotocol opts)
      "sqlite"
      (merge {:classname "org.sqlite.JDBC"}
             (assoc spec :subname (io/file (:database opts))))
      spec)))

(defn- print-help [summary]
  (println (conf/format-intro))
  (println)
  (println summary)
  (println)
  (println "Database subprotocols include:" product-list))

(defn- configure [conf]
  (doseq [[k v] conf]
    (log/debugf "%s -> %s" (keyword k) v)
    (conf/set-config (keyword k) v)))

(defn -main [& args]
  (lu/configure "cisql-log4j2.xml")
  (zp/set-program-name "cisql")
  (let [{summary :summary opts :options errs :errors}
        (parse-opts args cli-options :in-order true)]
    (if (:repl opts)
      (future (repl/run-server)))
    (try
      (if errs
        (throw (ex-info (zp/error-msg errs) {})))
      (cond (:help opts) (print-help summary)
            (:version opts) (println (conf/format-version))
            true
            (let [dbspec (create-db-spec opts)]
              (if (:config opts)
                (configure (:config opts)))
              (conf/print-help nil)
              (log/infof "connecting to %s..." (:subname dbspec))
              (log/debugf "dbspec: %s" dbspec)
              (query/start-event-loop dbspec)))
      (catch ExceptionInfo e
        (zp/handle-exception e)))))

