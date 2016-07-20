(ns honeysql-talk.core-test
  (:refer-clojure :exclude [format])
  (:require [clojure.test :refer :all]
            [honeysql.core :refer [format]]
            [honeysql.helpers :refer :all]
            [honeysql-talk.extra :refer [with-static]]))

;; Some examples of build queries from https://pgexercises.com

(deftest test-0-select-all
  (testing "https://pgexercises.com/questions/basic/selectall.html"
    (is (= (-> (select :*)
               (from :cd.facilities)
               format))
        ["SELECT * FROM cd.facilities"])))

(deftest test-1-select-specific
  (testing "https://pgexercises.com/questions/basic/selectspecific.html"
    (is (= (-> (select :name :membercost)
               (from :cd.facilities)
               format)
           ["SELECT name, membercost FROM cd.facilities"]))))

(deftest test-2-where
  (testing "https://pgexercises.com/questions/basic/where.html"
    (is (= (-> (select :*)
               (from :cd.facilities)
               (where [:> :membercost 0])
               format)
           ["SELECT * FROM cd.facilities WHERE membercost > ?" 0]))))

(deftest test-3-where2
  (testing "https://pgexercises.com/questions/basic/where2.html"
    (is (= (-> (select :facid :name :membercost :monthlymaintenance)
               (from :cd.facilities)
               (where [:> :membercost 0]
                      [:< :membercost #sql/call [:/ :monthlymaintenance 50.0]])
               format)
           ["SELECT facid, name, membercost, monthlymaintenance FROM cd.facilities WHERE (membercost > ? AND membercost < (monthlymaintenance / ?))" 0 50.0]))))

(deftest test-4-where3
  (testing "https://pgexercises.com/questions/basic/where3.html"
    (is (= (-> (select :*)
               (from :cd.facilities)
               (where [:like :name "%Tennis%"])
               format)
           ["SELECT * FROM cd.facilities WHERE (name like ?)" "%Tennis%"]))))


(deftest test-5-where4
  (testing "https://pgexercises.com/questions/basic/where4.html"
    (is (= (-> (select :*)
               (from :cd.facilities)
               (where [:in :facid [1 5]])
               format)
           ["SELECT * FROM cd.facilities WHERE (facid in (?, ?))" 1 5]))
    (is (= (-> (select :*)
               (from :cd.facilities)
               (where [:in :facid (-> (select :facid)
                                      (from :cd.facilities))])
               format)
           ["SELECT * FROM cd.facilities WHERE (facid in (SELECT facid FROM cd.facilities))"]))))

(deftest test-6-classify
  (testing "https://pgexercises.com/questions/basic/classify.html"
    (is (= (-> (select :name
                       [#sql/call [:case
                                   [:> :monthlymaintenance 100] "expensive"
                                   :else "cheap"]
                        :cost])
               (from :cd.facilities)
               format)
           ["SELECT name, CASE WHEN monthlymaintenance > ? THEN ? ELSE ? END AS cost FROM cd.facilities"
            100
            "expensive"
            "cheap"]))))

(deftest test-7-date
  (testing "https://pgexercises.com/questions/basic/date.html"
    (is (= (-> (select :memid :surname :firstname :joindate)
               (from :cd.members)
               (where [:>= :joindate "2012-09-01"])
               format)
           ["SELECT memid, surname, firstname, joindate FROM cd.members WHERE joindate >= ?"
            "2012-09-01"]))))

(deftest test-8-unique
  (testing "https://pgexercises.com/questions/basic/unique.html"
    (is (= (-> (select :surname)
               (modifiers :distinct)
               (from :cd.members)
               (order-by :surname)
               (limit 10)
               format)
           ["SELECT DISTINCT surname FROM cd.members ORDER BY surname LIMIT ? " 10]))))

(deftest test-9-union
  (testing "https://pgexercises.com/questions/basic/union.html"
    (is (= (format {:union [(-> (select :surname)
                                (from :cd.members))
                            (-> (select :name)
                                (from :cd.facilities))]})
           ["(SELECT surname FROM cd.members) UNION (SELECT name FROM cd.facilities)"]))))

(deftest test-10-agg
  (testing "https://pgexercises.com/questions/basic/agg.html"
    (is (= (-> (select [#sql/call [:max :joindate] :latest])
               (from :cd.members)
               format)
           ["SELECT max(joindate) AS latest FROM cd.members"]))))

(deftest test-11-agg2
  (testing "https://pgexercises.com/questions/basic/agg2.html"
    (is (= (-> (select :firstname :surname :joindate)
               (from :cd.members)
               (where [:= :joindate (-> (select #sql/call [:max :joindate])
                                        (from :cd.members))])
               format)
           ["SELECT firstname, surname, joindate FROM cd.members WHERE joindate = (SELECT max(joindate) FROM cd.members)"]))
    (is (= (-> (select :firstname :surname :joindate)
               (from :cd.members)
               (order-by [:joindate :desc])
               (limit 1)
               format)
           ["SELECT firstname, surname, joindate FROM cd.members ORDER BY joindate DESC LIMIT ?" 1]))))

(deftest test-12-simplejoin
  (testing "https://pgexercises.com/questions/joins/simplejoin.html"
    (is (= (-> (select :bks.starttime)
               (from [:cd.bookings :bks])
               (join [:cd.members :memes] [:= :mems.memid :bks.memid])
               (where [:= :mems.firstname "David"]
                      [:= :mems.surname "Farrell"])
               format)
           ["SELECT bks.starttime FROM cd.bookings bks INNER JOIN cd.members memes ON mems.memid = bks.memid WHERE (mems.firstname = ? AND mems.surname = ?)"
            "David"
            "Farrell"]))
    (is (= (-> (select :bks.starttime)
               (from [:cd.bookings :bks]
                     [:cd.members :mems])
               (where [:= :mems.firstname "David"]
                      [:= :mems.surname "Farrell"]
                      [:= :mems.memid :bks.memid])
               format)
           ["SELECT bks.starttime FROM cd.bookings bks, cd.members mems WHERE (mems.firstname = ? AND mems.surname = ? AND mems.memid = bks.memid)"
            "David"
            "Farrell"]))))

(deftest test-13-simplejoin2
  (testing "https://pgexercises.com/questions/joins/simplejoin2.html"
    (is (= (-> (select [:bks.starttime :start] [:facs.name :name])
               (from [:cd.facilities :facs])
               (join [:cd.bookings :bks] [:= :facs.facid :bks.facid])
               (where [:in :facs.facid [0 1]]
                      [:>= :bks.starttime "2012-09-21"]
                      [:< :bks.starttime "2012-09-22"])
               (order-by :bks.starttime)
               format)
           ["SELECT bks.starttime AS start, facs.name AS name FROM cd.facilities facs INNER JOIN cd.bookings bks ON facs.facid = bks.facid WHERE ((facs.facid in (?, ?)) AND bks.starttime >= ? AND bks.starttime < ?) ORDER BY bks.starttime"
            0
            1
            "2012-09-21"
            "2012-09-22"]))))

(deftest test-14-joinself
  (testing "https://pgexercises.com/questions/joins/self.html"
    (is (= (-> (select [:recs.firstname :firstname] [:recs.surname :surname])
               (from [:cd.members :mems])
               (join [:cd.members :recs] [:= :recs.memid :mems.recommendedby])
               (order-by :surname :firstname)
               format)
           ["SELECT recs.firstname AS firstname, recs.surname AS surname FROM cd.members mems INNER JOIN cd.members recs ON recs.memid = mems.recommendedby ORDER BY surname, firstname"]))))

(deftest test-update-increment
  (testing "update using increment"
    (is (= (-> (update :foo)
               (sset {:total #sql/call [:+ :total 1]})
               (where [:= :id 99])
               format)
           ["UPDATE foo SET total = (total + ?) WHERE id = ?" 1 99]))))

(deftest test-with-static-data
  (testing "with-query-static-data"
    (is (= (-> (with-static :foo
                 [:a :b :c]
                 [1 2 3]
                 [4 5 6])
               (select :*)
               (from :foo)
               format)
           ["WITH foo (a, b, c) AS (VALUES (?, ?, ?), (?, ?, ?)) SELECT * FROM foo" 1 2 3 4 5 6]))))

