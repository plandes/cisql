(ns zensols.cisql.export
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.pprint :refer (pprint print-table)]
            [zensols.tabres.display-results :as dis]
            [zensols.cisql.conf :as conf]
            [zensols.cisql.db-access :as db]))

(defn- export-table-csv
  "Execute **query** and save as an CSV file to **filename**."
  [query filename]
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

(defn- narrow-query
  "Return either **query** or **last-query** based on what's available."
  [query last-query]
  (log/debugf "last query: %s" last-query)
  (let [narrowed (or query last-query)]
    (if-not narrowed
      (binding [*out* *err*]
        (-> "no queued query (skip the query delimiter (%s) after SQL)"
            (format (conf/config :linesep))
            (ex-info {:query query
                      :last-query last-query})
            throw)))
    (if-not query
      (println "no queued query so using the last executed"))
    narrowed))

(defn export-query-to-csv
  "Execute **query** or **last-query** and save to a CSV file."
  [query last-query csv-name]
  (-> (narrow-query query last-query)
      (export-table-csv csv-name)))

(defn- display-results
  "Display results either in text or as a graphical swing table.
  
  See [[zensols.cisql.db-access/display-result-set]]."
  [data header title]
  (if (conf/config :gui)
    (let [data (->> data
                    (map (fn [row]
                           (map #(get row %) header))))]
     (dis/display-results data :columns header :title title))
    (print-table header data)))

(defn export-query-to-function
  "Export result set output to a the last defined Clojure function in a file."
  [query last-query clj-file]
  (let [handler-fn (load-file clj-file)
        title (name (:name (meta handler-fn)))
        res-inst (atom nil)]
    (letfn [(rs-handler [rs]
              (let [rs-data (db/result-set-to-array rs :make-string? false)
                    {:keys [header rows]} rs-data]
                (reset! res-inst (handler-fn rows header))
                (count (:rows rs-data))))]
      (-> (narrow-query query last-query)
          (db/execute-query rs-handler))
      (let [res @res-inst
            {:keys [display]} res]
        (if display
          (display-results (:rows display) (:header display) title)
          (if res
            (println res)))))))

(defn export-query-to-eval
  [query last-query code]
  (let [handler-fn (eval (read-string code))
        title "eval"
        res-inst (atom nil)]
    (letfn [(rs-handler [rs]
              (let [rs-data (db/result-set-to-array rs :make-string? false)
                    {:keys [header rows]} rs-data]
                (reset! res-inst (handler-fn rows header))
                (count (:rows rs-data))))]
      (-> (narrow-query query last-query)
          (db/execute-query rs-handler))
      (let [res @res-inst
            {:keys [display]} res]
        (if display
          (display-results (:rows display) (:header display) title)
          (if res
            (println res)))))))
