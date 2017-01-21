(ns ^{:doc "This package manages DB specs.  It also class loads and optionally
downloads the JDBC drivers."
      :author "Paul Landes"}
    zensols.cisql.spec
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.data.csv :as csv])
  (:require [cemerick.pomegranate :refer (add-dependencies)])
  (:require [zensols.actioncli.dynamic :refer (defa-) :as dyn])
  (:require [zensols.cisql.pref :as pref]))

(defa- driver-meta-inst)
(defa- driver-pref-meta-inst)

(defn reset []
  (reset! driver-meta-inst nil)
  (reset! driver-pref-meta-inst nil))

(dyn/register-purge-fn reset)

(defn- default-subname
  "Create a JDBC subname using the port, host and database keys."
  [{:keys [host port database]}]
  (let [port (if port (format ":%s" port) "")]
    (format "//%s%s/%s" host port database)))

(defn- rows-to-maps
  [rows]
  (let [header (->> rows first (map keyword))]
    (->> (rest rows)
         (map #(zipmap header %)))))

(defn- row-to-meta
  [{:keys [name subname class] :as row}]
  (let [subname (if (empty? subname)
                  default-subname
                  (eval (read-string subname)))
        class (if-not (empty? class) class)]
    {name (merge (dissoc row :name)
                 {:subname subname :class class})}))

(defn- create-driver-meta
  "Create the driver metadata by parsing the driver information CSV."
  []
  (with-open [reader (io/reader (io/resource "driver.csv"))]
    (->> (csv/read-csv reader)
         rows-to-maps
         (map row-to-meta)
         (apply merge))))

(defn- driver-meta
  "Return the driver meta data.  If **name** is given, only return the metadata
  for that DB name, otherwise return all as a map."
  ([name]
   (or (get (driver-meta) name)
       (throw (ex-info (format "No such driver: %s" name)
                       {:name name}))))
  ([]
   (swap! driver-meta-inst #(or % (create-driver-meta)))))

;; (-> (driver-meta)
;;     (get "postgres")
;;     (assoc :name "postgres")
;;     (dissoc :subname)
;;     list
;;     pref/set-driver-metas)

;; (->> (pref/driver-metas)
;;      (map row-to-meta)
;;      (apply merge))

(defn- load-dependencies
  "Download and class load a JDBC driver."
  [meta]
  (let [{:keys [artifact-id group-id version]} meta]
    (log/infof "loading dependencies for %s/%s/%s"
               artifact-id group-id version)
    (->> (format "[[%s/%s \"%s\"]]" group-id artifact-id version)
         read-string
         (add-dependencies :coordinates))))

(defn- create-db-spec
  "Create a Clojure JDBC database spec (`db-spec`)."
  [conn {:keys [subname port class] :as meta}]
  (let [subname-context (assoc conn :port port)]
    (->> (subname subname-context)
         (hash-map :subname)
         (merge (select-keys meta [:subprotocol])
                (select-keys conn [:user :password])
                (if class {:class class})))))

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
    (load-dependencies meta)
    (create-db-spec conn meta)))

(defn registered-names
  "Return a sequence of provided drivers.  These are the default driver
  information (not actual JDBC drivers)."
  []
  (->> (driver-meta) keys))
