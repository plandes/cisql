(ns temporary
  (:require [clojure.tools.logging :as log]
            [zensols.cisql.conf :as conf]))

(defn- process-query [rows header]
  (log/debugf "header: %s" (vec header))
  (let [col-name "coder"]
   (->> rows
        (map (fn [row]
               (assoc row col-name (str "Mrs " (get row col-name)))))
        (array-map :header header :rows)
        (array-map :display))))

(defn- table-names [rows header]
  (log/debugf "header: %s" (vec header))
  (let [col-name "coder"]
   (->> rows
        (map (fn [row]
               {"name" (get row "TABLE_NAME")}))
        (array-map :header ["name"] :rows)
        (array-map :display))))

(defn- set-table-names [rows _]
  (->> rows
       (map #(get % "TABLE_NAME"))
       vec
       (conf/set-config :table-names))
  "set table-names variable")

(defn- read-table-names [$ _]
  (format "%d tables" (count (conf/config :table-names))))
