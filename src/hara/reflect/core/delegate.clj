(ns hara.reflect.core.delegate
  (:require [hara.reflect.core.query :as q]))

(deftype Delegate [pointer fields]
  Object
  (toString [self]
    (format "<%s@%s %s>" (.getName ^Class (type pointer)) (.hashCode pointer) (self)))

  clojure.lang.IDeref
  (deref [self] fields)

  java.util.Map
  (equals [self other] (= (self) other))
  (size [self] (count fields))
  (keySet [self] (keys fields))
  (entrySet [self] (set (map (fn [[k f]] (clojure.lang.MapEntry. k (f pointer))) fields)))
  (containsKey [self key] (contains? fields key))
  (values [self] (map (fn [f] (f pointer)) (vals fields)))

  clojure.lang.ILookup
  (valAt [self key]
    (if-let [f (get fields key)]
      (f pointer)))
  (valAt [self key not-found]
    (if-let [f (get fields key)]
      (f pointer)
      not-found))

  clojure.lang.IFn
  (invoke [self]
    (->> fields
         (map (fn [[k f]]
                [k (f pointer)]))
         (into {})))
  (invoke [self key]
    (.valAt self key))
  (invoke [self key value]
    (if-let [f (get fields key)]
      (f pointer value))
    self))

(defn delegate
  "Allow transparent field access and manipulation to the underlying object.
 
   (def a \"hello\")
   (def >a  (delegate a))
 
   (seq (>a :value)) => [\\h \\e \\l \\l \\o]
 
   (>a :value (char-array \"world\"))
   a => \"world\""
  {:added "2.1"}
  [obj]
  (let [fields (->> (map (juxt (comp keyword :name) identity) (q/query-instance obj [:field]))
                    (into {}))]
    (Delegate. obj fields)))

(defmethod print-method Delegate
  [v ^java.io.Writer w]
  (.write w (str v)))
