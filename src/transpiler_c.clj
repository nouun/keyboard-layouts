(ns transpiler-c
  (:require [clojure.string :as string]
            [utils :as utils :refer [fmt]]))

(declare transpile-ast)

(defn- transpile-first [ast]
  (-> ast first transpile-ast))

(defn- transpile-last [ast]
  (-> ast last transpile-ast))

(defn- transpile-nth [ast n default]
  (if (< n (count ast))
    (transpile-ast (nth ast n))
    default))

(defn- transpile-list [ast separator]
  (->> ast (map transpile-ast) (string/join separator)))

(defn- transpile-rest [ast separator]
  (-> ast rest (transpile-list separator)))

(defn- transpile-fn-call [ast]
  (let [fn (transpile-first ast)
        args (transpile-rest ast ", ")]
    (fmt "~A(~A)" fn args)))

(defn- transpile-fn-decl-args [ast]
  (->> ast
       (map (fn [arg]
              (utils/assert-args-eq arg 2)
              (fmt "~A ~A" (transpile-last arg) (transpile-first arg))))
       (string/join ", ")))

(defn- transpile-fn-decl [ast]
  (let [fn (-> ast first transpile-ast)
        type-arg (-> ast rest first)
        is-typed (-> type-arg vector? not)
        _ (utils/assert-args-eq ast (if is-typed 4 3))
        fn-type (if is-typed (transpile-ast type-arg) "void")
        args (-> (if is-typed (-> ast rest rest first) type-arg) transpile-fn-decl-args)
        body (-> (if is-typed (-> ast rest rest rest) (-> ast rest rest)) (transpile-list ";\n"))]
    (fmt "~A ~A(~A){~%~A;~%}" fn-type fn args body)))

(defn- transpile-if [ast]
  (let [conditional (transpile-first ast)
        has-else (= 3 (count ast))
        if-true (->> ast rest first transpile-ast)]
    (str
      (fmt "if(~A) {~%~A~%}" conditional if-true)
      (when has-else
        (fmt " else {~%~A~%}" (->> ast last transpile-ast))))))

(defn- transpile-when [ast]
  (let [conditional (transpile-first ast)
        if-true (-> ast rest (transpile-list ";\n"))]
    (fmt "if(~A) {~%~A;~%}" conditional if-true)))

