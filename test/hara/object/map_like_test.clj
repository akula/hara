(ns hara.object.map-like-test
  (:use hara.test)
  (:require [hara.object.map-like :refer :all]
            [hara.object.write :as write]))

^{:refer hara.object.map-like/key-selection :added "2.3"}
(fact "selects map based on keys"

  (key-selection {:a 1 :b 2} [:a] nil)
  => {:a 1}

  (key-selection {:a 1 :b 2} nil [:a])
  => {:b 2})

^{:refer hara.object.map-like/extend-map-like :added "2.3"}
(fact "creates an entry for map-like classes"

  (extend-map-like test.DogBuilder
                      {:tag "build.dog"
                       :write {:empty (fn [_] (test.DogBuilder.))}
                       :read :reflect})

  (extend-map-like test.Dog {:tag "dog"
                             :write  {:methods :reflect
                                      :from-map (fn [m] (-> m
                                                            (write/from-map test.DogBuilder)
                                                            (.build)))}
                             :exclude [:species]})
  
  (with-out-str
    (prn (write/from-data {:name "hello"} test.Dog)))
  => "#dog{:name \"hello\"}\n"
  
  (extend-map-like test.Cat {:tag "cat"
                             :write  {:from-map (fn [m] (test.Cat. (:name m)))}
                             :exclude [:species]})
  
  (extend-map-like test.Pet {:tag "pet"
                             :write {:from-map (fn [m] (case (:species m)
                                                         "dog" (write/from-map m test.Dog)
                                                         "cat" (write/from-map m test.Cat)))}})
  
  (with-out-str
    (prn (write/from-data {:name "hello" :species "cat"} test.Pet)))
  => "#cat{:name \"hello\"}\n")


