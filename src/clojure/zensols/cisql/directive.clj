(ns ^{:doc "Default installed directives."
      :author "Paul Landes"}
    zensols.cisql.directive
  (:require [clojure.tools.logging :as log]
            [clojure.string :as s]
            [zensols.actioncli.log4j2 :as lu]
            [zensols.actioncli.parse :as parse]
            [zensols.cisql.conf :as conf]
            [zensols.cisql.read :as r]
            [zensols.cisql.spec :as spec]
            [zensols.cisql.db-access :as db]
            [zensols.cisql.export :as ex]
            [zensols.cisql.pref :as pref]
            [zensols.cisql.cider-repl :as repl]))

(declare directives)

(defn- assert-no-query
  "Throw an exception if there's a current query that exists."
  [{:keys [query directive]}]
  (if query
    (-> (format "unexpected query: %s" query)
        (ex-info {:query query
                  :directive directive})
                throw)))

(defn- exec-command-line-directive
  "Execute a general command line directive from the general definition creator
  `command-line-directive`."
  [name desc ns-sym func-sym usage {:keys [directive] :as opts} args]
  (log/debugf "%s: opts: %s, args: <%s>" name opts args)
  (binding [parse/*include-program-in-errors* false]
    (let [ctx (-> (list ns-sym func-sym)
                  parse/single-action-context)]
      (if (= "help" (first args))
        (binding [parse/*parse-context* 
                  {:action-context ctx
                   :actions (parse/create-actions ctx)
                   :single-action-mode? true}]
          (->> (if usage (str " " usage) "")
               (format "usage: %s%s" name)
               println)
          (println (parse/help-message :usage false)))
        (let [res (parse/process-arguments ctx args)]
          (assert-no-query opts)
          (if res
            (println "configured" (:connection-uri res))))))))

(defn- command-line-directive
  ([name desc ns-sym func-sym]
   (command-line-directive name desc ns-sym func-sym "<help|[options]>"))
  ([name desc ns-sym func-sym usage]
   {:name name
    :arg-count "*"
    :usage usage
    :desc desc
    :fn (fn [{:keys [query directive] :as opts} args]
          (exec-command-line-directive name desc ns-sym
                                       func-sym usage opts args))}))

(defn- print-command-help []
  (let [decls (->> (directives)
                   (map (fn [{:keys [name usage desc]}]
                          (when desc
                            {:decl (str name " " usage)
                             :desc desc})))
                   (remove nil?))
        space (->> decls (map #(-> % :decl count)) (reduce max) (max 0) (+ 2))]
    (->> decls
         (map (fn [{:keys [decl desc]}]
                (let [fmt (str "%-" space "s %s")]
                  (println (format fmt decl desc)))))
         doall)))

(defn- print-help []
  (println "commands:")
  (print-command-help)
  (println)
  (println "variables:")
  (conf/print-key-desc))

(defn- grammer []
  (->> (directives)
       (map #(select-keys % [:name :arg-count]))))

(defn init-grammer []
  (r/set-grammer (conf/config :linesep) (grammer)))

(defn directives-by-name []
  (let [dirs (directives)]
   (zipmap (map :name (directives))
           (map #(dissoc % :name) dirs))))

(defn- set-log-level [key value]
  (when (= key :loglevel)
    (if (nil? value)
      (log/error "can't set level: no log level given")
      (do
        (lu/change-log-level value)
        (log/info (format "set log level to %s" value))))))

(conf/add-set-config-hook set-log-level)

(defn- directives []
  [{:name "help"
    :arg-count 0
    :fn (fn [& _]
          (print-help))}
   (command-line-directive "conn" "connect to a database (try 'help')"
                           'zensols.cisql.interactive 'interactive-directive
                           "<help|driver [options]>")
   (command-line-directive "newdrv" "add a JDBC driver (try 'help')"
                           'zensols.cisql.spec 'driver-add-command)
   {:name "removedrv"
    :arg-count 1
    :usage "<driver>"
    :desc "remove a JDBC driver"
    :fn (fn [opts [driver-name]]
          (assert-no-query opts)
          (spec/remove-meta driver-name))}
   (command-line-directive "purgedrv" "purge custom JDBC driver configuration"
                           'zensols.cisql.spec 'driver-user-registry-purge-command
                           nil)
   {:name "listdrv"
    :arg-count 0
    :desc "list all registered JDBC drivers"
    :fn (fn [opts args]
          (assert-no-query opts)
          (spec/print-drivers))}
   (command-line-directive "repl" "start a REPL"
                           'zensols.cisql.cider-repl 'repl-command)
   {:name "sh"
    :arg-count ".."
    :usage "[variable]"
    :desc "show 'variable', or show them all if not given"
    :fn (fn [opts args]
          (assert-no-query opts)
          (let [vkey (and (seq? args) (first args))]
            (if vkey
              (->> (conf/config (keyword vkey))
                   (format "%s: %s" vkey)
                   println)
              (conf/print-key-values))))}
   {:name "rm"
    :arg-count 1
    :usage "<variable>"
    :desc "delete user variable"
    :fn (fn [opts [name]]
          (assert-no-query opts)
          (conf/remove-config (keyword name)))}
   {:name "set"
    :arg-count "*"
    :usage "<variable> [value]"
    :desc "set a variable, if 'value' is not given, then take it from the previous query input"
    :fn (fn [{:keys [query] :as opts} args]
          (if (= 0 (count args))
            (throw (ex-info "missing variable to set (try 'help')"
                            {:query query :opts opts})))
          (let [key (keyword (first args))
                oldval (conf/config key)
                newval (if (> (count args) 1)
                         (s/join " " (rest args))
                         query)]
            (conf/set-config key newval)
            ;; end of query terminator has changed so reinitialize grammer
            (if (= :linesep key)
              (init-grammer))
            (println (format "%s: %s -> %s" (name key) oldval newval))))}
   {:name "resetvar"
    :arg-count 0
    :desc "Reset all variables to their nascient state"
    :fn (fn [opts [driver-name]]
          (assert-no-query opts)
          (conf/reset))}
   {:name "tg"
    :arg-count 1
    :usage "<variable>"
    :desc "toggle a boolean variable"
    :fn (fn [opts [key-name]]
          (assert-no-query opts)
          (let [key (keyword key-name)
                oldval (conf/config key)
                nextval (not oldval)]
            (conf/set-config key nextval)
            (println (format "%s: %s -> %s"
                             key-name oldval nextval))))}
   {:name "shtab"
    :arg-count ".."
    :usage "[table]"
    :desc "show table metdata or all if no table given"
    :fn (fn [opts args]
          (assert-no-query opts)
          (let [table (and (seq? args) (first args))]
            (db/show-table-metadata table)))}
   {:name "vaporize"
    :arg-count 0
    :desc "reset all configuration including drivers (careful!)"
    :fn (fn [opts args]
          (assert-no-query opts)
          (let [table (and (seq? args) (first args))]
            (pref/clear)
            (conf/reset)))}
   {:name "orph"
    :arg-count ".."
    :usage "[label]"
    :desc "orphan the current frame optionally giving it a label"
    :fn (fn [opts args]
          (assert-no-query opts)
          (let [label (and (seq? args) (first args))]
            (db/orphan-frame label)))}
   {:name "cat"
    :arg-count 1
    :usage "<catalog>"
    :desc "set the schema/catalog of the database"
    :fn (fn [opts [cat-name]]
          (assert-no-query opts)
          (db/set-catalog cat-name))}
   {:name "export"
    :arg-count 1
    :usage "<csv file>"
    :desc "export the the query on the previous line (no delimiter ';')) to a CSV file"
    :fn (fn [{:keys [query last-query]} [csv-name]]
          (ex/export-query-to-csv query last-query csv-name))}
   {:name "load"
    :arg-count "*"
    :usage "[clojure file] [function-name]"
    :desc "evaluate a query with a function (defaults to last) a clojure file (default to cisql.clj)"
    :fn (fn [{:keys [query last-query]} args]
          (let [clj-file (if (> (count args) 0)
                           (first args)
                           "cisql.clj")
                fn-name (if (> (count args) 1)
                            (second args))]
            (if (> (count args) 2)
              (throw (ex-info "invalid load syntax; try 'help'" {})))
           (ex/export-query-to-function query last-query clj-file fn-name)))}
   {:name "eval"
    :arg-count "*"
    :usage "<clojure code>"
    :desc "evaluate a query with clojure code"
    :fn (fn [{:keys [query last-query]} code]
          (ex/export-query-to-eval query last-query
                                   (s/join " " code)))}])
