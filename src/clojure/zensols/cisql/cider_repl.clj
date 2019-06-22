(ns ^{:doc "Provide support for Emacs Cider and nREPL."
      :author "Paul Landes"}
    zensols.cisql.cider-repl
  (:require [clojure.tools.logging :as log]
            [nrepl.server :as nrepl-server]))

(def default-port 32345)

(defn nrepl-handler
  "Enables application level (even in uber jars) to handle Cider requests."
  []
  (require 'cider.nrepl)
  (ns-resolve 'cider.nrepl 'cider-nrepl-handler))

(defn run-server
  [port & {:keys [type] :or {type 'cider}}]
  (if (= type 'cider)
    (do
      (log/info "using cider")
      (nrepl-server/start-server :port port :handler (nrepl-handler)))
    (do
      (log/info "using nrepl (non cider)")
      (nrepl-server/start-server :port port))))

(def repl-command
  "REPL start command.."
  {:description "Start a REPL in the background"
   :options
   [["-c" "--cider" "whether or not to start a Cider or standard nREPL" 
     :required "<true|false>"
     :parse-fn #(Boolean/parseBoolean %)
     :default true]
    ["-p" "--port" "the port bind for the repl server"
     :required "<number>"
     :default default-port
     :parse-fn read-string
     :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]]
   :app (fn [{:keys [cider port]}]
          (let [type (if cider
                       'cider
                       'nrepl)]
            (log/errorf "starting REPL %s on port %d" type port)
            (run-server port :type type)
            nil))})
