(ns honeysql-talk.repl
  (:refer-clojure :exclude [format update cast])
  (:require [system.components.postgres :refer [new-postgres-database]]
            [com.stuartsierra.component :as component]
            [clj-time.core :as t]
            clj-time.jdbc
            [honeysql.core :refer [call format raw]]
            [honeysql.helpers :refer :all]
            [honeysql-talk.extra :refer :all]))

(def db (-> (new-postgres-database {:classname "org.postgresql.Driver"
                                    :subprotocol "postgresql"
                                    :subname "//127.0.0.1:5432/exercises"
                                    :user "honeysql"
                                    :password "honeysql"})
            (component/start)))

(comment

  (query-runner db (-> (select :*)
                       (from :cd.members)
                       (limit 1)))

  )

