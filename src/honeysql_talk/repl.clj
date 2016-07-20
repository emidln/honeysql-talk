(ns honeysql-talk.repl
  (:refer-clojure :exclude [format update cast])
  (:require [system.components.postgres :refer [new-postgres-database]]
            [com.stuartsierra.component :as component]
            [clj-time.core :as t]
            clj-time.jdbc
            [clojure.java.jdbc :refer [query execute!]]
            [honeysql.core :refer [call format raw]]
            [honeysql.helpers :refer :all]
            [honeysql-talk.extra :refer :all]))

;; this is setup for the docker instructions in this project's README.md
(def db (-> (new-postgres-database {:classname "org.postgresql.Driver"
                                    :subprotocol "postgresql"
                                    :subname "//127.0.0.1:5432/exercises"
                                    :user "honeysql"
                                    :password "honeysql"})
            (component/start)))

(comment

  ;; using HoneySQL

  (with-query-logged
    (query-runner db (-> (select :*)
                         (from :cd.members)
                         (limit 1))))

  ;; using SQL
  (query db ["SELECT * FROM cd.members LIMIT 1"])

  )

