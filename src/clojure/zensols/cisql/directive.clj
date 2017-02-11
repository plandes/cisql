(ns ^{:doc "Default installed directives."
      :author "Paul Landes"}
    zensols.cisql.directive
  (:require [clojure.tools.logging :as log])
  (:require [zensols.actioncli.log4j2 :as lu])
  (:require [zensols.cisql.conf :as conf]
            [zensols.cisql.read :as r]
            [zensols.cisql.db-access :as db]
            [zensols.cisql.table-export :as te]))

(declare print-help)

(declare init-grammer)

(def ^:private directives
  [{:name "help"
    :arg-count 0
    :fn (fn [& _]
          (print-help))}
   {:name "sh"
    :arg-count ".."
    :usage "[variable]"
    :desc "show 'variable', or show them all if not given"
    :fn (fn [{:keys [query]} args]
          (let [vkey (and (seq? args) (first args))]
            (log/debugf "query: <%s>, args: <%s>" query args)
            (if vkey
              (->> (conf/config (keyword vkey))
                   (format "%s: %s" vkey)
                   println)
              (conf/print-key-values))))}
   {:name "set"
    :arg-count 2
    :usage "<variable> <value>"
    :desc "set a variable"
    :fn (fn [_ [key-name newval]]
          (let [key (keyword key-name)
                oldval (conf/config key)]
            (conf/set-config key newval)
            ;; end of query terminator has changed so reinitialize grammer
            (if (= :linesep key-name)
              (init-grammer))
            (println (format "%s: %s -> %s" (name key) oldval newval))))}
   {:name "tg"
    :arg-count 1
    :usage "<variable>"
    :desc "toggle a boolean variable"
    :fn (fn [_ [key-name]]
          (let [key (keyword key-name)
                oldval (conf/config key)
                nextval (not oldval)]
            (conf/set-config key nextval)
            (println (format "%s: %s -> %s"
                             key-name oldval nextval))))}
   {:name "shtab"
    :arg-count 1
    :usage "[table]"
    :desc "show table metdata or all if no table given"
    :fn (fn [_ [table]]
          (db/show-table-metadata table))}
   {:name "orphan"
    :arg-count ".."
    :usage "[label]"
    :desc "orphan the current frame optionally giving it a label"
    :fn (fn [_ args]
          (let [table (and (seq? args) (first args))]
            (db/orphan-frame table)))}
   {:name "cat"
    :arg-count 1
    :usage "<catalog>"
    :desc "show the schema/catalog of the database"
    :fn (fn [_ [cat-name]]
          (db/set-catalog cat-name))}
   {:name "export"
    :arg-count 1
    :usage "<csv file>"
    :desc "export the queued query to a CSV file"
    :fn (fn [{:keys [query last-query]} [csv-name]]
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
            (te/export-table-csv to-export csv-name)))}])

(defn- print-command-help []
  (let [decls (->> directives
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
  (->> directives
       (map #(select-keys % [:name :arg-count]))))

(defn init-grammer []
  (r/set-grammer (conf/config :linesep) (grammer)))

(defn directives-by-name []
  (zipmap (map :name directives)
          (map #(dissoc % :name) directives)))
