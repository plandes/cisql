(ns zensols.cisql.export
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [zensols.cisql.conf :as conf]
            [zensols.cisql.db-access :as db]))

(defn- export-table-csv [query filename]
  (letfn [(rs-handler [rs]
            (let [rs-data (db/result-set-to-array rs)
                  csv (map (fn [row]
                             (map #(get row %) (:header rs-data)))
                           (:rows rs-data))
                  csv-with-header (cons (:header rs-data) csv)]
              (with-open [out (io/writer filename)]
                (csv/write-csv out csv-with-header))
              (count (:rows rs-data))))]
    (log/infof "exporting csv to %s" filename)
    (db/execute-query query rs-handler)))

(defn export-query-to-csv [query last-query csv-name]
  (log/debugf "last query: %s" last-query)
  (let [to-export (or query last-query)]
    (if-not to-export
      (binding [*out* *err*]
        (-> "no queued query (skip the query delimiter (%s) after SQL)"
            (format (conf/config :linesep))
            (ex-info {:query query
                      :last-query last-query
                      :csv-name csv-name})
            throw)))
    (if-not query
      (println "no queued query so using the last executed"))
    (export-table-csv to-export csv-name)))
