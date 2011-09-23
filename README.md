# clj-jdbm

clj-jdbm is a small Clojure wrapper for the [JDBM](http://jdbm.sourceforge.net/) embedded store library.

## Usage

Some basic uses:

```clojure
(ns clj-jdbm
  (:import [jdbm RecordManagerOptions]
           [jdbm.helper StringComparator])
  (:require [clj-jdbm :as jdbm]))

(let [manager (jdbm/create-manager "path/to/file" {RecordManagerOptions/AUTO_COMMIT true})
      htree (jdbm/get-db :htree manager "test1")
      btree (jdbm/get-db :btree manager "test2" (StringComparator.))]
  (jdbm/db-store htree "foo" 50) ; 50
  (jdbm/db-fetch htree "foo") ; 50
  (jdbm/db-update htree "foo" inc)
  (letfn [(add [val & args] (apply + val args))]
     (jdbm/db-update htree "bar" add :args [5 6] :default 0))) ; 11
```

Transaction support:

```clojure
(let [manager (jdbm/create-manager "path/to/file")
      btree (jdbm/get-db :btree manager "test3" (StringComparator.))]
  (jdbm/db-store btree "foo" 50) ; 50
  (.commit manager)
  (try
    (jdbm/with-txn manager
      (jdbm/db-store btree "foo" 50) ; 50
      (jdbm/db-fetch btree "foo") ; 50
    (throw (Exception. "this ends now!")))
      (catch Exception e
        nil))
    (jdbm/db-fetch btree "foo")) ; 60
```

For more details see tests and function documentation.

## License

Copyright (C) 2011 [Linkfluence](http://us.linkfluence.net)

Distributed under the Eclipse Public License, the same as Clojure.
