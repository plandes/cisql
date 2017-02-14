(ns ^{:doc "This package manages DB specs.  It also class loads and optionally
downloads the JDBC drivers."
      :author "Paul Landes"}
    zensols.cisql.spec
  (:import [java.sql DriverManager]
           [java.util Properties])
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.string :as s]
            [clojure.set :refer (rename-keys)]
            [clojure.java.jdbc :as jdbc]
            [clojure.data.csv :as csv])
  (:require [cemerick.pomegranate :refer (add-dependencies)])
  (:require [zensols.actioncli.dynamic :refer (defa-) :as dyn]
            [zensols.actioncli.parse :refer (with-exception) :as parse]
            [zensols.actioncli.log4j2 :as lu])
  (:require [zensols.cisql.pref :as pref]
            [zensols.cisql.conf :as conf]))

(defa- driver-meta-inst)
(defa- driver-pref-meta-inst)

(defn reset []
  (reset! driver-meta-inst nil)
  (reset! driver-pref-meta-inst nil))

(dyn/register-purge-fn reset)

(defn- rows-to-maps
  [rows]
  (let [header (->> rows first (map keyword))]
    (->> (rest rows)
         (map #(zipmap header %)))))

(defn- flat-to-map [flat]
  {(:name flat) (dissoc flat :name)})

(defn- resource-driver-meta
  "Parse the CSV resource JDBC driver registry."
  []
  (with-open [reader (io/reader (io/resource "driver.csv"))]
    (->> (csv/read-csv reader)
         rows-to-maps
         (map (fn [mrow]
                {(:name mrow) (dissoc mrow :name)}))
         (apply merge))))

(defn- create-driver-meta
  "Create the driver metadata by parsing the driver information CSV."
  []
  (->> (pref/driver-metas)
       (remove nil?)
       (concat (list (resource-driver-meta)))
       (apply merge)))

(defn- driver-meta
  "Return the driver meta data.  If **name** is given, only return the metadata
  for that DB name, otherwise return all as a map."
  ([name]
   (or (get (driver-meta) name)
       (throw (ex-info (format "No such driver: %s" name)
                       {:name name}))))
  ([]
   (swap! driver-meta-inst #(or % (create-driver-meta)))))

(defn- format-dependency [{:keys [artifact group version]}]
  (format "[[%s/%s \"%s\"]]" group artifact version))

(defn- load-dependencies
  "Download and class load a JDBC driver."
  [meta]
  (let [dep (format-dependency meta)]
    (log/infof "loading dependencies for %s" dep)
    (->> (read-string dep)
         (add-dependencies :coordinates))))

(defn- create-db-spec
  "Create a Clojure JDBC database spec (`db-spec`)."
  [{:keys [user password port host database] :as conn}
   {:keys [url default-port class] :as meta}]
  (let [host (or host "localhost")
        port (or port default-port)
        url (format url user password host port database)]
    (merge (select-keys conn [:user :password])
           {:connection-uri url
            :class class})))

(defn- ^Properties as-properties [m]
  (let [p (Properties.)]
    (doseq [[k v] m]
      (.setProperty p (jdbc/as-sql-name identity k)
                    (if (instance? clojure.lang.Named v)
                      (jdbc/as-sql-name identity v)
                      (str v))))
    p))

(defn db-spec
  "Create a database spec used by the Clojure JDBC API.  If the JDBC driver for
  the spec doesn't exist it is downloaded and then class loaded.

  The parameter **conn** is a map with string values and the following keys:

  * **:name** name of the DB/JDBC connection implementation (ex: mysql)
  * **:user** login user
  * **:password** login passowrd
  * **:host** name of server hosting the DB
  * **:database** the database name"
  [conn]
  (let [meta (driver-meta (:name conn))]
    (log/debugf "spec meta: %s" (pr-str meta))
    (load-dependencies meta)
    (Class/forName (:class meta))
    (let [spec (create-db-spec conn meta)
          props (as-properties (dissoc spec :connection-url))
          factory #(DriverManager/getConnection (:connection-uri %) props)
          spec (-> spec
                   (rename-keys {:class :classname})
                   (assoc :factory factory))]
      (log/debugf "spec: %s" (pr-str spec))
      spec)))

(defn registered-names
  "Return a sequence of provided drivers.  These are the default driver
  information (not actual JDBC drivers)."
  []
  (->> (driver-meta) keys))

(defn- opts-to-flat [{:keys [dependency] :as opts}]
  (let [manditory-keys [:class :url :name :dependency]
        optional-keys [:port]
        [group artifact version] (if dependency
                                   (s/split dependency #"/"))]
    (->> manditory-keys
         (map (fn [okey]
                (if-not (contains? opts okey)
                  (throw (ex-info (format "Missing --%s option" (-> okey name))
                                  {:opts opts
                                   :key okey})))
                okey))
         (concat optional-keys)
         (select-keys opts)
         (#(rename-keys % {:port :default-port}))
         (#(assoc % :group group :artifact artifact :version version)))))

(defn- put-flat [flat]
  (log/debugf "adding <%s>" (pr-str flat))
  (-> (pref/driver-metas)
      (merge (flat-to-map flat))
      pref/set-driver-metas)
  (log/infof "added driver: %s" (:name flat)))

(defn name-option
  "Create a usage option for the database name."
  [validate?]
  (concat ["-n" "--name" "JDBC driver name (ex: mysql)"
           :required "<name>"]
          (if validate?
            [:validate [#(contains? (set (registered-names)) %)
                        (str "Must be one of: "
                             (s/join ", " (registered-names)))]])))

(defn remove-meta [driver-name]
  (let [metas (driver-meta)]
    (if-not (contains? metas driver-name)
      (-> (format "No such driver: %s" driver-name)
          (ex-info {:driver-name driver-name})
          throw))
    (-> metas
        (dissoc driver-name)
        pref/set-driver-metas)
    (reset)
    (log/infof "removed driver: %s" driver-name)))

(defn- driver-describe []
  (->> (driver-meta)
       (into (sorted-map))
       (map (fn [[name meta]]
              (let [fmt "  - %s: %s"]
                (->> [:url :class]
                     (map #(format fmt
                                   (clojure.core/name %)
                                   (or (get meta %) "*none*")))
                     (#(concat % (->> (format-dependency meta)
                                      read-string first pr-str
                                      (format fmt "dependency")
                                      list)))
                     (cons (format "* %s:" name))
                     (s/join \newline)))))
       (s/join (str \newline \newline))))

(defn print-drivers []
  (with-exception
    (println "# Driver Registry")
    (println (driver-describe))))

(def driver-describe-command
  "CLI command to list drivers currently installed."
  {:description "List drivers currently installed"
   :options []
   :app (fn [opts & args]
          (print-drivers))})

(def driver-add-command
  "CLI command to install a JDBC driver."
  {:description "Install a driver to the user's local JDBC registry"
   :options
   [(name-option false)
    ["-u" "--url" (str "the URL pattern defaults to "
                       "jdbc:<name>://3$s:%4$s/%5$s"
                       " (substitued with user, password, host, port, database)")
     :required "<string>"
     :default "jdbc:<name>://3$s:%4$s/%5$s"]
    ["-p" "--port" "the default bound database port (ex: 3306)"
     :required "<number>"
     :parse-fn read-string
     :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
    ["-c" "--class" "JDBC driver class (ex: org.sqlite.JDBC)"
     :required "<string>"]
    ["-d" "--dependency" "maven group ID coordinate element (ex: org.mysql/mysql-connector/5.1.35)"
     :required "<string>"]]
   :app (fn [{:keys [name] :as opts} & args]
          (log/infof "loading driver: %s" name)
          (binding [parse/*rethrow-error* (conf/config :prex)]
            (with-exception
              (let [flat (opts-to-flat opts)]
                (load-dependencies flat)
                (put-flat flat)
                (reset)))))})

(def driver-user-registry-purge-command
  "CLI command to install a JDBC driver."
  {:description "Purge (clear) the user's local JDBC registry"
   :options []
   :app (fn [opts & args]
          (log/info "purging user local JDBC registry")
          (binding [parse/*rethrow-error* (conf/config :prex)]
            (with-exception
              (lu/change-log-level "info")
              (pref/clear)
              (reset)
              (log/info "user's local JDBC registry purged"))))})
