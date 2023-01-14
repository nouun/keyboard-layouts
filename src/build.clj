(ns build
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :refer [difference union]]
            [clojure.string :as string]
            [conch :refer [programs] :as sh]
            [transpiler.cljc :refer [transpile]]
            [utils :refer [fmt]]))

(programs qmk)

(defn- kb-dir [keyboard]
  (fmt "./keyboards/~A/" keyboard))

(defn- build [path config flags]
  (println (fmt " -- Building '~A:~A'" (:keyboard config) (:layout config))))

(defn- compile-keymap [config flags]
  (let [p (sh/proc "qmk" (if (:flash flags) "flash" "compile") "-kb" (:keyboard config) "-km" (:layout config))
        err (future (sh/stream-to-out p :out))
        out (future (sh/stream-to-out p :err))
        exit-code (sh/exit-code p)]
    (if (zero? exit-code)
      (println "\n   --" (if (:flash flags) "Flashed" "Compiled") "successfully")
      (throw (Exception. "QMK error occured, please refer to output above")))))

(defn- compile-cljc [kb-path out]
  (defn compile-files [path out]
    (let [files (->> path io/file file-seq (filter #(or (string/ends-with? % ".cljc")
                                                        (string/ends-with? % ".cljh"))))]
      (println (fmt "   -- Compiling ~D file~:*~P from ~A" (count files) path))
      (doall
        (for [file (vec files)]
         (let [[out-name extension] (-> file .getName (string/split #"\.(?!.*\.)"))
               out-name (str out-name "." (if (= "cljc" extension) "c" "h"))
               in-file (.getAbsolutePath file)
               out-file (-> out (io/file out-name) .getAbsolutePath)]
           (println "     -- Compiling" (.getName file) "to" out-name)
           (transpile in-file out-file))))))

  (compile-files kb-path out)
  (compile-files "./shared/" out))

(defn- compile-rules [qmk-path config]
  (defn fmt-features [config enabled]
    (->> config
        enabled
        (map #(fmt "~A_ENABLE = ~A"
                   (-> % name string/upper-case (string/replace #"-" "_"))
                   (if (= :enabled enabled) "yes" "no")))
        (string/join "\n")))

  (let [out-file (-> qmk-path (io/file "rules.mk") .getAbsolutePath)
        enabled-features (fmt-features config :enabled)
        disabled-features (fmt-features config :disabled)
        files (->> config :files (map #(fmt "SRC += ~A" %)) (string/join "\n"))]
    (spit out-file (fmt "~A~%~A~%~%~A" enabled-features disabled-features files))
    (println "   -- Created 'rules.mk'")))

(defn- setup-qmk [path config]
  (defn get-qmk-dir []
    (let [out (qmk "config" "user.qmk_home")]
      (if (-> out count zero?)
        (throw (Exception. "QMK 'user.qmk_home' not setup. https://docs.qmk.fm/#/newbs_getting_started"))
        (let [path (-> out (string/split #"=") last string/trim)
              file (io/file path)]
          (if (not (and (.exists file)
                        (.isDirectory file)))
            (throw (Exception. (fmt "QMK 'user.qmk_home' does not point to a directory: '~A'" path)))
            path)))))

  (defn delete-recursively [f]
    (let [func (fn [func f]
                 (when (.isDirectory f)
                   (doseq [f2 (.listFiles f)]
                     (func func f2)))
                 (io/delete-file f))]
      (func func (io/file f))))

  (println "   -- Getting QMK directory")
  (let [qmk-dir (get-qmk-dir)
        kb-dir (io/file qmk-dir "keyboards/" (:keyboard config) "keymaps/" (:layout config))
        kb-path (.getAbsolutePath kb-dir)]
    (when (-> kb-dir .exists)
      (println "   -- Removing old keyboard layout")
      (delete-recursively kb-path))

    (when (-> kb-dir .exists not)
      (println (fmt "   -- Creating QMK_HOME/keyboards/~A/keymaps/~A/" (:keyboard config) (:layout config)))
      (.mkdirs kb-dir))

    kb-path))

(defn- pre-build [keyboard]
  (defn- load-config [path]
    (try
      (with-open [r (io/reader (str path "config.edn"))]
        (edn/read (java.io.PushbackReader. r)))

      (catch java.io.IOException e
        (throw (Exception. (fmt "Unable to open config.edn in ~A: ~A~%"
                                path (.getMessage e)))))
      (catch RuntimeException e
        (throw (Exception. (fmt "Error parsing config.edn in ~A: ~A~%"
                                path (.getMessage e)))))))

  (defn merge-config [board shared]
    (defn parse-features [board shared]
      (defn get-features [config enabled]
        (-> config :features ((if enabled :enabled :disabled)) (or []) set))

      (let [bef (get-features board true)
            bdf (get-features board false)]
        {:enabled (vec (union bef (-> shared (get-features true) (difference bdf))))
         :disabled (vec (union bdf (-> shared (get-features false) (difference bef))))}))
    (let [features (parse-features board shared)]
      (merge
        features
        {:keyboard (:keyboard board)
         :layout (:layout board)
         :files (->> [(-> board :files (or []))
                      (-> shared :files (or []))]
                    (apply concat) vec)})))

  (let [kb-path (kb-dir keyboard)
        config (merge-config (load-config kb-path)
                             (load-config "./shared/"))]
    [kb-path config]))

(defn- build [keyboard flags]
  (println " -- Loading config.edn")
  (let [[kb-path config] (pre-build keyboard)]
    (cond
      (-> config :keyboard nil?)
      (throw (Exception. "Missing :keyboard in config.edn"))

      (-> config :layout nil?)
      (throw (Exception. "Missing :layout in config.edn")))

    (println " -- Setting up QMK")
    (let [qmk-kb-path (setup-qmk kb-path config)]
      (println " -- Generating rules.mk")
      (compile-rules qmk-kb-path config)

      (println " -- Compiling cljc files")
      (compile-cljc kb-path qmk-kb-path)

      (println (fmt " -- Compiling~A keymap" (if (:flash flags) " and flashing" "")))
      (compile-keymap config flags))))

(defn- check-keyboard [keyboard]
  (let [kb (string/trim keyboard)
        file (io/file "./keyboards/" kb)]
    (and (not (zero? (count kb)))
         (.exists file)
         (.isDirectory file))))

(defn- print-help []
  (println "usage: clj -M -m build [keyboard] [-fh]"))

(defn- parse-args [args]
  (defn is-arg [k]
    (-> #{(str "-" k)} (some args) nil? not))

  (if (is-arg "h") nil
    {:flash (is-arg "f")}))

(defn -main [& args]
  (let [[keyboard & arg-flags] args
        flags (parse-args arg-flags)]
    (if (or (nil? keyboard)
            (nil? flags))
      (print-help)
      (if (not (check-keyboard keyboard))
        (println (fmt (str "Keyboard '~A' does not exist. "
                           "Check ./keyboards/ for a list of valid keyboards.")
                      keyboard))
       ;(try
        (build keyboard flags)))
       ; (catch Exception e
       ;  (println " !!" (.getMessage e))))))
    (shutdown-agents)))
