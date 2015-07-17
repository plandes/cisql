(ns com.zensol.cisql.conf)

(def ^:private config-data
  (atom {:line-terminator "go"
         :end-directive "end"
         :error-long-format false}))

(defn set-config [key val]
  (swap! config-data assoc key val))

(defn config [key]
  (get @config-data key))
