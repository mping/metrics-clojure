(ns metrics.timers
  (:require [metrics.core :refer [default-registry metric-name]]
            [metrics.utils :refer [get-percentiles desugared-title snapshot]])
  (:import [com.codahale.metrics MetricRegistry Timer]
           java.util.concurrent.TimeUnit))


(defn ^Timer timer
  ([title]
   (timer default-registry title))
  ([^MetricRegistry reg title]
   (.timer reg (metric-name title))))


(defmacro deftimer
  "Define a new Timer metric with the given title.

  The title uses some basic desugaring to let you concisely define metrics:

    ; Define a timer titled \"default.default.foo\" into var foo
    (deftimer foo)
    (deftimer \"foo\")

    ; Define a timer titled \"a.b.c\" into var c
    (deftimer [a b c])
    (deftimer [\"a\" \"b\" \"c\"])
    (deftimer [a \"b\" c])
  "
  ([title]
   (let [[s title] (desugared-title title)]
     `(def ~s (timer '~title))))
  ([^MetricRegistry reg title]
   (let [[s title] (desugared-title title)]
     `(def ~s (timer ~reg '~title)))))

(defn rate-one
  [^Timer m]
  (.getOneMinuteRate m))

(defn rate-five
  [^Timer m]
  (.getFiveMinuteRate m))

(defn rate-fifteen
  [^Timer m]
  (.getFifteenMinuteRate m))

(defn rate-mean
  [^Timer m]
  (.getMeanRate m))

(defn rates
  [^Timer m]
  {1 (rate-one m)
   5 (rate-five m)
   15 (rate-fifteen m)})

(defn mean
  [^Timer t]
  (.getMeanRate t))

(defn std-dev
  [^Timer t]
  (.getStdDev (snapshot t)))

(defn percentiles
  "Returns timing percentiles seen by a timer, in nanoseconds"
  ([^Timer t]
   (percentiles t [0.75 0.95 0.99 0.999 1.0]))
  ([^Timer t ps]
   (get-percentiles t ps)))


(defn ^long number-recorded
  [^Timer t]
  (.getCount t))

(defn largest
  "Returns the greatest timing seen by a timer, in nanoseconds"
  [^Timer t]
  (.getMax (snapshot t)))

(defn smallest
  "Returns the smallest timing seen by a timer, in nanoseconds"
  [^Timer t]
  (.getMin (snapshot t)))

(defn sample
  [^Timer t]
  (.getValues (snapshot t)))


(defmacro time!
  [^Timer t & body]
  `(.time ~(vary-meta t assoc :tag `Timer)
          (proxy [Callable] []
            (call [] (do ~@body)))))

(defn time-fn!
  [^Timer t ^clojure.lang.IFn f]
  (.time t (cast Callable f)))
