(ns com.zensol.cisql.event-loop
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:use [clojure.pprint :only (pprint)])
  (:import (java.io BufferedReader InputStreamReader StringReader)))

(def ^{:private true
       :dynamic true} *std-in* nil)

(def ^:private config-data
  (atom {:line-terminator "go"
         :end-directive "end"}))

(def ^{:private true
       :dynamic true}
  *query* nil)

(defmacro with-query [& body]
  `(.append *query* (with-out-str ~@body)))

(defn set-config [key val]
  (swap! config-data assoc key val))

(defn config [key]
  (get @config-data key))

(defn- add-line [line]
  (letfn [(has-eol [key]
            (let [conform (str/lower-case (str/trim line))]
              (.endsWith conform (config key))))
          (find-keyword [key]
            (str/trim
             (subs line 0 (- (count line)
                             (count (config key))))))]
    (cond (has-eol :line-terminator)
          (do (with-query
                (print (find-keyword :line-terminator)))
              :end-query)

          (has-eol :end-directive)
          (do (with-query
                (print (find-keyword :end-directive)))
              :end-session)

          :default
          (do (with-query
                (println line))
              :continue))))

(defn- read-query []
  (binding [*query* (StringBuilder.)]
    (let [lines-left (atom 20)
          directive (atom nil)]
      (letfn [(end [dir]
                (log/debugf "query: %s" *query*)
                (reset! lines-left 0)
                (reset! directive dir))]
        (while (> @lines-left 0)
   (log/tracef "lines left: %d" @lines-left)
   (let [user-input (.readLine *std-in*)]
     (log/debugf "line: %s" user-input)
     (if-not user-input
       (do
         (log/debugf "read EOF from input")
         (reset! lines-left 0))
       (let [dir (add-line user-input)]
         (log/tracef "dir: %s" dir)
         (log/tracef "query so far: %s" *query*)
         (case dir
           :end-query (end dir)
           :end-session (end dir)
           :continue (log/tracef "continue...")
           :default (throw (IllegalStateException.
                            (str "unknown directive: " dir)))))))
   (swap! lines-left dec)))
      {:query (str/trim (.toString *query*))
       :directive @directive})))

(defn- process-query-string [query-string dir-fns]
  (binding [*std-in* (BufferedReader. (StringReader. query-string))]
    (let [query-data (read-query)]
      (log/infof "query: <%s>" (:query query-data))
      (if-let [dir-fn (get dir-fns (:directive query-data))]
        (dir-fn query-data)
        (throw (IllegalArgumentException.
                (str "no mapping for command"
                     (:command query-data))))))))

(process-query-string
 "select id
    from annotation an, annotator ar
    where an.annotator_id = ar.id
go"
 {:end-query (fn [{query :query}] (println "query: " query))
  :end-session (fn [_] (println "end session"))})


;(set-config :line-terminator ";")

(defn start []
  (log/info "staring loop")
  (binding [*std-in* (BufferedReader. (InputStreamReader. System/in))]
    ))
