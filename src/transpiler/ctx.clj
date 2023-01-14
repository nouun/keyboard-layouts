(ns transpiler.ctx
  (:require [utils :refer [fmt]]))

(defn ctx [& [outer binds exprs]]
  (atom
    (loop [ctx {:outer outer}
           b binds
           e exprs]
      (cond
        (= nil b)
        ctx

        (= '& (first b))
        (assoc ctx (nth b 1) e)

        :else
        (recur (assoc ctx (first b) (first e)) (next b) (rest e))))))

(defn ctx-find [ctx k]
  (cond
    (contains? @ctx k) ctx
    (:outer @ctx) (ctx-find (:outer @ctx) k)
    :else nil))

(defn ctx-get [ctx k]
  (let [e (ctx-find ctx k)]
    (when-not e
      (throw (Exception. (fmt "'~A' not found" k))))
    (get @e k)))

(defn ctx-set [ctx k v]
  (swap! ctx assoc k v)
  v)
