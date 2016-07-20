(defproject honeysql-talk "0.1.0-SNAPSHOT"
  :description "Talk on HoneySQL given at Chicago Clojure Meetup"
  :url "http://github.com/emidln/honeysql-talk"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [;; producing the presentation
                 [org.clojure/clojure "1.8.0"]
                 [com.emidln/ssg "0.1.1"]
                 [hiccup "1.0.5"]
                 [fipp "0.6.5"]
                 ;; for experimenting
                 [org.clojure/tools.trace "0.7.9"]
                 [org.clojure/java.jdbc "0.6.2-alpha1"]
                 [org.postgresql/postgresql "9.4.1209"]
                 [honeysql "0.7.0"]
                 [org.clojure/java.jdbc "0.6.2-alpha1"]
                 [clj-time "0.12.0"]
                 [org.danielsz/system "0.3.0"]
                 [prismatic/schema "1.1.2"]
                 [com.taoensso/timbre "4.7.0"]
                 [curiosity.utils "0.9.0-SNAPSHOT"]])
