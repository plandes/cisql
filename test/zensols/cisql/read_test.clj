(ns zensols.cisql.read-test
  (:require [clojure.test :refer :all]
            [zensols.cisql.read :refer :all]))

(defn- init-grammer-dir []
  (set-grammer "go" [{:name "twoarg"
                      :arg-count 2}
                     {:name "shtab"
                      :arg-count 0}
                     {:name "export"
                      :arg-count 1}
                     {:name "many"
                      :arg-count "*"}
                     {:name "moreone"
                      :arg-count "+"}
                     {:name "zero-one"
                      :arg-count ".."}]))

(deftest test-directive []
  (init-grammer-dir)
  (is (= {:eoq? false, :directive [:twoarg "a1" "a2"]}
         (read-input "twoarg a1 a2")))
  (is (= {:eoq? false, :sql "twoarg arg"}
         (read-input "twoarg arg")))
  (is (= {:eoq? false, :directive [:export "arg1"]}
         (read-input "export arg1")))
  (is (= {:eoq? false, :directive [:export "arg1 2"]}
         (read-input "export 'arg1 2'")))
  (is (= {:eoq? false, :directive [:many]}
         (read-input "many")))
  (is (= {:eoq? false :directive [:many "a1"]}
         (read-input "many a1")))
  (is (= {:eoq? false :directive [:many "a1" "a2"]}
         (read-input "many a1 a2")))
  (is (= {:eoq? false :directive [:moreone "a1"]}
         (read-input "moreone a1")))
  (is (= {:eoq? false :directive [:moreone "a1" "a2"]}
         (read-input "moreone a1 a2")))
  (is (= {:eoq? false :sql "moreone"}
         (read-input "moreone")))
  (is (= {:eoq? false :directive [:zero-one]}
         (read-input "zero-one")))
  (is (= {:eoq? false :directive [:zero-one "a1"]}
         (read-input "zero-one a1")))
  (is (= {:eoq? false :sql "zero-one a1 a2"}
         (read-input "zero-one a1 a2")))
  (is (= {:eoq? false :sql "zero-one a1 a2 a3"}
         (read-input "zero-one a1 a2 a3"))))

(deftest test-sql []
  (init-grammer-dir)
  (is (= {:sql "select * from tmp" :eoq? true}
         (read-input "select * from tmpgo")))
  (is (= {:sql "select * from tmp" :eoq? false}
         (read-input "select * from tmp")))
  (is (= {:sql "select * from tmp " :eoq? true}
       (read-input "select * from tmp go")))
  (is (= {:sql "  select * from tmp " :eoq? true}
       (read-input "  select * from tmp go")))
  (is (= {:sql "  select * from tmp" :eoq? true}
         (read-input "  select * from tmpgo")))
  (is (= {:sql "   select * from tmp" :eoq? false}
         (read-input "   select * from tmp")))
  (is (= {:directive [:shtab] :eoq? false}
         (read-input " shtab")))
  (is (= {:directive [:shtab] :eoq? false}
         (read-input "shtab ")))
  (is (= {:sql "shtab" :eoq? true}
         (read-input "shtabgo")))
  (is (= {:sql "shtab " :eoq? true}
         (read-input "shtab go"))))

(defn- invoke-cel [sql]
  (let [reader (->> (java.io.StringReader. sql)
                    (java.io.BufferedReader.))
        res (atom nil)]
    {:output (with-out-str
               (binding [*std-in* reader]
                 (reset! res (read-query))))
     :result @res}))

(defn- init-grammer-lines []
  (set-grammer ";" [{:name "shtab"
                     :arg-count ".."}
                    {:name "export"
                     :arg-count 1}]))

(deftest test-cel []
  (init-grammer-lines)
  (is (= {:output " 1 >  1 > ", :result {:directive :end-of-session}}
         (invoke-cel ";")))
  (is (= {:output " 1 > ", :result {:directive :end-of-session}}
         (invoke-cel "")))
  (is (= {:output " 1 > ", :result {:query "select * from table1", :directive :end-of-query}}
         (invoke-cel "select * from table1;")))
  (is (= {:output " 1 >  2 > ", :result {:query "select * from table1", :directive :end-of-session}}
         (invoke-cel "select * from table1")))
  (is (= {:output " 1 >  2 > ", :result {:query "select *\nfrom table1", :directive :end-of-query}}
         (invoke-cel "select *
from table1;")))
  (is (= {:output " 1 > ", :result {:directive {:name :shtab}}}
         (invoke-cel "shtab"))))
