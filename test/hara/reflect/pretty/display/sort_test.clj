(ns hara.reflect.pretty.display.sort-test
  (:use hara.test)
  (:require [hara.reflect.pretty.display.sort :refer :all]))

^{:refer hara.reflect.pretty.display.sort/sort-fn :added "2.1"}
(fact "returns a function taking two inputs `x` and `y`, comparing the outputs after applying `f` to both"

  ((sort-fn :id) {:id 1} {:id 2}) => -1)

^{:refer hara.reflect.pretty.display.sort/sort-terms-fn :added "2.1"}
(fact "This is a little broken, it is supposed to sort on various keys, but currently only works with :name"

  ((sort-terms-fn {:sort-terms nil})
   [{:name 3}{:name 1} {:name 2}])
  => [{:name 1} {:name 2} {:name 3}])
