(ns crapperkeeper.internal-test
  (:require [clojure.test :refer :all]
            [crapperkeeper.internal :refer :all]
            [crapperkeeper.fixtures :refer :all]
            [slingshot.slingshot :refer [try+]]
            [schema.test :as schema-test]))

(use-fixtures :once schema-test/validate-schemas)

(deftest invalid-service-test
  (testing ":implements value is not a ServiceInterface"
    (let [service {:implements  nil
                   :service-fns {:foo (fn [context] nil)}}
          got-expected-exception? (atom false)]
      (try+
        (boot! [service] {})
        (catch [:type :schema.core/error] _
          (reset! got-expected-exception? true)))
      (is @got-expected-exception?)))

  ; Lots of more cases that could be added here,
  ; but we're really just testing schema validation.
  )

(deftest required-config-test
  (testing "a service can define a schema for its required configuration"
    (let [service {:lifecycle-fns {:init (fn [context] nil)}
                   :config-schema {:webserver {:host String
                                               :port Integer}}}
          got-expected-exception? (atom false)]
      (testing "empty config"
        (try+
          (boot! [service] {})
          (catch [:type :crapperkeeper/invalid-config-error] _
            (reset! got-expected-exception? true)))
        (is @got-expected-exception?)))))

(deftest dependency-extraction-test
  (testing "service->dependencies correctly reports the dependencies of a given service"
    (testing "one service, no dependencies"
      (is (empty? (service->dependencies foo-service [foo-service]))))

    (testing "two services w/ a dependency b/w them"
      (let [my-service {:dependencies  #{FooService}
                        :lifecycle-fns {:init identity}}]
        (is (= [[my-service foo-service]]
               (service->dependencies my-service [foo-service my-service])))
        (is (= [[my-service foo-service]]
               (services->dependencies [foo-service my-service])))))))

(deftest optional-dependency-test
  (let [my-service {:optional-dependencies #{FooService}
                    :lifecycle-fns         {:init (fn [_] nil)}}
        result (with-optional-dependencies [my-service foo-service])]
    (is (= result [(assoc my-service :dependencies #{FooService}) foo-service]))))
    #_(is (= (:dependencies result) #{FooService}))
