(ns ^{:doc "Process query at the command line from user input."
      :author "Paul Landes"}
    zensols.cisql.read
  (:require [clojure.tools.logging :as log]
            [clojure.string :as s]
            [instaparse.core :as insta]
            [zensols.cisql.conf :as conf]))

(def ^:private cmd-bnf-fn
  "Generated DSL parser."
  (atom nil))

(def ^:dynamic *std-in* nil)

(def ^:private line-limit 200)

(defn- directive-bnf [directive-name nargs]
  (->> (cond (= "*" nargs) " (ws arg)*"
             (= "+" nargs) " (ws arg)+"
             (= ".." nargs) " (ws arg)?"
             (and (number? nargs) (= 0 nargs)) ""
             (and (number? nargs) (= 1 nargs)) " ws arg"
             (and (number? nargs) (< 1 nargs))
             (->> (repeat nargs " ws arg") (apply str)))
       (str directive-name " = <'" directive-name "'>")))

(defn- cmd-bnf-def [eoq directives]
  (->> ["form = ws? directive ws? / sql eoq?"
        (str "directive = " (s/join " | " (map :name directives)))
        (->> directives
             (map #(directive-bnf (:name %) (:arg-count %)))
             (s/join \newline))
        "<arg> = ( <\"'\"> #\"[^']+\" <\"'\"> / #\"[^ ]+\" )"
        "<ws> = <#\"\\s+\">"
        (str "sql = #\".+(?=" eoq ")\" / #\".+\"")
        (str "eoq = <'" eoq "'>")]
       (s/join \newline)))

(defn set-grammer [end-of-query-separator directives]
  (->> (cmd-bnf-def end-of-query-separator directives)
       insta/parser
       (reset! cmd-bnf-fn)))

(defn read-input [line]
  (let [form (@cmd-bnf-fn line)
        eoq? (and (= 3 (count form)) (= [:eoq] (nth form 2)))]
    (if (insta/failure? form)
      (let [msg (pr-str form)]
        (throw (ex-info msg {:line line :msg msg}))))
    (->> (second form)
         list
         (cons [:eoq? eoq?])
         (into {}))))

(defn- process-input [end-fn reset-fn query user-input]
  (let [{:keys [sql directive eoq?]} (read-input user-input)]
    (log/tracef "sql: <%s>, dir: <%s>, eoq?: <%s>" sql directive eoq?)
    (log/tracef "query so far: %s" query)
    (cond directive
          (end-fn (merge {:name (first directive)}
                         (if (> (count directive) 1)
                           {:args (rest directive)})))
          (= (conf/config :linesep) sql)
          (if (-> query .toString s/trim empty?)
            (reset-fn)
            (end-fn :end-of-query))
          true (do (.append query sql)
                   (.append query \newline)
                   (if eoq? (end-fn :end-of-query))))))

(defn read-query
  "Read a query and process it."
  []
  (let [query (StringBuilder.)
        line-no (atom 1)
        directive (atom nil)]
    (letfn [(reset []
              (reset! line-no 0))
            (end [dir]
              (log/debugf "query: %s" query)
              (reset! line-no line-limit)
              (reset! directive dir))]
     (while (<= @line-no line-limit)
       (log/tracef "lines no: %d" @line-no)
       (let [prompt (try 
                      (format (conf/config :prompt) @line-no)
                      (catch Exception e
                        (format "<bad prompt: %s> " e)))]
         (print prompt))
       (flush)
       (let [user-input (.readLine *std-in*)]
         (log/debugf "line: %s" user-input)
         (cond (nil? user-input) (end :end-of-session)
               (-> user-input s/trim empty?) (.append query \newline)
               true (process-input end reset query user-input)))
       (swap! line-no inc)))
    (let [query-str (s/trim (.toString query))
          query (if-not (empty? query-str) query-str)]
      (merge (if query {:query query})
             {:directive @directive}))))
