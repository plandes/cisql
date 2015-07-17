(ns com.zensol.cisql.process-query
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:import (java.io BufferedReader InputStreamReader StringReader))
  (:require [com.zensol.cisql.conf :as conf]
            [com.zensol.cisql.db-access :as db]))

(def ^:dynamic *std-in* nil)

(def ^{:private true
       :dynamic true}
  *query* nil)

(def ^:private config-variable-pattern
  #"^\s*cf\s+([^\s]+)\s(.+?)$")

(def ^:private show-variable-pattern
  #"^\s*sh\s*([^ ]+)?\s*$")

(def ^:private toggle-variable-pattern
  #"^\s*tg\s+([^\s]+)\s*$")

(defmacro with-query [& body]
  `(.append *query* (with-out-str ~@body)))

(defn- add-line [line]
  (letfn [(config-setting []
            (let [[_ key val] (re-find config-variable-pattern line)]
              (if key [(keyword key) val])))
          (show-setting []
            (let [[_ key] (re-find show-variable-pattern line)]
              (if key (keyword key)
                  (if _ 'show))))
          (toggle-setting []
            (let [[_ key] (re-find toggle-variable-pattern line)]
              (keyword key)))
          (has-end-tok [key entire-line?]
            (let [conform (str/lower-case (str/trim line))
                  val (conf/config key)]
              (if entire-line?
                (= conform val)
                (.endsWith conform val))))
          (find-keyword [key]
            (str/trim
             (subs line 0 (- (count line)
                             (count (conf/config key))))))]
    (cond (has-end-tok :line-terminator false)
          (do (with-query
                (print (find-keyword :line-terminator)))
              {:dir :end-query})

          (has-end-tok :end-directive true)
          (do (with-query
                (print (find-keyword :end-directive)))
              {:dir :end-session})

          (has-end-tok :help-directive true)
          (do (with-query
                (print (find-keyword :help-directive)))
              {:dir :help})
          
          (config-setting)
          {:dir :setting
           :settings (config-setting)}

          (show-setting)
          {:dir :show
           :key (show-setting)}

          (toggle-setting)
          {:dir :toggle
           :key (toggle-setting)}

          :default
          (do (with-query
                (println line))
              {:dir :continue}))))

(defn- read-query [prompt-fn set-var-fn show-var-fn toggle-var-fn]
  (binding [*query* (StringBuilder.)]
    (let [lines-left (atom 60)
          directive (atom nil)]
      (letfn [(end [dir]
                (log/debugf "query: %s" *query*)
                (reset! lines-left 0)
                (reset! directive dir))]
        (while (> @lines-left 0)
          (log/tracef "lines left: %d" @lines-left)
          (prompt-fn false)
          (let [user-input (.readLine *std-in*)]
            (log/debugf "line: %s" user-input)
            (if-not user-input
              (do
                (log/debugf "read EOF from input")
                (end :end-file))
              (let [{dir :dir :as ui} (add-line user-input)]
                (log/tracef "dir: %s" dir)
                (log/tracef "query so far: %s" *query*)
                (if-not (= :continue dir)
                  (prompt-fn true))
                (case dir
                  :help (conf/print-help true)
                  :end-query (end dir)
                  :end-session (end dir)
                  :setting (set-var-fn (:settings ui))
                  :show (show-var-fn (:key ui))
                  :toggle (toggle-var-fn (:key ui))
                  :continue (log/tracef "continue...")
                  :default (throw (IllegalStateException.
                                   (str "unknown directive: " dir)))))))
          (swap! lines-left dec)))
      {:query (str/trim (.toString *query*))
       :directive @directive})))

(defn- gen-prompt-fn []
  (let [line-no (atom 0)]
    (fn [reset]
      (if reset
        (reset! line-no 0)
        (do
          (print (format (conf/config :prompt) (swap! line-no inc)))
          (flush))))))

(defn process-queries [dir-fns]
  (let [prompt-fn (or (get dir-fns :prompt-for-input)
                      (gen-prompt-fn))]
    (letfn [(set-var [[key newval]]
              (let [oldval (conf/config key)]
                (conf/set-config key newval)
                (println (format "%s: %s -> %s" (name key) oldval newval))))
            (show [key]
              (if (= key 'show)
                (conf/print-key-values)
                (let [val (conf/config key)]
                  (println (format "%s: %s" (name key) val))
                  ;(println (format "Error: no such variable: %s" (name key)))
                  )))
            (toggle [key]
              (let [oldval (conf/config key)
                    nextval (not oldval)]
                (conf/set-config key nextval)
                (println (format "%s: %s -> %s"
                                 (name key) oldval nextval))))]
      (loop [query-data (read-query prompt-fn set-var show toggle)]
        (log/tracef "query data: %s" query-data)
        (log/debugf "query: <%s>" (:query query-data))
        (if-let [dir-fn (get dir-fns (:directive query-data))]
          (try
            (dir-fn query-data)
            (prompt-fn true)
            (catch Exception e
              (let [handler (:exception-handler dir-fns)]
                (if handler
                  (handler e)
                  (log/error e "Error: " (.toString e))))))
          (throw (IllegalArgumentException.
                  (str "no mapping for directive: "
                       (:directive query-data)))))
        (when (= :end-query (:directive query-data))
          (recur (read-query prompt-fn set-var show toggle)))))))

(defn process-query-string [query-string dir-fns]
  (binding [*std-in* (BufferedReader. (StringReader. query-string))]
    (process-queries dir-fns)))
