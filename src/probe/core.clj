(ns probe.core
  (:require [probe.hook :as hook]
            [clojure.tools.logging :as log])
  (:import (java.util.concurrent CopyOnWriteArrayList ConcurrentHashMap)))

(defn get-state-obj
  [depth]
  (doto (ConcurrentHashMap.)
    (.put :depth depth)
    (.put :children (CopyOnWriteArrayList.))))

(defn update-state
  [obj class duration]
  (doto obj
    (.put :class class)
    (.put :duration duration)))

(defn add-child
  [obj child]
  (let [children (.get obj :children)]
    (.add children child)))

(defn get-qualify-name [vared-fn]
  (let [{:keys [ns name]} (meta vared-fn)]
    (format "%s/%s" (str ns) name)))

(defn log-state
  [state]
  (letfn [(log [depth state]
            (let [children (.get state :children)
                  class (.get state :class)
                  duration (.get state :duration)
                  anchor (str (apply str (repeat depth "    ")) "\\__")
                  new-depth (inc depth)]
              (printf "%s %s %s ms\n" anchor class duration)
              (when (seq children)
                (doseq [i children]
                  (log new-depth i)))))]
    (let [s (with-out-str
              (printf "Tracing: \n------------------\n")
              (log 0 (first (.get state :children))))]
      (log/info s))))

(def ^:private config (atom {:logger log-state}))

(defn setup!
  [m]
  (swap! config merge m))

(def root-state ::Root)

(def ^:dynamic *state* root-state)

(defn exec [state f args]
  (let [depth  (.get state :depth)
        state (get-state-obj (inc depth))
        upper-state *state*]
    (binding [*state* state]
      (let [start (System/currentTimeMillis)
            rs (apply f args)
            end (System/currentTimeMillis)
            qualify-name (get-qualify-name f)
            duration (- end start)]
        (update-state state qualify-name duration)
        (add-child upper-state state)
        rs))))

(defn duration
  [f args]
  (if (= root-state *state*)
    (binding [*state* (get-state-obj 0)]
      (let [r (exec *state* f args)]
       ((:logger @config) *state*)
       r))
    (exec *state* f args)))

(defn duration-var
  [f & args]
  (duration f args))

(defn add-hooks [var-list]
  (assert (every? var? var-list))
  (doseq [v var-list]
    (hook/add-hook v #'duration-var)))

(defn remove-hooks [var-list]
  (assert (every? var? var-list))
  (doseq [v var-list]
    (hook/remove-hook v)))

(def clear-all-hooks hook/clear-all-hooks)

(defn clear-and-add-hooks [var-list]
  (clear-all-hooks)
  (add-hooks var-list))

(def clear-all-hooks hook/clear-all-hooks)

(comment
  (defn test-4 []
    (let [t 6000]
      (printf "%s sleep: %s ms\n" "test-4" t)
      (Thread/sleep t)))

  (defn test-2 []
    (let [t 1000]
      (printf "%s sleep: %s ms\n" "test-2" t)
      (Thread/sleep 1000)
      (future (test-4))))

  (defn test-3 []
    (let [t 1000]
      (printf "%s sleep: %s ms\n" "test-3" t)
      (Thread/sleep t)))

  (defn test-1 []
    (let [t 4000]
      (printf "%s sleep: %s ms\n" "test-1" t)
      (Thread/sleep t)
      (test-2)
      (test-3)))

  (def var-list
    [#'test-1
     #'test-2
     #'test-3
     #'test-4])

  (clear-and-add-hooks var-list)

  (test-1))
