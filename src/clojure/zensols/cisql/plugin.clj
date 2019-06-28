(ns ^{:doc "Framework for user created plugins.

Plugins allow the user to create one or more directives added to the command
line by defining a Clojure source file that adheres to a set of constraints."
      :author "Paul Landes"}
    zensols.cisql.plugin
  (:import [java.io PushbackReader])
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [cemerick.pomegranate :refer (add-dependencies)]
            [zensols.actioncli.ns :refer (with-temp-ns with-context-ns)]
            [zensols.tabres.display-results :as dis]))

(defn- read-all
  [file]
  (with-open [rdr (-> file io/file io/reader PushbackReader.)]
    (loop [forms []]
      (let [form (try (read rdr) (catch Exception e nil))]
        (if form
          (recur (conj forms form))
          forms)))))

(defn- get-forms [forms form-def clj-file expect-name]
  (let [flen (count form-def)]
    (->> forms
         (some #(and (= form-def (take flen %)) %))
         (#(if (and (nil? %) expect-name)
             (-> (format "missing '%s' function in %s"
                         expect-name clj-file)
                 (ex-info {:clj-file clj-file})
                 throw)
             %)))))

(defn- get-dependencies [forms clj-file]
  (let [var-form (get-forms forms '[def dependencies] clj-file nil)]
    (if var-form
      (with-context-ns var-form []
        (var-get (eval var-form)))
      (log/infof "no depndencies found for plugin '%s'" clj-file))))

(defn- load-plugin [clj-file]
  (let [forms (read-all clj-file)
        dep-def (first (get-dependencies forms clj-file))]
    (log/debugf "loading dependencies: %s" dep-def)
    (apply add-dependencies
           (concat [:repositories
                    (merge cemerick.pomegranate.aether/maven-central
                           {"clojars" "https://clojars.org/repo"})]
            dep-def))
    (load-file clj-file)
    (-> (get-forms forms '[ns] clj-file "ns")
        second
        (ns-resolve 'directives)
        var-get)))

(defn load-plugins [directory]
  (log/infof "scanning directory %s for plugins" directory)
  (->> (file-seq (io/file directory))
       (filter #(.isFile %))
       (map #(load-plugin (.getAbsolutePath %)))
       (apply concat)))
