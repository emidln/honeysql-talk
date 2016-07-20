(ns honeysql-talk.slides
  (:require [ssg.core :as ssg]
            [hiccup.element :refer [javascript-tag]]
            [hiccup.page :refer [include-js include-css]]
            fipp.clojure))

(defmacro pprint-str
  [& body]
  (->> (map #(with-out-str (fipp.clojure/pprint %)) body)
       (clojure.string/join "")))

(defmacro clj-code
  [& body]
  `[:pre [:code (pprint-str ~@body)]])

(defn init-reveal-js []
  (javascript-tag
   "Reveal.initialize({
         history: true,
         dependencies: [
             { src: 'plugin/markdown/marked.js' },
             { src: 'plugin/markdown/markdown.js' },
             { src: 'plugin/notes/notes.js' },
             { src: 'plugin/highlight/highlight.js',
               async: true,
               callback: function() { hljs.initHighlightingOnLoad();} }]})"))

(defmacro reveal-js-slides
  "Creates a reveal.js slides document"
  {:style/indent 1}
  [opts & slides]
  `[:html
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"}]
     [:title ~(:title opts)]
     (include-css "css/reveal.css"
                  ~(str "css/theme/" (:theme opts) ".css")
                  ~(str "lib/css/" (:code-theme opts) ".css"))]
    [:body
     [:div.reveal
      [:div.slides ~@slides]]
     (include-js "lib/js/head.min.js" "js/reveal.js")
     (init-reveal-js)]])

