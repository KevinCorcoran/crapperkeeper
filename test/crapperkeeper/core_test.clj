(ns crapperkeeper.core-test
  (:require [clojure.test :refer :all]
            [crapperkeeper.core :refer :all]
            [slingshot.slingshot :refer [try+]])
  (:import (crapperkeeper.schemas ServiceInterface)))

(def FooService
  (ServiceInterface. :foo-service #{:foo}))

(def InvalidService nil)

(deftest simplest-service-test
  (let [service {:implements  FooService
                 :service-fns {:foo (fn [context]
                                      "hello from foo")}}]
    (boot! [service]))
  (is (= (service-call FooService :foo)
         "hello from foo")))

(deftest invalid-service-test
  (testing ":implements value is not a ServiceInterface"
    (let [service {:implements  InvalidService
                   :service-fns {:foo (fn [context]
                                        "hello from foo")}}
          got-expected-exception? (atom false)]
      (try+
        (boot! service)
        (catch [:type :schema.core/error] _
          (reset! got-expected-exception? true)))
      (is @got-expected-exception?)))

  ; Lots of more cases that could be added here,
  ; but we're really just testing schema validation.
  )

(deftest lifecycle-test
  (let [results (atom #{})
        service {:lifecycle-fns {:init  (fn [context]
                                          (swap! results conj "init ran"))
                                 :start (fn [context]
                                          (swap! results conj "start ran"))
                                 :stop  (fn [context]
                                          (swap! results conj "stop ran"))}}]
    (boot! [service])
    (is (= @results #{"init ran" "start ran"}))
    (shutdown!)
    (is (= @results #{"init ran" "start ran" "stop ran"}))))

(deftest config-test
  (testing "a service can read Trapperkeeper's configuration data"
    (let [result (atom nil)
          extract-config (fn [context]
                           (reset! result
                                   (get-in context [:config :foo])))
          service {:lifecycle-fns {:init extract-config}}]
      (boot! [service] {:foo "bar"})
      (is (= "bar" @result)))))
