(ns ^{:doc "Default installed directives.

See README.md for more information on directives."
      :author "Paul Landes"}
    zensols.cisql.directive
  (:require [clojure.tools.logging :as log]
            [clojure.string :as s]
            [clojure.java.browse :as browse]
            [zensols.actioncli.log4j2 :as lu]
            [zensols.actioncli.parse :as parse]
            [zensols.cisql.version :as ver]
            [zensols.cisql.conf :as conf]
            [zensols.cisql.read :as r]
            [zensols.cisql.spec :as spec]
            [zensols.cisql.db-access :as db]
            [zensols.cisql.export :as ex]
            [zensols.cisql.pref :as pref]
            [zensols.cisql.cider-repl :as repl]))

(declare directives)

(def ^:private man-url-format
  "The README.md GitHub URL to browse for the `man` directive command."
  "https://github.com/plandes/cisql#%s")

(defn directives-by-name
  "Return a map of the directives using their names as keys.

See README.md for more information on directives."
  []
  (let [dirs (directives)]
   (zipmap (map :name (directives))
           (map #(dissoc % :name) dirs))))

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
        (do
          (assert-no-query opts)
          (let [res (parse/process-arguments ctx args)]
            (if res
              (println "configured" (:connection-uri res)))))))))

(defn- command-line-directive
  ([name desc ns-sym func-sym]
   (command-line-directive name desc ns-sym func-sym nil nil))
  ([name desc ns-sym func-sym usage help-section]
   {:name name
    :arg-count "*"
    :usage (or usage "<help|[options]>")
    :desc desc
    :help-section help-section
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
        space (->> decls
                   (map #(-> % :decl count))
                   (reduce max)
                   (max 0)
                   inc)]
    (->> decls
         (map (fn [{:keys [decl desc]}]
                (let [fmt (str " * %-" space "s %s")]
                  (println (format fmt decl desc)))))
         doall)
    space))

(defn init-grammer
  ([]
   (init-grammer (conf/config :linesep)))
  ([val]
   (r/set-grammer val
                  (->> (directives)
                       (map #(select-keys % [:name :arg-count]))))))

(defn- set-log-level [key value]
  (when (= key :loglevel)
    (if (nil? value)
      (log/error "can't set level: no log level given")
      (do
        (lu/change-log-level value)
        (log/info (format "set log level to %s" value))))))

(conf/add-set-config-hook set-log-level)

(defn- update-linesep-variable [key val]
  ;; end of query terminator has changed so reinitialize grammer
  (if (= :linesep key)
    (init-grammer val)))

(conf/add-set-config-hook update-linesep-variable)

(defn- directives []
  [{:name "help"
    :arg-count 0
    :fn (fn [& _]
          (println "# Commands:")
          (let [space (print-command-help)]
            (println)
            (println "# Variables:")
            (conf/print-key-desc space)))}
   (command-line-directive "conn" "connect to a database"
                           'zensols.cisql.interactive 'interactive-directive
                           nil "connecting-to-a-database")
   {:name "shconn"
    :arg-count 0
    :desc "print the current connection information"
    :help-section "queries-and-directives"
    :fn (fn [opts _]
          (assert-no-query opts)
          (db/assert-connection)
          (println "# Connection")
          (->> (db/dbspec-meta-data)
               (map #(println (format "* %s: %s" (name (first %)) (second %))))
               doall))}
   (command-line-directive "newdrv" "add a JDBC driver"
                           'zensols.cisql.spec 'driver-add-command
                           nil "connecting-to-a-database")
   {:name "removedrv"
    :arg-count 1
    :usage "<driver>"
    :desc "remove a JDBC driver"
    :help-section "removing-jdbc-drivers"
    :fn (fn [opts [driver-name]]
          (assert-no-query opts)
          (spec/remove-meta driver-name))}
   {:name "listdrv"
    :arg-count 0
    :desc "list all registered JDBC drivers"
    :fn (fn [opts args]
          (assert-no-query opts)
          (spec/print-drivers))}
   (command-line-directive "purgedrv" "purge custom JDBC driver configuration"
                           'zensols.cisql.spec
                           'driver-user-registry-purge-command
                           nil "connecting-to-a-database")
   {:name "sh"
    :arg-count ".."
    :usage "[variable]"
    :desc "show 'variable', or show them all if not given"
    :help-section "variables"
    :fn (fn [opts args]
          (assert-no-query opts)
          (let [vkey (and (seq? args) (first args))]
            (if vkey
              (->> (conf/config (keyword vkey) :expect? true)
                   (format "%s: %s" vkey)
                   println)
              (conf/print-key-values))))}
   {:name "rm"
    :arg-count 1
    :usage "<variable>"
    :desc "delete user variable"
    :help-section "variables"
    :fn (fn [opts [name]]
          (assert-no-query opts)
          (conf/remove-config (keyword name)))}
   {:name "set"
    :arg-count "*"
    :usage "<variable> [value]"
    :desc "set a variable to 'value' or previous query input"
    :help-section "variables"
    :fn (fn [{:keys [query] :as opts} args]
          (if (= 0 (count args))
            (-> "missing variable to set (try 'help')"
                (ex-info {:query query :opts opts})
                throw))
          (let [key (keyword (first args))
                oldval (conf/config key)
                newval (->> (if (> (count args) 1)
                              (s/join " " (rest args))
                              query)
                            (#(if (contains? #{"true" "false"} %)
                                (Boolean/parseBoolean %)
                                %)))]
            (conf/set-config key newval)
            (println (format "%s: %s -> %s" (name key) oldval newval))))}
   {:name "tg"
    :arg-count 1
    :usage "<variable>"
    :desc "toggle a boolean variable"
    :help-section "variables"
    :fn (fn [opts [key-name]]
          (assert-no-query opts)
          (let [key (keyword key-name)
                oldval (conf/config key)
                nextval (not oldval)]
            (conf/set-config key nextval)
            (println (format "%s: %s -> %s"
                             key-name oldval nextval))))}
   {:name "resetvar"
    :arg-count 0
    :desc "Reset all variables to their nascient state"
    :help-section "variables"
    :fn (fn [opts [driver-name]]
          (assert-no-query opts)
          (conf/reset))}
   {:name "shtab"
    :arg-count ".."
    :usage "[table]"
    :desc "show table metdata or all if no table given"
    :help-section "variables"
    :fn (fn [opts args]
          (assert-no-query opts)
          (let [table (and (seq? args) (first args))]
            (db/show-table-metadata table)))}
   {:name "cat"
    :arg-count 1
    :usage "<catalog>"
    :desc "set the schema/catalog of the database"
    :fn (fn [opts [cat-name]]
          (assert-no-query opts)
          (db/set-catalog cat-name))}
   {:name "vaporize"
    :arg-count 0
    :desc "reset all configuration including drivers"
    :help-section "bad-state"
    :fn (fn [opts args]
          (assert-no-query opts)
          (let [table (and (seq? args) (first args))]
            (pref/clear)
            (conf/reset)))}
   {:name "orph"
    :arg-count ".."
    :usage "[title]"
    :desc "orphan the current frame with optional title"
    :help-section "graphical-results"
    :fn (fn [opts args]
          (assert-no-query opts)
          (let [label (and (seq? args) (first args))]
            (db/orphan-frame label)))}
   (command-line-directive "repl" "start a REPL"
                           'zensols.cisql.cider-repl 'repl-command
                           nil "emacs-integration")
   {:name "export"
    :arg-count 1
    :usage "<csv file>"
    :desc "export the query to a CSV file"
    :help-section "queries-and-directives"
    :fn (fn [{:keys [query last-query]} [csv-name]]
          (ex/export-query-to-csv query last-query csv-name))}
   {:name "do"
    :arg-count "*"
    :usage "<variable 1> [variable 2]"
    :desc "execute the contents of a variables"
    :help-section "macros"
    :fn (fn [_ varnames]
          (->> varnames
               (map #(conf/config (keyword %) :expect? true))
               (array-map :eval )))}
   {:name "clear"
    :arg-count 0
    :desc "clears any query"
    :help-section "querying-the-database"
    :fn (fn [& args]
          ;; last query will clear after the next event loop cycle
          )}
   {:name "load"
    :arg-count "*"
    :usage "[file] [function]"
    :desc "evaluate a query with a Clojure file"
    :help-section "evaluation"
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
    :help-section "evaluation"
    :fn (fn [{:keys [query last-query]} code]
          (ex/export-query-to-eval query last-query
                                   (s/join " " code)))}
   {:name "man"
    :arg-count 1
    :usage "<directive|variable name>"
    :desc "browse the program documentation"
    :help-section "documentation"
    :fn (fn [opts [name]]
          (assert-no-query opts)
          (let [directive (get (directives-by-name) name)
                section (or (:help-section directive)
                            (conf/help-section (keyword name)))
                url (and section (format man-url-format section))]
            (if url
              (do
                (log/infof "browsing documentation at %s" url)
                (browse/browse-url url))
              (println (format "no documentation available for %s" name)))))}
   {:name "ver"
    :arg-count 0
    :desc "print the version of this program"
    :fn (fn [& _]
          (println (format "v%s (%s)" ver/version ver/gitref)))}])
