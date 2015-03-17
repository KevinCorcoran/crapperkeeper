(ns crapperkeeper.core-test
  (:require [clojure.test :refer :all]
            [crapperkeeper.core :refer :all]))

(def ServiceA nil) ; TODO

(def service-a
  {:implements 'ServiceA
   :service-fns {:foo (fn [context]
                        "hello from foo")}})

(deftest a-test
  (boot! service-a)
  (is (= (service-call 'ServiceA :foo)
         "hello from foo")))
