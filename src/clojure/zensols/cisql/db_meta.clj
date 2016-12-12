(ns zensols.cisql.db-meta)

(def products ["mysql" "postgresql" "sqlite"])

(defn map-subproto [name host port-or-nil database]
  (let [portn (or port-or-nil
                  (if name
                    (case name
                      "mysql" 3306
                      "postgresql" 5432
                      nil)))
        port (if portn (format ":%d" portn) "")]
    (case name
      "sqlite" "sqlite"
      (format "//%s%s/%s" host port database))))
