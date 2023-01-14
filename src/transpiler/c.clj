(ns transpiler.c
  (:require [clojure.string :as string]
            [clojure.zip :as zip]
            [utils :as utils :refer [fmt]]
            [transpiler.ctx :as ctx]))

(declare transpile-ast)

(defn- transpile-first [ast ctx]
  (-> ast first (transpile-ast ctx)))

(defn- transpile-last [ast ctx]
  (-> ast last (transpile-ast ctx)))

(defn- transpile-nth [ast n default ctx]
  (if (< n (count ast))
    (transpile-ast (nth ast n) ctx)
    default))

(defn- transpile-list [ast separator ctx]
  (->> ast (map #(transpile-ast % ctx)) (string/join separator)))

(defn- transpile-rest [ast separator ctx]
  (-> ast rest (transpile-list separator ctx)))

(defn- is-macro-call [ast ctx]
  (and (seq? ast)
       (symbol? (first ast))
       (ctx/ctx-find ctx (first ast))
       (:is-macro (ctx/ctx-get ctx (first ast)))))

(defn- transpile-macro-call [ast ctx]
  (defn check-argc [fname args fn-args]
    (let [argc (count args)
          fn-argc (count fn-args)]
      (when (not (= argc fn-argc))
        (throw (Exception. (fmt "Wrong number of args passed to: ~A. Expected ~D, actual ~D" fname argc fn-argc))))))

  (defn macro-expand [ast args]
    (if (not (seq? ast)) ast
      (if (and (= (first ast) 'clojure.core/unquote)
               (contains? args (nth ast 1)))
        (get args (nth ast 1))
        (map #(macro-expand % args) ast))))

  (let [[fname & fn-args] ast
        {args :args func :func} (ctx/ctx-get ctx fname)]
    (check-argc fname args fn-args)
    (-> args
        (zipmap fn-args)
        (->> (macro-expand func))
        (transpile-ast ctx))))

(defn- transpile-fn-call [ast ctx]
  (if (is-macro-call ast ctx)
    (transpile-macro-call ast ctx)
    (let [fn (transpile-first ast ctx)
          args (transpile-rest ast ", " ctx)]
      (fmt "~A(~A)" fn args))))

(defn- transpile-fn-decl-args [ast ctx]
  (->> ast
       (map (fn [arg]
              (utils/assert-args-eq arg 2)
              (fmt "~A ~A" (transpile-last arg ctx) (transpile-first arg ctx))))
       (string/join ", ")))

(defn- transpile-fn-decl [ast ctx]
  (let [fn (-> ast first (transpile-ast ctx))
        type-arg (-> ast rest first)
        is-typed (-> type-arg vector? not)
        _ (utils/assert-args-eq ast (if is-typed 4 3))
        fn-type (if is-typed (transpile-ast type-arg ctx) "void")
        args (-> (if is-typed (-> ast rest rest first) type-arg) (transpile-fn-decl-args ctx))
        body (-> (if is-typed (-> ast rest rest rest) (-> ast rest rest)) (transpile-list ";\n" ctx))]
    (fmt "~A ~A(~A){~%~A;~%}" fn-type fn args body)))

(defn- transpile-if [ast ctx]
  (let [conditional (transpile-first ast ctx)
        has-else (= 3 (count ast))
        if-true (->> ast rest first (transpile-ast ctx))]
    (str
      (fmt "if(~A) {~%~A~%}" conditional if-true)
      (when has-else
        (fmt " else {~%~A~%}" (->> ast last (transpile-ast ctx)))))))

(defn- transpile-when [ast ctx]
  (let [conditional (transpile-first ast ctx)
        if-true (-> ast rest (transpile-list ";\n" ctx))]
    (fmt "if(~A) {~%~A;~%}" conditional if-true)))

(defn- transpile-switch [ast ctx]
  (defn assert-mod2 [statements]
    (utils/ast-assert
      (= 0 (mod (count statements) 2))
      (fmt "case requires a key and value pair:~S" ast)))

  (defn map-case [conditional]
    (fmt "case ~A:" (transpile-ast conditional ctx)))

  (defn map-statement [statement]
    (let [conditional (first statement)
          cases (if (vector? conditional)
                  (->> conditional
                       (map map-case)
                       (string/join "\n"))
                  (map-case conditional))]
      (fmt "~A~%~A;~%break;" cases (-> statement last (transpile-ast ctx)))))

  (let [conditional (transpile-first ast ctx)
        has-default (-> ast count (mod 2) zero?)
        statements (->> ast
                        rest
                        (#(if has-default (butlast %) %))
                        (partition 2)
                        (map map-statement)
                        (string/join "\n"))
        default (if has-default
                  (transpile-last ast ctx)
                  "break")]
    (fmt "switch(~A) {~%~A~%default: ~A;~%}" conditional statements default)))

(defn- transpile-thread-first [ast ctx]
  (defn thread [value ast]
    (if (list? ast)
      (let [[head tail] (split-at 1 ast)]
        (concat head (list value) tail))
      (list ast value)))

  (let [head (first ast)
        threads (rest ast)]
    (if (-> threads count zero?)
      (transpile-ast (apply list head) ctx)
      (let [next-thread (first threads)]
        (transpile-thread-first
          (apply list (concat (list (thread head next-thread))
                             (rest threads)))
          ctx)))))

(defn- transpile-def [ast ctx]
  (defn transpile-def-var [def-name const body ctx]
    (let [modifiers (->> body rest butlast (map #(transpile-ast % ctx)) ((fn [a] (if const (cons "const" a) a))) (string/join " "))
          value (transpile-last body ctx)]
      (fmt "~A ~A = ~A;" modifiers def-name value)))

  (let [def-name (transpile-first ast ctx)
        [typing value] (last ast)]
    (case (transpile-ast typing ctx)
      "type"
      (do (utils/assert-type value symbol? "symbol")
          (fmt "typedef ~A ~A;" (transpile-ast value ctx) def-name))

      "enum"
      (do (utils/assert-type value vector? "vector")
          (fmt "enum ~A { ~A };" def-name (transpile-list value ", " ctx)))

      "var"
      (transpile-def-var def-name false (last ast) ctx)

      "const"
      (transpile-def-var def-name true (last ast) ctx)

      (throw (Exception. (fmt "Unable to define ~A." typing))))))

(defn- transpile-defmacro [ast ctx]
  (let [[fname args func] ast]
    (ctx/ctx-set ctx fname {:args args
                            :func func
                            :is-macro true})
    nil))

(defn- transpile-call [ast ctx]
  (let [[sym & body] ast]
    (case (name sym)
      "comment"
      (do (fmt "~%// ~A" (string/replace (string/join "\n" body) #"\n" "\n// ")))

      "defmacro"
      (do (utils/assert-args-gt body 2)
          (transpile-defmacro body ctx))

      "defn"
      (do (utils/assert-args-gt body 2)
          (transpile-fn-decl body ctx))

      "define"
      (do (utils/assert-args-bt 1 2)
          (fmt "#define ~A ~A" (transpile-first body ctx) (transpile-nth body 1 "" ctx)))

      "def"
      (do (utils/assert-args-eq body 2)
          (transpile-def body ctx))

      "ptr"
      (do (utils/assert-args-eq body 1)
          (fmt "*~A" (transpile-last ast ctx)))

      "arr"
      (do (utils/assert-args-bt body 1 2)
          (fmt "~A[~A]" (transpile-first body ctx) (transpile-nth body 1 "" ctx)))

      "return"
      (do (utils/assert-args-eq body 1)
          (fmt "return ~A" (transpile-first body ctx)))

      "do"
      (do (utils/assert-args-ge body 1)
          (fmt "~A;" (transpile-list body ";\n" ctx)))

      "if"
      (do (utils/assert-args-bt body 2 3)
          (transpile-if body ctx))

      "when"
      (do (utils/assert-args-ge body 2)
          (transpile-when body ctx))

      "case"
      (do (utils/assert-args-ge body 2)
          (transpile-switch body ctx))

      "break"
      (do (utils/assert-args-eq body 0)
          "break")

      "ifdef"
      (do (utils/assert-args-eq body 2 3)
          (fmt "#ifdef ~A" (transpile-first body ctx)))

      "header"
      (do (utils/assert-args-eq body 1)
          (fmt "#include <~A.h>" (transpile-first body ctx)))

      "header-local"
      (do (utils/assert-args-eq body 1)
          (fmt "#include \"~A.h\"" (transpile-first body ctx)))

      "header-raw"
      (do (utils/assert-args-eq body 1)
          (fmt "#include ~A" (transpile-first body ctx)))

      "->"
      (do (utils/assert-args-ge body 1)
          (transpile-thread-first body ctx))

      "="
      (do (utils/assert-args-eq body 2)
          (fmt "~A == ~A" (transpile-first body ctx) (transpile-last body ctx)))

      (transpile-fn-call ast ctx))))

(defn- transpile-ast [ast ctx]
  (cond
    (boolean? ast) (str ast)
    (keyword? ast) (str ast)
    (symbol? ast) (-> ast name utils/sanitize-symbol)
    (number? ast) (str ast)

    (list? ast) (transpile-call ast ctx)
    (seq? ast) (transpile-call ast ctx)

    (string? ast)
    (fmt "\"~A\"" ast)

    (vector? ast)
    (fmt "{ ~A }" (transpile-list ast ", " ctx))

    (map? ast)
    (fmt "{ ~A }"
         (->>
           ast
           (map #(fmt "[~A] = ~A"
                      (-> % first (transpile-ast ctx))
                      (-> % last (transpile-ast ctx))))
           (string/join ", ")))

    :else (throw (Exception. (fmt "Fall through (~S): ~S" (type ast) ast)))))

; Main C transpiler
(defn transpile-c [ast]
  (transpile-list ast "\n" (ctx/ctx)))

