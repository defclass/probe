(ns probe.hook
  "refer: https://github.com/technomancy/robert-hooke")

(def enable-hooks? (atom true))

(defn disable-hooks [] (reset! enable-hooks? false))
(defn enable-hooks [] (reset! enable-hooks? true))

(defonce hooked-vars (atom []))

(defn- get-hook [v]
  (-> @v meta ::hook))

(defn- get-original [v]
  (-> @v meta ::original))

(defn- compose-hook [original hook]
  (if @enable-hooks?
    (fn [& args]
      (apply hook original args))
    original))

(defn- run-hooks [hook original args]
  (apply (compose-hook original hook) args))

(defn- prepare-for-hooks [v]
  (when-not (get-hook v)
    (let [hook (atom {})]
      (alter-var-root
        v (fn [original]
            (let [original-fn (with-meta original (meta v))]
              (with-meta
                (fn [& args]
                  (run-hooks @hook original-fn args))
                {::hook hook
                 ::original original-fn})))))))

(defn add-hook
  [target-var f]
  (prepare-for-hooks target-var)
  (swap! hooked-vars conj target-var)
  (reset! (get-hook target-var) f))

(defn- clear-hook-mechanism [target-var]
  (alter-var-root target-var
                  (constantly (get-original target-var))))

(defn remove-hook
  "Remove hook from target-var."
  [target-var]
  (when (get-hook target-var)
    (clear-hook-mechanism target-var)))

(defn clear-all-hooks
  []
  (doseq [v @hooked-vars]
    (remove-hook v))
  (reset! hooked-vars []))