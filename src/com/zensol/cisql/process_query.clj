(ns com.zensol.cisql.process-query
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:import (java.io BufferedReader InputStreamReader StringReader))
  (:require [com.zensol.cisql.conf :as c]
            [com.zensol.cisql.db-access :as db]))

(def ^:dynamic *std-in* nil)

(def ^{:private true
       :dynamic true}
  *query* nil)

(def ^:private config-variable-pattern
  #"^\s*cnf\s+([^\s]+)\s+(.+?)\s*$")

(defmacro with-query [& body]
  `(.append *query* (with-out-str ~@body)))

(defn- add-line [line]
  (letfn [(has-config-setting []
            (let [group (re-find config-variable-pattern line)
                  key (keyword (nth group 1))
                  val (nth group 2)]
              (if key [key val])))
          (has-end-tok [key entire-line?]
            (let [conform (str/lower-case (str/trim line))
                  val (c/config key)]
              (if entire-line?
                (= conform val)
                (.endsWith conform val))))
          (find-keyword [key]
            (str/trim
             (subs line 0 (- (count line)
                             (count (c/config key))))))]
    (cond (has-end-tok :line-terminator false)
          (do (with-query
                (print (find-keyword :line-terminator)))
              {:dir :end-query})

          (has-end-tok :end-directive true)
          (do (with-query
                (print (find-keyword :end-directive)))
              {:dir :end-session})
          
          (has-config-setting)
          {:dir :setting
           :settings (has-config-setting)}

          :default
          (do (with-query
                (println line))
              {:dir :continue}))))

(defn- read-query []
  (binding [*query* (StringBuilder.)]
    (let [lines-left (atom 60)
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
              (let [{dir :dir :as ui} (add-line user-input)]
                (log/tracef "dir: %s" dir)
                (log/tracef "query so far: %s" *query*)
                (case dir
                  :end-query (end dir)
                  :end-session (end dir)
                  :setting (let [[key val] (:settings ui)]
                             (c/set-config key val))
                  :continue (log/tracef "continue...")
                  :default (throw (IllegalStateException.
                                   (str "unknown directive: " dir)))))))
          (swap! lines-left dec)))
      {:query (str/trim (.toString *query*))
       :directive @directive})))

(defn process-queries [dir-fns]
  (let [prompt-fn (get dir-fns :prompt-for-input)]
    (prompt-fn)
    (loop [query-data (read-query)]
      (log/tracef "query data: %s" query-data)
      (log/debugf "query: <%s>" (:query query-data))
      (if-let [dir-fn (get dir-fns (:directive query-data))]
        (try
          (dir-fn query-data)
          (catch Exception e
            (let [handler (:exception-handler dir-fns)]
              (if handler
                (handler e)
                (log/error e "Error: " (.toString e))))))
        (throw (IllegalArgumentException.
                (str "no mapping for directive: "
                     (:directive query-data)))))
      (when (= :end-query (:directive query-data))
        (prompt-fn)
        (recur (read-query))))))

(defn process-query-string [query-string dir-fns]
  (binding [*std-in* (BufferedReader. (StringReader. query-string))]
    (process-queries dir-fns)))
