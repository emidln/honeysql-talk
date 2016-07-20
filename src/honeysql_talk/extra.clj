(ns honeysql-talk.extra
  (:refer-clojure :exclude [cast format])
  (:require [clojure.string :as str]
            [honeysql.core :refer [call format]]
            [taoensso.timbre :as log]
            [curiosity.utils :refer [when-seq-let]]
            [clojure.java.jdbc :as jdbc]))


(defn with-static
  {:arglists '([name columns & values]
               [m name columns & values])}
  [& args]
  (let [[m name columns & values] (if (map? (first args))
                                    args
                                    (cons {} args))]
    (assoc m :with [[[name {:columns columns}]
                     {:values (vec values)}]])))

(defn with-query
  ([name query]
   (with-query {} name query))
  ([m name query]
   (assoc m :with [[name query]])))

(defn with-recursive
  ([name query]
   (with-recursive {} name query))
  ([m name query]
   (assoc m :with-recursive [[name query]])))

(defn union
  {:argslists '([& queries] [m & queries])}
  [& args]
  (let [[m & queries] (if (map? (first args))
                        args
                        (cons {} args))]
    (assoc m :union (vec queries))))

(defn union-all
  {:arglists '([& queries] [m & queries])}
  [& args]
  (let [[m & queries] (if (map? (first args))
                        args
                        (cons {} args))]
    (assoc m :union-all (vec queries))))

(defn intersect
  {:arglists '([& queries] [m & queries])}
  [& args]
  (let [[m & queries] (if (map? (first args))
                        args
                        (cons {} args))]
    (assoc m :intersect (vec queries))))

(defn snake-case
  "Foo-bar -> foo_bar"
  [s]
  (-> s str/lower-case (str/replace "-" "_")))

(defn kebob-case
  "Foo_Bar -> foo-bar"
  [s]
  (-> s str/lower-case (str/replace "_" "-")))

(defn cast
  "CAST()"
  [type field]
  (call :cast field type))

(def ^:dynamic *jdbc-log-level*
  "Log Level at which to print honey-jdbc-runner queries. nil is off."
  nil)

(defmacro with-query-logged
  {:arglists '[[& body]
               [log-level & body]]
   :doc "Runs the body with queries logged at log-level (default :error)"}
  [& body]
  (let [[car & cdr] body
        log-level (if (keyword? car) car :debug)
        query (if (keyword? car) cdr body)]
    `(binding [~'honeysql-talk.extra/*jdbc-log-level* ~log-level]
       (do ~@query))))

(defn query-logger
  "Helper to spy on honeysql queries at a particular level defined by *jdbc-log-level*"
  [q]
  (when-let [level *jdbc-log-level*]
                                        ;(log/spy level q)
    (prn level q)
    )
  q)

(defn honey-jdbc-runner
  "Returns a vector of results given a db-component and a built honeysql map.
  Takes a jdbc-fn implementation and feeds the db-component and the generated sql vec from query-map to it"
  [jdbc-fn quoting db-component query-map]
  (when-seq-let [results (->> (format query-map :quoting quoting)
                              query-logger
                              (jdbc-fn db-component))]
                (vec results)))

;; These are set up for PostgreSQL
(def ^{:arglists '[[db-conn honeysql-map]]}
  query-runner
  "Given a db-component and a query-map, query using the query-map returning a vector or nil"
  (partial honey-jdbc-runner #(jdbc/query %1 %2 {:identifiers kebob-case}) :ansi))

(def ^{:arglists '[[db-conn honeysql-map]]}
  exec-runner
  "Given a db-component and a query-map, execute the query-map returning a vector or nil"
  (partial honey-jdbc-runner jdbc/execute! :ansi))