(def slides-index
  (reveal-js-slides {:title "SQL as Data"
                     :theme "black"
                     :code-theme "zenburn"}
    [:section
     [:h1 "HoneySQL"]
     [:br]
     [:h4 "Chicago Clojure"]
     [:h6 "July 20, 2016"]]
    [:section
     [:p "about me"]
     (clj-code
      {:name "Brandon Adams"
       :languages #{:clojure :java :python :c}
       :works-at "https://curiosity.com"
       :github "@emidln"})]
    [:section
     [:p "&lt;Plug&gt; Curiosity.com &lt;/Plug&gt;"]
     (clj-code
      {:languages #{:clojure :python}
       :databases #{:postgresql :redis :zookeeper :mongodb}
       :location "Ravenswood, Chicago, IL, USA"
       :hiring? true})]
    [:section
     [:p "Why SQL?"]
     [:ul
      [:li.fragment "popular databases ..."
       [:ul
        [:li "Oracle, MySQL, PostgreSQL, MS SQL"]
        [:li "SQLite, H2, and many more"]]]
      [:li.fragment "because boss said so"]]]
    [:section
     [:p "SQL"]
     [:br]
     [:pre [:code "SELECT * FROM foo WHERE id = 9 AND spam = 'blah';"]]
     [:pre [:code "UPDATE bar SET quux = 'spam' WHERE id = 37;"]]
     [:pre [:code {:data-trim :data-noescape}
            "INSERT INTO pokedex (pokemon, name, level)\n"
            "VALUES ('pikachu', 'Perry', 9);"]]]
    [:section
     [:p "What is HoneySQL?"]
     [:ul
      [:li.fragment "AST for SQL represented using Clojure data"]
      [:li.fragment "Helper library for friendly parameterized query construction"]
      [:li.fragment "Written by Justin Kramer ("[:a {:href "https://github.com/jkk"} "@jkk"]") and maintained at "
       [:a {:href "https://github.com/jkk/honeysql"}
        "github.com/jkk/honeysql"]]]]
    [:section
     [:p "HoneySQL the AST"]
     (clj-code
      (require '[honeysql.helpers :refer :all])
      (= (-> (select :*)
             (from :foo)
             (where [:= :id 9]
                    [:= :spam "blah"]))
         {:select '(:*)
          :from '(:foo)
          :where [:and
                  [:= :id 9]
                  [:= :spam "blah"]]}))]
    [:section
     [:p "You don't need HoneySQL if your queries are static"]
     (clj-code
      (require '[clojure.java.jdbc :as jdbc])
      (require 'clj-time.jdbc) ;; serialize/deserialize DateTimes
      (defn find-foos-by-range
        [db start stop]
        (jdbc/query db ["SELECT * FROM foo WHERE ts BETWEEN ? and ?"])))
     [:p.fragment "clojure.java.jdbc is fine if you don't need dynamic query building"]]
    [:section
     [:p "But if you need something a little more dynamic..."]
     [:span.fragment
      (clj-code
       (let [id 9
             spam "blah"]
         (cond-> "SELECT * FROM foo WHERE"
           (some? id) (str " id = " id)
           (and (some? id)
                (some? spam)) (str " AND ")
           (some? spam) (str " spam = " spam))))]
     [:br]
     [:spam.fragment
      [:p "Have you done this?"]
      [:p "Spot any potential bugs?"]]]
    [:section
     [:p "SQL Injection?"]
     [:p.fragment "We forgot to insert driver-specific placeholders and track the values in an array so we can pass those separately to JDBC."]]
    [:section
     [:p "Easy, right?"]
     [:span.fragment
      (clj-code
       (require 'clojure.java.jdbc)
       (let [id 9
             spam "blah"
             selector (fn [id spam]
                        (let [params (atom [])]

                          [(cond-> "SELECT * FROM foo WHERE"
                             (some? id) (str " id = " id)
                             (and (some? id)
                                  (some? spam)) (str " AND ")
                             (some? spam) (#(do (swap! params conj %)
                                                (str " spam = ?"))))
                           @params]))
             [sql params] (selector id spam)]
         (clojure.java.jdbc/query db sql params)))]]
    [:section
     [:p "Yikes! Maybe something does this for us?"]]
    [:section
     [:p "HoneySQL the AST (cont'd)"]
     (clj-code
      (require 'honeysql.core)
      (require '[honeysql.helpers :refer :all])
      (require 'clojure.java.jdbc)
      (let [id 9
            spam "blah"
            selector (fn [id spam]
                       (-> (cond-> (-> (select :*)
                                       (from :foo))
                             (some? id) (merge-where [:= :id id])
                             (some? spam) (merge-where [:= :spam spam]))
                           honeysql.core/format))
            [sql params] (selector id spam)]
        (clojure.java.jdbc/query db sql params)))]
    [:section
     [:p "AST Advantages"]
     [:ul
      [:li "Safety"
       [:ul [:li "Let the library worry about parameters"]]]
      [:li "Tooling"
       [:ul
        [:li "clojure.core"]
        [:li "plumbing.core"]
        [:li "your favorite utility library"]]]]]
    [:section
     [:p "By representing the AST as Clojure data structures, our core Clojure language can be used to easily manipulate a query"]
     [:br]
     [:p "Sound familiar?"]]
    [:section
     [:p "HoneySQL Notes (part 1)"]
     [:br]
     [:ul
      [:li "Maps nest as subqueries. If you want a map value to the JDBC layer use "
       [:a {:href "https://crossclj.info/doc/honeysql/0.7.0/honeysql.format.html#_value"} [:code "honeysql.format/value"]]]
      [:li "Vectors are handled by the individual clause. When it makes sense, it is used for aliasing."
       (clj-code
        (from [:foo :f]))]]]
     [:section
      [:p "HoneySQL Notes (part 2)"]
      [:ul
       [:li "Not all clauses have helpers. They are easy to define if you think it'll help readability:"
        (clj-code
         (defn union
           {:argslists '([& queries] [m & queries])}
           [& args]
           (let [[m & queries] (if (map? (first args))
                                 args
                                 (cons {} args))]
             (assoc m :union (vec queries)))))]
       [:li "Raw SQL access is available, see " [:a {:href "https://crossclj.info/doc/honeysql/latest/honeysql.core.html#_raw"} [:code "honeysql.core/raw"]]]]]
    [:section
     [:p "Extending HoneySQL Operators"]
     (clj-code
      ;; define a new operator
      (require 'honeysql.format)
      (defmethod honeysql.format/fn-handler "json-eq" [_ outer-key inner-key val]
        (format "%s->>'%s' = %s" outer-key inner-key (honeysql.format/to-sql val)))
      ;;  use it
      (-> (select :*)
          (from :foo)
          (where [:json-eq "extra" "foo-id" 5])))]
    [:section
     [:p "Extending HoneySQL Clauses"]
     (clj-code
      (require 'honeysql.core)
      (require '[honeysql.format :refer [comma-join format-clause to-sql]])
      (require '[honeysql.helpers :refer :all])
      (defmethod format-clause :returning [[_ fields] _]
        (str "RETURNING " (comma-join (map to-sql fields))))
      (defhelper returning [m args]
        (assoc m :returning args))
      (-> (update :foo)
          (sset {} {:count #sql/call [:+ :count 1]})
          (where [:= :id 99])
          (returning :count)))]
    [:section
     [:p "Using clojure.java.jdbc"]
     (clj-code
      (require '[clojure.java.jdbc :as jdbc]
               '[cheshire.core :as json])
      (import 'org.postgresql.util.PGobject)
      ;; setup JSON fields to automatically serialize-deserialize
      (defn value-to-json-pgobject [value]
        (doto (PGobject.)
          ;; hack for now -- eventually we should properly determine the actual type
          (.setType "jsonb")
          (.setValue (json/generate-string value))))
      (extend-protocol jdbc/ISQLValue
        clojure.lang.IPersistentMap
        (sql-value [value] (value-to-json-pgobject value)))
      (extend-protocol jdbc/IResultSetReadColumn
        org.postgresql.util.PGobject
        (result-set-read-column [pgobj metadata idx]
          (let [type (.getType pgobj)
                value (.getValue pgobj)]
            (if (#{"jsonb" "json"} type)
              (json/parse-string value true)
              value)))))]
    [:section
     [:p "Someone already did this for ..."
      [:p "Postgres: " [:a {:href "https://github.com/nilenso/honeysql-postgres"} "nilenso/honey-postgres"]]
      [:p "Joda Time: " [:a {:href "https://github.com/clj-time/clj-time/blob/master/src/clj_time/jdbc.clj"}
                         "clj-time"]]]]
    [:section
     [:p "Solid Alternatives"]
     [:ul
      [:li [:a {:href "https://github.com/krisajenkins/yesql"} "yesql"]
       [:ul
        [:li "SQL is already a DSL, why do we need another abstraction?"]
        [:li "parses SQL into clojure functions"]
        [:li "kinda leaky"]]]
      [:li [:a {:href "http://hugsql.com/"} "HugSQL"]
       [:ul
        [:li "inspired by yesql"]
        [:li "parses SQL into clojure functions"]
        [:li "supports vastly more SQL than yesql"]
        [:li "actively maintained"]]]]]
    [:section
     [:p "Exercises"]
     [:br]
     [:p "See " [:a {:href "https://github.com/emidln/honeysql-talk"} "this talk's github"]
      " for examples of extending and using HoneySQL using " [:a {:href "https://pgexercises.com"} "PGExercises.com"]]]))

(defn render
  []
  (ssg/render-pages "resources/" {"index.html" slides-index}))

(render)

(defn -main []

  (render)

  )
