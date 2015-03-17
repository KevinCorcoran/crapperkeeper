(ns crapperkeeper.core-test
  (:require [clojure.test :refer :all]
            [crapperkeeper.core :refer :all]
            [slingshot.slingshot :refer [try+]])
  (:import (crapperkeeper.core ServiceInterface)))

(def FooService
  (ServiceInterface. #{:foo}))

(def InvalidService nil)

(defn schema-error? [x] (= (:type x) :schema.core/error))

(deftest simplest-service-test
  (let [service {:implements  'FooService
                 :service-fns {:foo (fn [context]
                                      "hello from foo")}}]
    (boot! service))
  (is (= (service-call 'FooService :foo)
         "hello from foo")))

(deftest invalid-service-test
  (testing ":implements value is not a ServiceInterface"
    (let [service {:implements  InvalidService
                   :service-fns {:foo (fn [context]
                                        "hello from foo")}}
          got-expected-exception? (atom false)]
      (try+
        (boot! service)
        (catch schema-error? _
          (reset! got-expected-exception? true)))
      (is @got-expected-exception?)))

  ; Lots of more cases that could be added here, but we're really just
  ; testing schema validation.
  )
