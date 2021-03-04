(defproject com.zensols.tools/cisql "0.1.0-SNAPSHOT"
  :description "Command line SQL interface tool"
  :url "https://github.com/plandes/SQL CLI Interface"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"
            :distribution :repo}
  :plugins [[lein-codox "0.10.7"]
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
  :dependencies [[org.clojure/clojure "1.9.0"]

                 ;; api
                 [org.clojure/java.jdbc "0.6.1"]

                 ;; db drivers
                 [org.clojure/data.csv "0.1.4"]

                 ;; driver dep download
                 [com.cemerick/pomegranate "0.3.1"]

                 ;; gui
                 [com.zensols.gui/tabres "0.0.11"]

                 ;; mnemonic DSL
                 [instaparse "1.4.5"]

                 ;; repl
                 [cider/cider-nrepl "0.25.3"]

                 ;; jdbc
                 [commons-dbcp/commons-dbcp "1.4"]

                 ;; logging: log4j2
                 [org.apache.logging.log4j/log4j-core "2.7"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.7"]
                 [org.apache.logging.log4j/log4j-1.2-api "2.7"]
                 [org.apache.logging.log4j/log4j-jcl "2.7"]

                 ;; command line
                 [com.zensols.tools/actioncli "0.0.30"]]
  :pom-plugins [[org.codehaus.mojo/appassembler-maven-plugin "1.6"
                 {:configuration ([:programs
                                   [:program
                                    ([:mainClass "zensols.cisql.core"]
                                     [:id "cisql"])]]
                                  [:environmentSetupFileName "setupenv"])}]]
  :profiles {:uberjar {:aot :all}
             :appassem {:aot :all}
             :snapshot {:git-version {:version-cmd "echo -snapshot"}}
             :test
             {:jvm-opts ["-Dlog4j.configurationFile=test-resources/test-log4j2.xml" "-Xmx12g"]}}
  :main zensols.cisql.core)
