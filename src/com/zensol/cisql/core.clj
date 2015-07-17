(ns com.zensol.cisql.core
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io])
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as str]
            [clojure.pprint :only (pprint)])
  (:require [com.zensol.cisql.event-loop :as el]
            [com.zensol.cisql.db-access :as db])
  (:import (clojure.lang ExceptionInfo))
  (:gen-class :main true))

(def ^:private product-list
  (str/join ", " db/products))

(def ^:private cli-options
  [["-s" "--subprotocol <vender>" "DB implementation"
    :validate [#(contains? (set db/products) %)
               (str "Must be one of: " product-list)]]
   ["-u" "--user <name>" "login name"]
   ["-p" "--password <pass>" "login password"]
   ["-h" "--host <name>" "database host name"
    :default "localhost"]
   ["-d" "--database <DB name>" "database name"]
   [nil "--port <number>" "database port"]
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

(defn -main [& args]
  (let [{summary :summary opts :options errs :errors}
        (parse-opts args cli-options :in-order true)]
    (try
      (if errs
        (throw (ExceptionInfo. (error-msg errs) {})))
      (if (:help opts)
        (do
          (println summary)
          (println "Database subprotocols include:" product-list))
        (let [dbspec (create-db-spec opts)]
          (log/infof "connecting to %s..." (:subname dbspec))
          (log/debugf "dbspec: %s" dbspec)
          (el/start dbspec)))
      (catch ExceptionInfo e
        (binding [*out* *err*]
          (println (.getMessage e))
          (print \newline)
          (println summary))))))
