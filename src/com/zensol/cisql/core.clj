(ns com.zensol.cisql.core
  (require [com.zensol.cisql.event-loop :as el])
  (:gen-class :main true))

(defn -main [& args]
  (el/start))

;(-main)
