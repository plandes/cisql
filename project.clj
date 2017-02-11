(defproject com.zensols.tools/cisql "0.1.0-SNAPSHOT"
  :description "Command line SQL interface tool"
  :url "https://github.com/plandes/SQL CLI Interface"
  :license {:name "Apache License version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"
            :distribution :repo}
  :plugins [[lein-codox "0.10.1"]
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
               log4j/log4j
               ch.qos.logback/logback-classic]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ;; api
                 [org.clojure/java.jdbc "0.3.7"]

                 ;; db drivers
                 [org.clojure/data.csv "0.1.2"]

                 ;; driver dep download
                 [com.cemerick/pomegranate "0.3.1"]

                 ;; gui
                 [com.zensols.gui/tabres "0.0.6"]
                 [com.zensols.gui/pref "0.0.2"]

                 ;; mnemonic DSL
                 [instaparse "1.4.5"]

                 ;; logging: log4j2
                 [org.apache.logging.log4j/log4j-api "2.7"]
                 [org.apache.logging.log4j/log4j-core "2.7"]
                 [org.apache.logging.log4j/log4j-1.2-api "2.7"]
                 [org.apache.logging.log4j/log4j-jcl "2.7"]

                 ;; command line
                 [com.zensols.tools/actioncli "snapshot"]]
  :pom-plugins [[org.codehaus.mojo/appassembler-maven-plugin "1.6"
                 {:configuration ([:programs
                                   [:program
                                    ([:mainClass "zensols.cisql.core"]
                                     [:id "cisql"])]]
                                  [:environmentSetupFileName "setupenv"])}]]
  :profiles {:uberjar {:aot [zensols.cisql.core]}
             :appassem {:aot :all}
             :dev
             {:jvm-opts
              ["-Dlog4j.configurationFile=test-resources/log4j2.xml" "-Xms4g" "-Xmx8g" "-XX:+UseConcMarkSweepGC"]
              :dependencies [[com.zensols/clj-append "1.0.5"]]}}
  :main zensols.cisql.core)
