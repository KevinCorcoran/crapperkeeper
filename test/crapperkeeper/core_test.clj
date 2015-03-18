(ns crapperkeeper.core-test
  (:require [clojure.test :refer :all]
            [crapperkeeper.core :refer :all]
            [slingshot.slingshot :refer [try+]]
            [schema.test :as schema-test])
  (:import (crapperkeeper.schemas ServiceInterface)))

(use-fixtures :once schema-test/validate-schemas)

(def HelloService
  (ServiceInterface. :hello-service #{:hello}))

(def hello-service
  {:implements  HelloService
   :service-fns {:hello (fn [context]
                        "hello world")}})

(def InvalidService nil)

(deftest simplest-service-test
  (boot! [hello-service])
  (is (= (service-call HelloService :hello)
         "hello world")))

(deftest invalid-service-test
  (testing ":implements value is not a ServiceInterface"
    (let [service {:implements  InvalidService
                   :service-fns {:foo (fn [context] nil)}}
          got-expected-exception? (atom false)]
      (try+
        (boot! [service])
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

(deftest required-config-test
  (testing "a service can define a schema for its required configuration"
    (let [service {:lifecycle-fns {:init (fn [context] nil)}
                   :config-schema {:webserver {:host String
                                               :port Integer}}}
          got-expected-exception? (atom false)]
      (testing "empty config"
        (try+
          (boot! [service])
          (catch [:type :crapperkeeper/invalid-config-error] _
            (reset! got-expected-exception? true)))
        (is @got-expected-exception?)))))

(deftest service-dependency-test
  (testing "a service can express a dependency on another service"
    (let [result (atom nil)
          init-fn (fn [context]
                    (reset! result
                            (str (service-call HelloService :hello)
                                 " and mars")))
          bar-service {:dependencies  #{HelloService}
                       :lifecycle-fns {:init init-fn}}]
      (boot! [hello-service bar-service])
      (is (= "hello world and mars" @result)))))
