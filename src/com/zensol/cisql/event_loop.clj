(ns com.zensol.cisql.event-loop
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:use [clojure.pprint :only (pprint print-table)])
  (:require [clojure.java.jdbc :as jdbc])
  (:import (java.io BufferedReader InputStreamReader StringReader))
  (:import (java.sql SQLException))
  (:require [com.zensol.cisql.conf :as c]))

(def ^{:private true
       :dynamic true} *std-in* nil)

(def ^{:private true
       :dynamic true}
  *query* nil)

(defmacro with-query [& body]
  `(.append *query* (with-out-str ~@body)))

(defn- add-line [line]
  (letfn [(has-eol [key]
            (let [conform (str/lower-case (str/trim line))]
              (.endsWith conform (c/config key))))
          (find-keyword [key]
            (str/trim
             (subs line 0 (- (count line)
                             (count (c/config key))))))]
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
         (end :end-file))
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

(defn process-queries [dir-fns]
  (loop [query-data (read-query)]
    (log/tracef "query data: %s" query-data)
    (log/debugf "query: <%s>" (:query query-data))
    (when (= :end-query (:directive query-data))
      (if-let [dir-fn (get dir-fns (:directive query-data))]
        (dir-fn query-data)
        (throw (IllegalArgumentException.
                (str "no mapping for directive: "
                     (:directive query-data)))))
      (recur (read-query)))))

(defn process-query-string [query-string dir-fns]
  (binding [*std-in* (BufferedReader. (StringReader. query-string))]
    (process-queries dir-fns)))

(defn start []
  (log/info "staring loop")
  (binding [*std-in* (BufferedReader. (InputStreamReader. System/in))]))
