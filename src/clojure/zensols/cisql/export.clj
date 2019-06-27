(ns ^{:doc "This library contains functions that allow for exporting of table data.

This includes functions to export to CSV files and adhoc Clojure functions."
      :author "Paul Landes"}
    zensols.cisql.export
    (:require [clojure.tools.logging :as log]
              [clojure.java.io :as io]
              [clojure.data.csv :as csv]
              [zensols.tabres.display-results :as dis]
              [zensols.cisql.conf :as conf]
              [zensols.cisql.db-access :as db]))

(defn export-table-csv
  "Execute **query** and save as an CSV file to **filename**."
  [query filename]
  (letfn [(rs-handler [rs _]
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

(defn- function-by-name
  "Export result set output to a the last defined Clojure function in a file."
  [clj-file fn-name]
  (let [last-fn (load-file clj-file)
        handler-fn (if (nil? fn-name)
                     last-fn
                     (-> (:ns (meta last-fn))
                         ns-name
                         (ns-resolve (symbol fn-name))))]
    (or handler-fn
        (-> (format "no such function %s found in file %s" fn-name clj-file)
            (ex-info {:file clj-file :fn-name fn-name})
            throw))))

(defn export-query-to-function
  "Export result set output to a the last defined Clojure function in a file."
  [query clj-file fn-name]
  (let [handler-fn (function-by-name clj-file fn-name)
        fmeta (meta handler-fn)
        fn-name (name (:name fmeta))
        res-inst (atom nil)]
    (letfn [(rs-handler [rs _]
              (let [rs-data (and rs (db/result-set-to-array rs))
                    {:keys [header rows]} rs-data
                    argn (count (first (:arglists (meta handler-fn))))]
                (if (and (> argn 0) (nil? query))
                  (-> (format "function %s expects a query with %d argument(s)"
                              fn-name argn)
                      (ex-info {:argn argn
                                :fn-name fn-name})
                      throw))
                (->> (case argn
                       0 (handler-fn)
                       1 (handler-fn rows)
                       (handler-fn rows header))
                     (reset! res-inst))
                (count (:rows rs-data))))]
      (log/infof "evaluating function %s" fn-name)
      (if query
        (db/execute-query query rs-handler)
        (rs-handler nil nil))
      (let [res @res-inst
            {:keys [display]} res]
        (if display
          (db/display-results (:rows display) (:header display) fn-name)
          (if res
            (println res)))))))

(defn export-query-to-eval
  [query code]
  (let [handler-fn (eval (read-string code))
        title "eval"
        res-inst (atom nil)]
    (letfn [(rs-handler [rs _]
              (let [rs-data (db/result-set-to-array rs)
                    {:keys [header rows]} rs-data]
                (reset! res-inst (handler-fn rows header))
                (count (:rows rs-data))))]
      (db/execute-query query rs-handler)
      (let [res @res-inst
            {:keys [display]} res]
        (if display
          (db/display-results (:rows display) (:header display) title)
          (if res
            (println res)))))))
