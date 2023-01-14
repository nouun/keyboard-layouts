(ns transpiler.h
  (:require [clojure.string :as string]
            [utils :as utils :refer [fmt]]))

(declare transpile-ast)

(defn- transpile-first [ast]
  (-> ast first transpile-ast))

(defn- transpile-last [ast]
  (-> ast last transpile-ast))

(defn- transpile-nth [ast n]
  (if (< n (count ast))
    (transpile-ast (nth ast n))
    ""))

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
        _ (utils/assert-args-eq ast (if is-typed 3 2))
        fn-type (if is-typed (transpile-ast type-arg) "void")
        args (-> (if is-typed (-> ast rest rest first) type-arg) transpile-fn-decl-args)]
    (fmt "~A ~A(~A);" fn-type fn args)))

(defn- transpile-def [ast]
  (let [def-name (transpile-first ast)
        [typing value] (last ast)]
    (case (transpile-ast typing)
      "type"
      (do (utils/assert-type value symbol? "symbol")
          (fmt "typedef ~A ~A;" (transpile-ast value) def-name))

      "enum"
      (do (utils/assert-type value vector? "vector")
          (let [typedef (string/upper-case def-name)
                enums   (->> value
                             (map #(if (list? %)
                                     (fmt "~A = ~A" (transpile-first %) (transpile-last %))
                                     (transpile-ast %)))
                             (string/join ", "))]
            (fmt "typedef enum { ~A } ~A;~%extern enum ~:*~A ~A;" enums typedef def-name)))

      (throw (Exception. (fmt "Unable to define ~A." typing))))))

(defn- transpile-call [ast]
  (let [sym (-> ast first name)
        body (-> ast rest)]
    (case sym
      "comment"
      (do (fmt "~%// ~A" (string/replace (string/join "\n" body) #"\n" "\n// ")))

      "defn"
      (do (utils/assert-args-bt body 2 3)
          (transpile-fn-decl body))

      "def"
      (do (utils/assert-args-eq body 2)
          (transpile-def body))

      "pragma"
      (do (utils/assert-args-eq body 1)
          (fmt "#pragma ~A" (transpile-first body)))

      "define"
      (do (utils/assert-args-bt body 1 2)
          (fmt "#define ~A ~A" (transpile-first body) (transpile-nth body 1)))

      "defines"
      (do (utils/assert-args-ge body 1)
          (->> body
               (map #(fmt "#define ~A ~A" (transpile-first %) (transpile-nth % 1)))
               (string/join "\n")))

      "ifdef"
      (do (utils/assert-args-bt body 2 3)
          (fmt "#ifdef ~A~%~A~%~A#endif"
               (transpile-first body) (transpile-nth body 1)
               (if (-> body count (= 2)) ""
                 (fmt "#else~%~A~%" (transpile-nth body 2)))))

      "header"
      (do (utils/assert-args-eq body 1)
          (fmt "#include <~A.h>" (transpile-first body)))

      "header-local"
      (do (utils/assert-args-eq body 1)
          (fmt "#include \"~A.h\"" (transpile-first body)))

      "header-raw"
      (do (utils/assert-args-eq body 1)
          (fmt "#include ~A" (transpile-first body)))

      "ptr"
      (do (utils/assert-args-eq body 1)
          (fmt "*~A" (transpile-last ast)))

      "arr"
      (do (utils/assert-args-bt body 1 2)
          (fmt "~A[~A]" (transpile-first body) (transpile-nth body 1)))

      (transpile-fn-call ast))))

(defn- transpile-ast [ast]
  (cond
    (symbol? ast) (-> ast name utils/sanitize-symbol)
    (number? ast) (str ast)
    (string? ast) (fmt "\"~A\"" ast)
    (vector? ast) (fmt "{ ~A }" (transpile-list ast ", "))
    (map? ast) (fmt "{ ~A }" (->> ast (map #(fmt "[~A] = ~A" (-> % first transpile-ast) (-> % last transpile-ast))) (string/join ", ")))
    (list? ast) (transpile-call ast)

    :else (throw (Exception. (fmt "Fall through (~S): ~S" (type ast) ast)))))

; Main H transpiler
(defn transpile-h [ast output-file]
  (let [header (-> output-file (string/split #"/") last (string/replace #"[^a-zA-Z]" "_") (string/upper-case))
        h-code (transpile-list ast "\n")]
    (fmt "#ifndef ~A~%#define ~:*~A~%~%~A~%~%#endif // ~@*~A" header h-code)))
  

