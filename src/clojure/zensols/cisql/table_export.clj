(ns zensols.cisql.table-export
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv])
  (:require [zensols.cisql.db-access :as db]))

(defn export-table-csv [query filename]
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
