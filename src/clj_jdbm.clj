(ns clj-jdbm
  (:import [jdbm RecordManagerFactory RecordManagerOptions]
           [jdbm.htree HTree]
           [jdbm.btree BTree]
           [java.util Properties])
  (:require [clojure.string :as str]))

(defn ^Properties as-properties
  "Convert any seq of pairs to a java.utils.Properties instance.
   Uses as-str to convert both keys and values into strings."
  {:tag Properties}
  [m]
  (let [p (Properties.)]
    (doseq [[k v] m]
      (.setProperty p (str k) (str v)))
    p))

(defn create-manager
  "Create a db manager from a filename."
  ([filename]
     (create-manager filename (Properties.)))
  ([filename options]
     (let [props (as-properties (map (fn [[key val]]
                                      (vector
                                       key val)) options))]
       (RecordManagerFactory/createRecordManager filename props))))

(defprotocol JdbmStore
  "Protocol to handle difference in API of JDBM stores"
  (db-fetch [this key] "gets a value")
  (db-store [this key val] "inserts a value"))

(extend-type BTree
  JdbmStore
  (db-fetch
    ([this key] (.find this key))
    ([this key default] (let [current (.find this key)] (if (nil? current) default current))))
  (db-store [this key val] (.insert this key val true) val))

(extend-type HTree
  JdbmStore
  (db-fetch
    ([this key] (.get this key))
    ([this key default] (let [current (.get this key)] (if (nil? current) default current))))
  (db-store [this key val] (.put this key val) val))

(defn load-db
  "Load an existing database."
  [type manager id & [comparator]]
  (cond 
    (= type :htree) (HTree/load manager id)
    (= type :btree) (BTree/load manager id)))

(defn create-db
  "Create a new database."
  [type manager name & [comparator]]
  (let [store (cond
                (= type :htree) (HTree/createInstance manager)
                (= type :btree) (BTree/createInstance manager comparator)
                :else (throw (Exception. (str type " is not a known type, try :htree or :btree"))))]
    (.setNamedObject manager name (.getRecid store))
    store))

(defn get-db
  "Load or create the named database."
  [type manager name & [comparator]]
  (let [id (.getNamedObject manager name)]
    (if (not= id 0)
      (load-db type manager id)
      (create-db type manager name comparator))))

(defn close-manager
  "Close a db manager."
  [manager]
  (.close manager))

(defn db-delete
  [db key]
  (.remove db key))

(defn db-update
  "Takes a function and optional default value and args.
  ex: (db-update db :foo (fn [& args] (apply + args)) :args [1 2] :default 0)"
  [db key func &  {:keys [default args]}]
  (let [fetch (db-fetch db key)
        val (if (nil? fetch) default fetch)
        updated (apply func val args)]
    (if (not= fetch updated) (db-store db key updated))
    updated))

(defmacro with-txn
  [manager & body]
  `(let [res# (try
               ~@body
               (catch Exception e#
                 (.rollback ~manager)
                 (throw e#)))]     
     (.commit ~manager)
     res#))
