# probe

A lightweight profiler for clojure app.


## Usage

Add probe dependence:

[![Clojars Project](https://img.shields.io/clojars/v/probe.svg)](https://clojars.org/defclass/probe)

Examples:

```clojure
(require '[probe.core :as probe])
=> nil

(defn test-4
  []
  (Thread/sleep 1000))
=> #'user/test-4

(defn test-2
  []
  (Thread/sleep 1000)
  (future (test-4)))
=> #'user/test-2

(defn test-3
  []
  (Thread/sleep 1000))
=> #'user/test-3

(defn test-1
  []
  (Thread/sleep 2000)
  (test-2)
  (test-3))
=> #'user/test-1

(def var-list
  [#'test-1
   #'test-2
   #'test-3
   #'test-4])
=> #'user/var-list

(probe/add-hooks var-list)
=> nil

(test-1)
=> nil

------------------
\__ user/test-1 4009 ms
    \__ user/test-2 1004 ms
        \__ user/test-4 1003 ms
    \__ user/test-3 1002 ms

(clear-all-hooks)
=> []

(test-1)
=> nil

```

## License

Copyright © 2019 Michael Wong

Distributed under the Eclipse Public License .
