(ns com.zensol.cisql.event-loop
  (:require [clojure.tools.logging :as log])
  (:import (java.io BufferedReader InputStreamReader)))

(defn start []
  (log/info "staring loop"))

(start)