(defn- transpile-switch [ast]
  (defn assert-mod2 [statements]
    (utils/ast-assert
      (= 0 (mod (count statements) 2))
      (fmt "case requires a key and value pair:~S" ast)))

  (defn map-case [conditional]
    (fmt "case ~A:" (transpile-ast conditional)))

  (defn map-statement [statement]
    (let [conditional (first statement)
          cases (if (vector? conditional)
                  (->> conditional
                       (map map-case)
                       (string/join "\n"))
                  (map-case conditional))]
      (fmt "~A~%~A;~%break;" cases (-> statement last transpile-ast))))

  (let [conditional (transpile-first ast)
        has-default (-> ast count (mod 2) zero?)
        statements (->> ast
                        rest
                        (#(if has-default (butlast %) %))
                        (partition 2)
                        (map map-statement)
                        (string/join "\n"))
        default (if has-default
                  (transpile-last ast)
                  "break")]
    (fmt "switch(~A) {~%~A~%default: ~A;~%}" conditional statements default)))

(defn- transpile-thread-first [ast]
  (defn thread [value ast]
    (if (list? ast)
      (let [[head tail] (split-at 1 ast)]
        (concat head (list value) tail))
      (list ast value)))

  (let [head (first ast)
        threads (rest ast)]
    (if (-> threads count zero?)
      (transpile-ast (apply list head))
      (let [next-thread (first threads)]
        (transpile-thread-first
          (apply list (concat (list (thread head next-thread))
                             (rest threads))))))))

(defn- transpile-def [ast]
  (defn transpile-def-var [def-name const body]
    (let [modifiers (->> body rest butlast (map transpile-ast) ((fn [a] (if const (cons "const" a) a))) (string/join " "))
          value (transpile-last body)]
      (fmt "~A ~A = ~A;" modifiers def-name value)))

  (let [def-name (transpile-first ast)
        [typing value] (last ast)]
    (case (transpile-ast typing)
      "type"
      (do (utils/assert-type value symbol? "symbol")
          (fmt "typedef ~A ~A;" (transpile-ast value) def-name))

      "enum"
      (do (utils/assert-type value vector? "vector")
          (fmt "enum ~A { ~A };" def-name (transpile-list value ", ")))

      "var"
      (transpile-def-var def-name false (last ast))

      "const"
      (transpile-def-var def-name true (last ast))

      (throw (Exception. (fmt "Unable to define ~A." typing))))))

(defn- transpile-macro [ast]
  (str ast))

(defn- transpile-call [ast]
  (let [sym (-> ast first name)
        body (-> ast rest)]
    (case sym
      "comment"
      (do (fmt "~%// ~A" (string/replace (string/join "\n" body) #"\n" "\n// ")))

      "defclj"
      (do (utils/assert-args-gt body 2)
          (transpile-macro body))

      "defn"
      (do (utils/assert-args-gt body 2)
          (transpile-fn-decl body))

      "define"
      (do (utils/assert-args-bt 1 2)
          (fmt "#define ~A ~A" (transpile-first body) (transpile-nth body 1 "")))

      "def"
      (do (utils/assert-args-eq body 2)
          (transpile-def body))

      "ptr"
      (do (utils/assert-args-eq body 1)
          (fmt "*~A" (transpile-last ast)))

      "arr"
      (do (utils/assert-args-bt body 1 2)
          (fmt "~A[~A]" (transpile-first body) (transpile-nth body 1 "")))

      "return"
      (do (utils/assert-args-eq body 1)
          (fmt "return ~A" (transpile-first body)))

      "do"
      (do (utils/assert-args-ge body 1)
          (fmt "~A;" (transpile-list body ";\n")))

      "if"
      (do (utils/assert-args-bt body 2 3)
          (transpile-if body))

      "when"
      (do (utils/assert-args-ge body 2)
          (transpile-when body))

      "case"
      (do (utils/assert-args-ge body 2)
          (transpile-switch body))

      "break"
      (do (utils/assert-args-eq body 0)
          "break")

      "ifdef"
      (do (utils/assert-args-eq body 2 3)
          (fmt "#ifdef ~A" (transpile-first body)))

      "header"
      (do (utils/assert-args-eq body 1)
          (fmt "#include <~A.h>" (transpile-first body)))

      "header-local"
      (do (utils/assert-args-eq body 1)
          (fmt "#include \"~A.h\"" (transpile-first body)))

      "header-raw"
      (do (utils/assert-args-eq body 1)
          (fmt "#include ~A" (transpile-first body)))

      "->"
      (do (utils/assert-args-ge body 1)
          (transpile-thread-first body))

      (transpile-fn-call ast))))

(defn- transpile-ast [ast]
  (cond
    (boolean? ast) (str ast)
    (keyword? ast) (str ast)
    (symbol? ast) (-> ast name utils/sanitize-symbol)
    (number? ast) (str ast)
    (string? ast) (fmt "\"~A\"" ast)
    (vector? ast) (fmt "{ ~A }" (transpile-list ast ", "))
    (map? ast) (fmt "{ ~A }" (->> ast (map #(fmt "[~A] = ~A" (-> % first transpile-ast) (-> % last transpile-ast))) (string/join ", ")))
    (list? ast) (transpile-call ast)
    (seq? ast) (transpile-call ast)

    :else (throw (Exception. (fmt "Fall through (~S): ~S" (type ast) ast)))))


; Main C transpiler
(defn transpile-c [ast]
  (transpile-list ast "\n"))

