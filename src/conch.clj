; A minified version of conch
; https://github.com/Raynes/conch

(ns conch
  (:refer-clojure :exclude [flush read-line])
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import java.util.concurrent.LinkedBlockingQueue)
  (:import (java.util.concurrent TimeUnit TimeoutException)))

(defn proc
  "Spin off another process. Returns the process's input stream,
  output stream, and err stream as a map of :in, :out, and :err keys
  If passed the optional :dir and/or :env keyword options, the dir
  and enviroment will be set to what you specify. If you pass
  :verbose and it is true, commands will be printed. If it is set to
  :very, environment variables passed, dir, and the command will be
  printed. If passed the :clear-env keyword option, then the process
  will not inherit its environment from its parent process."
  [& args]
  (let [[cmd args] (split-with (complement keyword?) args)
        args (apply hash-map args)
        builder (ProcessBuilder. (into-array String cmd))
        env (.environment builder)]
    (when (:clear-env args)
      (.clear env))
    (doseq [[k v] (:env args)]
      (.put env k v))
    (when-let [dir (:dir args)]
      (.directory builder (io/file dir)))
    (when (:verbose args) (apply println cmd))
    (when (= :very (:verbose args))
      (when-let [env (:env args)] (prn env))
      (when-let [dir (:dir args)] (prn dir)))
    (when (:redirect-err args)
      (.redirectErrorStream builder true))
    (let [process (.start builder)]
      {:out (.getInputStream process)
       :in  (.getOutputStream process)
       :err (.getErrorStream process)
       :process process})))

(defn destroy
  "Destroy a process."
  [process]
  (.destroy (:process process)))

;; .waitFor returns the exit code. This makes this function useful for
;; both getting an exit code and stopping the thread until a process
;; terminates.
(defn exit-code
  "Waits for the process to terminate (blocking the thread) and returns
   the exit code. If timeout is passed, it is assumed to be milliseconds
   to wait for the process to exit. If it does not exit in time, it is
   killed (with or without fire)."
  ([process] (.waitFor (:process process)))
  ([process timeout]
   (try
     (.get (future (.waitFor (:process process))) timeout TimeUnit/MILLISECONDS)
     (catch Exception e
       (if (or (instance? TimeoutException e)
               (instance? TimeoutException (.getCause e)))
         (do (destroy process)
             :timeout)
         (throw e))))))

(defn flush
  "Flush the output stream of a process."
  [process]
  (.flush (:in process)))

(defn done
  "Close the process's output stream (sending EOF)."
  [proc]
  (-> proc :in .close))

(defn stream-to
  "Stream :out or :err from a process to an ouput stream.
  Options passed are fed to clojure.java.io/copy. They are :encoding to 
  set the encoding and :buffer-size to set the size of the buffer. 
  :encoding defaults to UTF-8 and :buffer-size to 1024."
  [process from to & args]
  (apply io/copy (process from) to args))

(defn feed-from
  "Feed to a process's input stream with optional. Options passed are
  fed to clojure.java.io/copy. They are :encoding to set the encoding
  and :buffer-size to set the size of the buffer. :encoding defaults to
  UTF-8 and :buffer-size to 1024. If :flush is specified and is false,
  the process will be flushed after writing."
  [process from & {flush? :flush :or {flush? true} :as all}]
  (apply io/copy from (:in process) all)
  (when flush? (flush process)))

(defn stream-to-string
  "Streams the output of the process to a string and returns it."
  [process from & args]
  (with-open [writer (java.io.StringWriter.)]
    (apply stream-to process from writer args)
    (str writer)))

;; The writer that Clojure wraps System/out in for *out* seems to buffer
;; things instead of writing them immediately. This wont work if you
;; really want to stream stuff, so we'll just skip it and throw our data
;; directly at System/out.
(defn stream-to-out
  "Streams the output of the process to System/out"
  [process from & args]
  (apply stream-to process from (System/out) args))

