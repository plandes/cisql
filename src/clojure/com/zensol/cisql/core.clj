(ns com.zensol.cisql.core
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as str]
            [clojure.pprint :only (pprint)])
  (:import (java.io BufferedReader InputStreamReader))
  (:require [com.zensol.cisql.process-query :as query]
            [com.zensol.cisql.db-access :as db]
            [com.zensol.cisql.conf :as conf]
            [com.zensol.cisql.repl :as repl])
  (:import (clojure.lang ExceptionInfo))
  (:gen-class :main true))

(def ^:private product-list
  (str/join ", " db/products))

(def ^:private cli-options
  [["-s" "--subprotocol <vender>" "DB implementation"
    :validate [#(contains? (set db/products) %)
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
    :parse-fn (fn [op] (map #(str/split % #"=") (str/split op #"\s*,\s*")))]
   [nil "--repl" "start the REPL"]
   ["-v" "--version"]
   [nil "--help"]])

(defn- map-subproto [name host port-or-nil database]
  (let [portn (or port-or-nil
                  (if name
                    (case name
                      "mysql" 3306
                      "postgresql" 5432
                      nil)))
        port (if portn (format ":%d" portn) "")]
    (case name
      "sqlite" "sqlite"
      (format "//%s%s/%s" host port database))))

(defn- create-db-spec [opts]
  (if-not (:database opts)
    (throw (ExceptionInfo. "Missing -d parameter." {})))
  (if-not (:subprotocol opts)
    (throw (ExceptionInfo. "Missing -s parameter." {})))
  (let [spec
        (apply merge
               (cons {:subname
                      (apply map-subproto
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

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn- print-help [summary]
  (println (conf/format-intro))
  (println \newline)
  (println summary)
  (println \newline)
  (println "Database subprotocols include:" product-list))

(defn start-event-loop [dbspec]
  (log/debug "staring loop")
  (binding [query/*std-in* (BufferedReader. (InputStreamReader. System/in))]
    (query/process-queries
     {:end-query #(do (db/execute-query (:query %) dbspec))
      :end-session (fn [_]
                     (println "exiting...")
                     (System/exit 0))
      :end-file (fn [_] (System/exit 0))})))

(defn- configure [conf]
  (doseq [[k v] conf]
    (log/debugf "%s -> %s" (keyword k) v)
    (conf/set-config (keyword k) v)))

(defn -main [& args]
  (let [{summary :summary opts :options errs :errors}
        (parse-opts args cli-options :in-order true)]
    (if (:repl opts)
      (future (repl/run-server)))
    (try
      (if errs
        (throw (ExceptionInfo. (error-msg errs) {})))
      (cond (:help opts) (print-help summary)
            (:version opts) (println (conf/format-version))
            true
            (let [dbspec (create-db-spec opts)]
              (if (:config opts)
                (configure (:config opts)))
              (conf/print-help false)
              (log/infof "connecting to %s..." (:subname dbspec))
              (log/debugf "dbspec: %s" dbspec)
              (start-event-loop dbspec)))
      (catch ExceptionInfo e
        (binding [*out* *err*]
          (println (.getMessage e))
          (print \newline)
          (println summary))))))
