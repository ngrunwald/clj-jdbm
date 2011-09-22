(ns clj-jdbm.test.core
  (:use [clj-jdbm]
        [clojure.test]
        [clojure.java.io :only [delete-file]])
  (:import [jdbm.helper StringComparator]
           [jdbm RecordManagerOptions]))

(def db-file "/tmp/test-jdbm")

(defn cleanup
  [path]
  (doseq [ext [".lg" ".db"]]
    (delete-file (str path ext) true)))

(deftest jdbm
  (try
    (do
      (let [manager (create-manager db-file {RecordManagerOptions/AUTO_COMMIT true})]
        (testing "htree"
          (let [htree (get-db :htree manager "htree")]
            (is (db-store htree "foo" 50) 50)
            (is (db-fetch htree "foo") 50)
            (is (db-update htree "foo" inc) 51)
            (is (db-fetch htree "foo") 51)
            (letfn [(add [val num] (+ val num))]
              (is (db-update htree "foo" add :args [9]) 60)
              (is (db-update htree "bar" add :args [5] :default 0) 5))
            (is (db-fetch htree "foo") 60)
            (is (db-fetch htree "bar") 5)))
        (testing "btree"
          (let [btree (get-db :btree manager "btree" (StringComparator.))]
            (is (db-store btree "foo" 50) 50)
            (is (db-fetch btree "foo") 50)
            (is (db-update btree "foo" inc) 51)
            (is (db-fetch btree "foo") 51)
            (letfn [(add [val num] (+ val num))]
              (is (db-update btree "foo" add :args [9]) 60)
              (is (db-update btree "bar" add :args [5] :default 0) 5))
            (is (db-fetch btree "foo") 60)
            (is (db-fetch btree "bar") 5)))
        (close-manager manager))
      (let [manager (create-manager db-file)]
        (testing "persistence"
          (let [btree (get-db :btree manager "btree" (StringComparator.))
                htree (get-db :htree manager "htree")]
            (is (db-fetch htree "foo") 60)
            (is (db-fetch btree "foo") 60)))
        (testing "transaction"
          (let [btree (get-db :btree manager "btree" (StringComparator.))]
            (try
              (with-txn manager
                (is (db-store btree "foo" 50) 50)
                (is (db-fetch btree "foo") 50)
                (throw (Exception. "this ends now!")))
              (catch Exception e
                nil))
            (is (db-fetch btree "foo") 60)))))
    (finally (cleanup db-file))))
