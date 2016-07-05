(defproject com.zensols/cisql "0.1.0-SNAPSHOT"
  :description "Clojure based interactive SQL session"
  :license {:name "Apache License - v2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"
            :distribution :repo}
  :plugins [[lein-codox "0.9.5"]
            [org.clojars.cvillecsteele/lein-git-version "1.0.3"]]
  :codox {:metadata {:doc/format :markdown}
          :output-path "doc/codox"}
  :source-paths ["src/clojure"]
  :javac-options ["-Xlint:unchecked"]
  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; useful for debugging
                 [org.clojure/tools.nrepl "0.2.7"]

                 ;; api
                 [org.clojure/java.jdbc "0.3.7"]

                 ;; db drivers
                 [mysql/mysql-connector-java "5.1.35"]
                 [postgresql/postgresql "9.1-901-1.jdbc4"]
                 [org.xerial/sqlite-jdbc "3.8.11.2"]
                 [org.clojure/data.csv "0.1.2"]

                 ;; command line
                 [org.clojure/tools.cli "0.3.1"]

                 ;; logging
                 [org.apache.logging.log4j/log4j-core "2.3"]
                 [org.apache.logging.log4j/log4j-api "2.3"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.3"]
                 [org.apache.logging.log4j/log4j-jcl "2.3"]
                 [org.clojure/tools.logging "0.3.1"]

                 ;; gui
                 [com.zensols.gui/tabres "0.0.2"]]
  :profiles {:uberjar {:aot :all}
             :jar {:aot :all}
             :dev {:jvm-opts
                   ["-Dlog4j.configurationFile=test-resources/log4j2.xml"]
                   :dependencies [[com.zensols/clojappend "1.0.2"]]}}
  :main com.zensols.cisql.core)
