(ns transpiler.cljc
  (:require [clojure.string :refer [ends-with?]]
            [transpiler.c :refer [transpile-c]]
            [transpiler.h :refer [transpile-h]]
            [utils :refer [fmt]]))

(defn transpile [input-file output-file]
  (let [input (fmt "(~A)" (slurp input-file))
        ast (read-string input)]
    (cond
      (ends-with? input-file ".cljc")
      (spit output-file (transpile-c ast))

      (ends-with? input-file ".cljh")
      (spit output-file (transpile-h ast output-file))

      :else (throw (Exception. (fmt "Unknown file type ~A. Expecting .cljc and .cljh file extension." input-file))))))

(defn -main [& args]
  (let [[input-file output-file] args]
    (transpile input-file output-file)
    (println "Wrote output to" output-file)))

