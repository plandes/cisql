(defproject com.zensol/cisql "0.1.0-SNAPSHOT"
  :description "Clojure based interactive SQL session"
  :plugins [[codox "0.8.12"]
            [org.clojars.cvillecsteele/lein-git-version "1.0.3"]]
  :codox {:defaults {:doc/format :markdown}
          :output-dir "target/doc/codox"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                
                 [org.clojure/java.jdbc "0.3.7"]
                 [mysql/mysql-connector-java "5.1.35"]

                 ;; command line
                 [org.clojure/tools.cli "0.3.1"]

                 ;; logging
                 [org.apache.logging.log4j/log4j-core "2.3"]
                 [org.apache.logging.log4j/log4j-api "2.3"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.3"]
                 [org.apache.logging.log4j/log4j-jcl "2.3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.zensol/clojappend "1.0.2"]]
  :profiles {:uberjar {:aot :all}}
  :main com.zensol.cisql.core)
