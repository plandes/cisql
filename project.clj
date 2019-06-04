(defproject com.zensols.tools/cisql "0.1.0-SNAPSHOT"
  :description "Command line SQL interface tool"
  :url "https://github.com/plandes/SQL CLI Interface"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"
            :distribution :repo}
  :plugins [[lein-codox "0.10.3"]
            [org.clojars.cvillecsteele/lein-git-version "1.2.7"]]
  :codox {:metadata {:doc/format :markdown}
          :project {:name "SQL CLI Interface"}
          :output-path "target/doc/codox"
          :source-uri "https://github.com/plandes/cisql/blob/v{version}/{filepath}#L{line}"}
  :git-version {:root-ns "zensols.cisql"
                :path "src/clojure/zensols/cisql"
                :version-cmd "git describe --match v*.* --abbrev=4 --dirty=-dirty"}
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options ["-Xlint:unchecked"]
  :jar-exclusions [#".gitignore"]
  :exclusions [org.slf4j/slf4j-log4j12
               ch.qos.logback/logback-classic]
  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; api
                 [org.clojure/java.jdbc "0.6.1"]

                 ;; db drivers
                 [org.clojure/data.csv "0.1.2"]

                 ;; driver dep download
                 [com.cemerick/pomegranate "0.3.1"
                  :exclusions [ch.qos.logback/logback-classic
                               org.slf4j/slf4j-log4j12]]

                 ;; gui
                 [com.zensols.gui/tabres "0.0.6"]
                 [com.zensols.gui/pref "0.0.2" :exclusions [commons-logging]]

                 ;; mnemonic DSL
                 [instaparse "1.4.5"]

                 ;; repl
                 [cider/cider-nrepl "0.21.1"]

                 ;; logging: log4j2
                 [org.apache.logging.log4j/log4j-core "2.7"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.7"]
                 [org.apache.logging.log4j/log4j-1.2-api "2.7"]
                 [org.apache.logging.log4j/log4j-jcl "2.7"]

                 ;; command line
                 [com.zensols.tools/actioncli "0.0.29"]]
  :pom-plugins [[org.codehaus.mojo/appassembler-maven-plugin "1.6"
                 {:configuration ([:programs
                                   [:program
                                    ([:mainClass "zensols.cisql.core"]
                                     [:id "cisql"])]]
                                  [:environmentSetupFileName "setupenv"])}]]
  :profiles {:1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.0"]]}
             :uberjar {:aot :all}
             :appassem {:aot :all}
             :snapshot {:git-version {:version-cmd "echo -snapshot"}}
             :test
             {:jvm-opts ["-Dlog4j.configurationFile=test-resources/test-log4j2.xml" "-Xmx12g"]}}
  :main zensols.cisql.core)