(defn feed-from-string
  "Feed the process some data from a string."
  [process s & args]
  (apply feed-from process (java.io.StringReader. s) args))

(defn read-line
  "Read a line from a process' :out or :err."
  [process from]
  (binding [*in* (io/reader (from process))]
    (clojure.core/read-line)))

(def ^:dynamic *throw*
  "If set to false, exit codes are ignored. If true (default),
   throw exceptions for non-zero exit codes."
  true)

(defprotocol Redirectable
  (redirect [this options k proc]))

(defn byte? [x]
  (and (not (nil? x))
       (= java.lang.Byte (.getClass x))))

(defn test-array
  [t]
  (let [check (type (t []))]
    (fn [arg] (instance? check arg))))

(def byte-array?
  (test-array byte-array))


(defn write-to-writer [writer s is-binary]
  (cond
   (byte? (first s)) (.write writer (byte-array s))
   (or (not is-binary)
       (byte-array? (first s))) (if (char? (first s))
                                  (.write writer (apply str s))
                                  (doseq [x s] (.write writer x)))))

(extend-type java.io.File
  Redirectable
  (redirect [f options k proc]
    (let [s (k proc)
          is-binary (:binary options)]
      (with-open [writer (if is-binary (io/output-stream f) (java.io.FileWriter. f))]
        (write-to-writer writer s is-binary)))))

(extend-type clojure.lang.IFn
  Redirectable
  (redirect [f options k proc]
    (doseq [buffer (get proc k)]
      (f buffer proc))))

(extend-type java.io.Writer
  Redirectable
  (redirect [w options k proc]
    (let [s (get proc k)]
      (write-to-writer w s (:binary options)))))

(defn seqify? [options k]
  (let [seqify (:seq options)]
    (or (= seqify k)
        (true? seqify))))

(extend-type nil
  Redirectable
  (redirect [_ options k proc]
    (let [seqify (:seq options)
          s (k proc)]
      (cond
       (seqify? options k) s
       (byte? (first s)) (byte-array s)
       (byte-array? (first s)) (byte-array (mapcat seq s))
       :else (string/join s)))))

(defprotocol Drinkable
  (drink [this proc]))

(extend-type clojure.lang.ISeq
  Drinkable
  (drink [s proc]
    (with-open [writer (java.io.PrintWriter. (:in proc))]
      (binding [*out* writer]
        (doseq [x s]
          (println x))))
    (done proc)))

(extend-type java.io.Reader
  Drinkable
  (drink [r proc]
    (feed-from proc r)
    (done proc)))

(extend-type java.io.File
  Drinkable
  (drink [f proc]
    (drink (io/reader f) proc)))

(extend-type java.lang.String
  Drinkable
  (drink [s proc]
    (feed-from-string proc s)
    (done proc)))

(defn get-drunk [item proc]
  (drink
   (if (coll? item)
     (seq item)
     item)
   proc))

(defn add-proc-args [args options]
  (if (seq options)
    (apply concat args
           (select-keys options
                        [:redirect-err
                         :env
                         :clear-env
                         :dir]))
    args))

(defn queue-seq [q]
  (lazy-seq
   (let [x (.take q)]
     (when-not (= x :eof)
       (cons x (queue-seq q))))))

(defmulti buffer (fn [kind _ _]
                   (if (number? kind)
                     :number
                     kind)))

(defmethod buffer :number [kind reader binary]
  #(try
     (let [cbuf (make-array (if binary Byte/TYPE Character/TYPE) kind)
           size (.read reader cbuf)]
       (when-not (neg? size)
         (let [result (if (= size kind)
                        cbuf
                        (take size cbuf))]
           (if binary
             (if (seq? result) (byte-array result) result)
             (string/join result)))))
     (catch java.io.IOException _)))

(defn ubyte [val]
   (if (>= val 128)
     (byte (- val 256))
     (byte val)))

(defmethod buffer :none [_ reader binary]
  #(try
     (let [c (.read reader)]
       (when-not (neg? c)
         (if binary
           ;; Return a byte (convert from unsigned value)
           (ubyte c)
           ;; Return a char
           (char c))))
     (catch java.io.IOException _)))

