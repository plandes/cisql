(ns com.zensols.cisql.repl
  (:require [clojure.tools.nrepl.server :as replserv])
  (:require [clojure.tools.nrepl.cmdline :as replcmd]))

(defn run-server
  ([]
   (run-server 52605))
  ([port]
   (let [fmt "nREPL server started on port %d on host 127.0.0.1 - nrepl://127.0.0.1:%d"]
     (println (format fmt port port))
     (replserv/start-server :port port))))
