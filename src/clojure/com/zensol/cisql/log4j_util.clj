(ns com.zensol.cisql.log4j-util
  (:require [clojure.tools.logging :as log])
  (:import (org.apache.logging.log4j LogManager Level)))

(defn change-log-level
  "Change the Log4j2 log level.
  *level-thing* is the log level as either the Level instance or a string."
  [level-thing]
  (log/debugf "changing log level to %s" level-thing)
  (let [ctx (LogManager/getContext false)
        config (.getConfiguration ctx)
        log-config (.getLoggerConfig config LogManager/ROOT_LOGGER_NAME)
        loggers (concat (.values (.getLoggers config))
                        [log-config])
        level (if (string? level-thing)
                (Level/toLevel level-thing)
                level-thing)]
    (doseq [logger loggers]
      (.setLevel logger level))
    (.updateLoggers ctx)))
