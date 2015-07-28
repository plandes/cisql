(defproject com.zensol/cisql "0.1.0-SNAPSHOT"
  :description "Clojure based interactive SQL session"
  :plugins [[codox "0.8.12"]
            [org.clojars.cvillecsteele/lein-git-version "1.0.3"]]
  :codox {:defaults {:doc/format :markdown}
          :output-dir "target/doc/codox"}
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options ["-Xlint:unchecked"]
  :dependencies [[org.clojure/clojure "1.6.0"]

                 ;; useful for debugging
                 [org.clojure/tools.nrepl "0.2.7"]

                 ;; api
                 [org.clojure/java.jdbc "0.3.7"]

                 ;; db drivers
                 [mysql/mysql-connector-java "5.1.35"]
                 [postgresql/postgresql "9.1-901-1.jdbc4"]
                 [org.xerial/sqlite-jdbc "3.8.7"]
                 [org.clojure/data.csv "0.1.2"]

                 ;; command line
                 [org.clojure/tools.cli "0.3.1"]

                 ;; logging
                 [org.apache.logging.log4j/log4j-core "2.3"]
                 [org.apache.logging.log4j/log4j-api "2.3"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.3"]
                 [org.apache.logging.log4j/log4j-jcl "2.3"]
                 [org.clojure/tools.logging "0.3.1"]]
  :profiles {:uberjar {:aot :all}
             :dev {:jvm-opts
                   ["-Dlog4j.configurationFile=test-resources/log4j2.xml"]
                   :dependencies [[com.zensol/clojappend "1.0.2"]
                                  [cider/cider-nrepl "0.9.0"]]}}
  :main com.zensol.cisql.core)
