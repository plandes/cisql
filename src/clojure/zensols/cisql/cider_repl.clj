(ns zensols.cisql.cider-repl
  (:require [nrepl.server :as nrepl-server]
            [cider.nrepl :refer (cider-nrepl-handler)]))

(defn run-server
  [port]
  (nrepl-server/start-server :port port :handler cider-nrepl-handler))

(defn repl-port-set-option
  [short long port]
  [short long "the port bind for the repl server"
   :required "<number>"
   :default port
   :parse-fn #(Integer/parseInt %)
   :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]])

(def repl-command
  "Command to start an Emacs Cider handled REPL."
  {:description "start a repl either on the command line or headless with -h"
   :options
   [(repl-port-set-option "-c" "--cider" 12345)]
   :app (fn [{:keys [port]} & args]
          (run-server port))})
;(nrepl-server/start-server :port 32345 :handler cider-nrepl-handler)

