(ns utils
  (:require [clojure.pprint :refer [cl-format]]
            [clojure.string :refer [join replace] :rename {replace str-replace}]))

(defn fmt [f & args]
  (apply cl-format nil f args))

(defn ast-assert [c m] (when (not c) (throw (Exception. m))))
(defn assert-type [ast f tn] (ast-assert (f ast) (fmt "Expected type ~A:~%~S" tn ast))) 
(defn assert-args-eq [ast n] (ast-assert (= n (count ast)) (fmt "Expected ~D arg~:P:~%~S" n ast))) 
(defn assert-args-lt [ast n] (ast-assert (> n (count ast)) (fmt "Expected less than ~D arg~:P:~%~S" n ast))) 
(defn assert-args-le [ast n] (ast-assert (>= n (count ast)) (fmt "Expected ~D arg~:P or less:~%~S" n ast))) 
(defn assert-args-gt [ast n] (ast-assert (< n (count ast)) (fmt "Expected more than ~D arg~:P:~%~S" n ast))) 
(defn assert-args-ge [ast n] (ast-assert (<= n (count ast)) (fmt "Expected ~D arg~:P or more:~%~S" n ast))) 
(defn assert-args-bt [ast n x] (ast-assert (and (<= n (count ast)) (>= x (count ast))) (fmt "Expected between ~D and ~D arg~:P:~%~S" n x ast)))

(defn sanitize-symbol [fn]
  (-> fn
   (str-replace #"-([^>])" "_$1")))

