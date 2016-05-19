(ns hara.data.complex
  (:require [hara.common.checks :refer [hash-map?]]
            [hara.common.error :refer [error suppress]]
            [hara.data.combine :refer [combine decombine]]
            [hara.expression.shorthand :refer [check?->]]
            [clojure.set :as set]))

(defn assocs
  "Similar to `assoc` but conditions of association is specified
   through `sel` (default: `identity`) and well as merging specified
   through `func` (default: `combine`).
   (assocs {:a #{1}} :a #{2 3 4})
   => {:a #{1 2 3 4}}

   (assocs {:a {:id 1}} :a {:id 1 :val 1} :id merge)
   => {:a {:val 1, :id 1}}

   (assocs {:a #{{:id 1 :val 2}
                 {:id 1 :val 3}}} :a {:id 1 :val 4} :id merges)
   => {:a #{{:id 1 :val #{2 3 4}}}}"
  {:added "2.1"}
  ([m k v] (assocs m k v identity combine))
  ([m k v sel func]
   (let [z (get m k)]
     (cond (nil? z) (assoc m k v)
           :else
           (assoc m k (combine z v sel func))))))

(defn dissocs
  "Similar to `dissoc` but allows dissassociation of sets of values from a map.

   (dissocs {:a 1} :a)
   => {}

   (dissocs {:a #{1 2}} [:a #{0 1}])
   => {:a #{2}}

   (dissocs {:a #{1 2}} [:a #{1 2}])
   => {}"
  {:added "2.1"}
  [m k]
  (cond (vector? k)
        (let [[k v] k
              z (get m k)
              res (decombine z v)]
          (if (nil? res)
            (dissoc m k)
            (assoc m k res)))
        :else
        (dissoc m k)))

(defn gets
  "Returns the associated values either specified by a key or a key and predicate pair.

   (gets {:a 1} :a) => 1

   (gets {:a #{0 1}} [:a zero?]) => #{0}

   (gets {:a #{{:b 1} {}}} [:a :b]) => #{{:b 1}}"
  {:added "2.1"}
  [m k]
  (if-not (vector? k)
    (get m k)
    (let [[k prchk] k
          val (get m k)]
      (if-not (set? val) val
              (-> (filter #(check?-> % prchk) val) set)))))

(defn merges
  "Like `merge` but works across sets and will also
   combine duplicate key/value pairs together into sets of values.

    (merges {:a 1} {:a 2})
    => {:a #{1 2}}

    (merges {:a #{{:id 1 :val 1}}}
            {:a {:id 1 :val 2}}
            :id merges)
    => {:a #{{:id 1 :val #{1 2}}}}"
  {:added "2.1"}
  ([m1 m2] (merges m1 m2 identity combine))
  ([m1 m2 sel] (merges m1 m2 sel combine))
  ([m1 m2 sel func]
   (reduce-kv (fn [out k v]
                (assoc out k (combine (get out k) v sel func)))
              m1
              m2)))

(defn merges-nested
  "Like `merges` but works on nested maps

    (merges-nested {:a {:b 1}} {:a {:b 2}})
    => {:a {:b #{1 2}}}

    (merges-nested {:a #{{:foo #{{:bar #{{:baz 1}}}}}}}
               {:a #{{:foo #{{:bar #{{:baz 2}}}}}}}
               hash-map?
               merges-nested)
    => {:a #{{:foo #{{:bar #{{:baz 2}}}
                     {:bar #{{:baz 1}}}}}}}"
  {:added "2.1"}
  ([] nil)
  ([m] m)
  ([m1 m2] (merges-nested m1 m2 identity combine))
  ([m1 m2 sel] (merges-nested m1 m2 sel combine))
  ([m1 m2 sel func]
   (reduce-kv (fn [out k v]
                (let [v1 (get out k)]
                  (cond (not (and (hash-map? v1) (hash-map? v)))
                        (assoc out k (combine v1 v sel func))

                        :else
                        (assoc out k (merges-nested v1 v sel func)))))
              m1
              m2)))

(defn merges-nested*
  "Like `merges-nested but can recursively merge nested sets and values

   (merges-nested* {:a #{{:id 1 :foo
                          #{{:id 2 :bar
                             #{{:id 3 :baz 1}}}}}}}
                   {:a {:id 1 :foo
                        {:id 2 :bar
                         {:id 3 :baz 2}}}}
                  :id)

   => {:a #{{:id 1 :foo
             #{{:id 2 :bar
                #{{:id 3 :baz #{1 2}}}}}}}}"
  {:added "2.1"}
  ([] nil)
  ([m] m)
  ([m1 m2] (merges-nested* m1 m2 hash-map? combine))
  ([m1 m2 sel] (merges-nested* m1 m2 sel combine))
  ([m1 m2 sel func]
   (reduce-kv (fn [out k v]
                (let [v1 (get out k)]
                  (cond (and (hash-map? v1) (hash-map? v))
                        (assoc out k (merges-nested* v1 v sel func))

                        (or (set? v1) (set? v))
                        (assoc out k (combine v1 v sel #(merges-nested* %1 %2 sel func)))

                        :else
                        (assoc out k (func v1 v)))))
              m1
              m2)))

(declare gets-in)

(defn- gets-in-loop
  [m [k & ks :as all-ks]]
  (cond (nil? ks)
        (let [val (gets m k)]
          (cond (set? val) val
                :else (list val)))
        :else
        (let [val (gets m k)]
          (cond (set? val)
                (apply concat (map #(gets-in-loop % ks) val))
                :else (gets-in-loop val ks)))))

(defn gets-in
  "Similar in style to `get-in` with operations on sets. returns a set of values.

   (gets-in {:a 1} [:a]) => #{1}

   (gets-in {:a 1} [:b]) => #{}

   (gets-in {:a #{{:b 1} {:b 2}}} [:a :b]) => #{1 2}"
  {:added "2.1"}
  [m ks]
  (-> (gets-in-loop m ks) set (disj nil)))

(declare assocs-in)

(defn assocs-in-filtered
  ([m all-ks v] (assocs-in-filtered m all-ks v identity combine))
  ([m [[k prchk] & ks :as all-ks] v sel func]
     (let [subm (get m k)]
       (cond (nil? subm) m

             (and (set? subm) (every? hash-map? subm))
             (let [ori-set (set (filter #(check?-> % prchk) subm))
                   new-set (set (map #(assocs-in % ks v sel func) ori-set))]
               (assoc m k (-> subm
                              (set/difference ori-set)
                              (set/union new-set))))

             (hash-map? subm)
             (if (check?-> subm prchk)
               (assoc m k (assocs-in subm ks v sel func))
               m)

             :else (error subm "needs to be hash-map or hash-set")))))

(defn assocs-in
  "Similar to assoc-in but can move through sets

   (assocs-in {:a {:b 1}} [:a :b] 2)
   => {:a {:b #{1 2}}}

   (assocs-in {:a #{{:b 1}}} [:a :b] 2)
   => {:a #{{:b #{1 2}}}}

   (assocs-in {:a #{{:b {:id 1}} {:b {:id 2}}}}
              [:a [:b [:id 1]] :c] 2)
   => {:a #{{:b {:id 1 :c 2}} {:b {:id 2}}}}"
  {:added "2.1"}
  ([m all-ks v] (assocs-in m all-ks v identity combine))
  ([m [k & ks :as all-ks] v sel func]
     (cond (nil? ks)
           (cond (vector? k) (error "cannot allow vector-form on last key " k)
                 (or (nil? m) (hash-map? m)) (assocs m k v sel func)
                 (nil? k) (combine m v sel func)
                 :else (error m " is not an associative map"))

           (or (nil? m) (hash-map? m))
           (cond (vector? k) (assocs-in-filtered m all-ks v sel func)
                 :else
                 (let [val (get m k)]
                   (cond (set? val)
                         (assoc m k (set (map #(assocs-in % ks v sel func) val)))
                         :else (assoc m k (assocs-in val ks v sel func)))))
           :else (error m " is required to be a map"))))

(declare dissocs-in)

(defn- dissocs-in-filtered
  ([m [[k prchk] & ks :as all-ks]]
     (let [subm (get m k)]
       (cond (nil? subm) m
             (and (set? subm) (every? hash-map? subm))
             (let [ori-set (set (filter #(check?-> % prchk) subm))
                   new-set (set (map #(dissocs-in % ks) ori-set))]
               (assoc m k (-> subm
                              (set/difference ori-set)
                              (set/union new-set))))

             (hash-map? subm)
             (if (check?-> subm prchk)
               (assoc m k (dissocs-in subm ks))
               m)

             :else (error subm "needs to be hash-map or hash-set")))))

(defn dissocs-in
  "Similiar to `dissoc-in` but can move through sets.

    (dissocs-in {:a #{{:b 1 :c 1} {:b 2 :c 2}}}
                 [:a :b])
    => {:a #{{:c 1} {:c 2}}}

    (dissocs-in {:a #{{:b #{1 2 3} :c 1}
                      {:b #{1 2 3} :c 2}}}
                [[:a [:c 1]] [:b 1]])
    => {:a #{{:b #{2 3} :c 1} {:b #{1 2 3} :c 2}}}"
  {:added "2.1"}
  [m [k & ks :as all-ks]]
  (cond (nil? ks) (dissocs m k)

        (vector? k) (dissocs-in-filtered m all-ks)

        :else
        (let [val (get m k)]
          (cond (set? val)
                (assoc m k (set (map #(dissocs-in % ks) val)))
                :else (assoc m k (dissocs-in m ks))))))