(defmethod buffer :line [_ reader binary]
  #(try
     (.readLine reader)
     (catch java.io.IOException _)))

(defn queue-stream [stream buffer-type binary]
  (let [queue (LinkedBlockingQueue.)
        read-object (if binary stream (io/reader stream))]
    (.start
     (Thread.
      (fn []
        (doseq [x (take-while identity (repeatedly (buffer buffer-type read-object binary)))]
          (.put queue x))
        (.put queue :eof))))
    (queue-seq queue)))

(defn queue-output [proc buffer-type binary]
  (assoc proc
    :out (queue-stream (:out proc) buffer-type binary)
    :err (queue-stream (:err proc) buffer-type binary)))

(defn compute-buffer [options]
  (update-in options [:buffer]
             #(if-let [buffer %]
                buffer
                (if (and (not (:binary options))
                         (or (:seq options)
                             (:pipe options)
                             (ifn? (:out options))
                             (ifn? (:err options))))
                  :line
                  1024))))

(defn exit-exception [verbose]
  (throw (ex-info (str "Program returned non-zero exit code "
                       @(:exit-code verbose))
                  verbose)))

(defn run-command [name args options]
  (let [proc (apply proc name (add-proc-args (map str args) options))
        options (compute-buffer options)
        {:keys [buffer out in err timeout verbose binary]} options
        proc (queue-output proc buffer binary)
        exit-code (future (if timeout
                            (exit-code proc timeout)
                            (exit-code proc)))]
    (when in (future (get-drunk in proc)))
    (let [proc-out (future (redirect out options :out proc))
          proc-err (future (redirect err options :err proc))
          proc-out @proc-out
          proc-err @proc-err
          verbose-out {:proc proc
                       :exit-code exit-code
                       :stdout proc-out
                       :stderr proc-err}
          result (cond
                  verbose verbose-out
                  (= (:seq options) :err) proc-err
                  :else proc-out)]
      ;; Not using `zero?` here because exit-code can be a keyword.
      (if (= 0 @exit-code)
        result
        (cond (and (contains? options :throw)
                   (:throw options))
              (exit-exception verbose-out)

              (and (not (contains? options :throw))
                   *throw*)
              (exit-exception verbose-out)

              :else result)))))

(defn execute [name & args]
  (let [[[options] args] ((juxt filter remove) map? args)]
    (if (:background options)
      (future (run-command name args options))
      (run-command name args options))))

(defn execute [name & args]
  (let [end (last args)
        in-arg (first (filter #(seq? %) args))
        args (remove #(seq? %) args)
        options (when (map? end) end)
        args (if options (drop-last args) args)
        options (if in-arg (assoc options :in in-arg) options)]
    (if (:background options)
      (future (run-command name args options))
      (run-command name args options))))

(defmacro programs
  "Creates functions corresponding to progams on the PATH, named by names."
  [& names]
  `(do ~@(for [name names]
           `(defn ~name [& ~'args]
              (apply execute ~(str name) ~'args)))))

(defn- program-form [prog]
  `(fn [& args#] (apply execute ~prog args#)))

(defn map-nth
  "Calls f on every nth element of coll. If start is passed, starts
   at that element (counting from zero), otherwise starts with zero."
  ([f nth coll] (map-nth f 0 nth coll))
  ([f start nth coll]
   (map #(% %2)
        (concat (repeat start identity)
                (cycle (cons f (repeat (dec nth) identity))))
        coll)))

(defmacro let-programs
  "Like let, but expects bindings to be symbols to strings of paths to
   programs."
  [bindings & body]
  `(let [~@(map-nth #(program-form %) 1 2 bindings)]
     ~@body))

(defmacro with-programs
  "Like programs, but only binds names in the scope of the with-programs call."
  [programs & body]
  `(let [~@(interleave programs (map (comp program-form str) programs))]
     ~@body))
