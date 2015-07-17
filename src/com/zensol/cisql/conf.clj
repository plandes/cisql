(ns com.zensol.cisql.conf)

(def ^:private config-data
  (atom {:line-terminator "go"
         :end-directive "end"
         :error-long-format false}))

(def ^:private parse-keys
  #{:error-long-format})

(defn set-config [key value]
  (let [val (if (and (contains? parse-keys key)
                     (string? value))
              (read-string value)
              value)]
    (swap! config-data assoc key val)))

(defn config [key]
  (get @config-data key))
