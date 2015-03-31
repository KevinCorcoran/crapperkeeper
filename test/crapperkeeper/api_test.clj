(ns crapperkeeper.api-test
  (:require [clojure.test :refer :all]
            [crapperkeeper :refer :all]
            [crapperkeeper.fixtures :refer :all]
            [crapperkeeper.test-utils :refer [with-tk with-services]]
            [slingshot.slingshot :refer [try+]]
            [schema.test :as schema-test]))

(use-fixtures :once schema-test/validate-schemas)


(deftest simplest-service-test
  (with-services [foo-service]
    (is (= (service-call FooService :foo) "hello from foo"))))

(deftest lifecycle-test
  (let [results (atom #{})
        service {:init  (fn [context]
                          (swap! results conj "init ran"))
                 :start (fn [context]
                          (swap! results conj "start ran"))
                 :stop  (fn [context]
                          (swap! results conj "stop ran"))}]
    (with-services [service]
      (is (= @results #{"init ran" "start ran"}))
      (shutdown!)
      (is (= @results #{"init ran" "start ran" "stop ran"})))))

(deftest context-test
  (let [service {:implements  FooService
                 :init        (fn [context]
                                (assoc context :init? true))
                 :service-fns {:foo identity}}]
    (with-services [service]
                   (is (:init? (service-call FooService :foo))))))

(deftest config-test
  (testing "a service can read Trapperkeeper's configuration data"
    (let [result (atom nil)
          extract-config (fn [context]
                           (reset! result
                                   (get-in context [:config :foo])))
          service {:init extract-config}]
      (with-tk [service] {:foo "bar"})
      (is (= "bar" @result)))))

(deftest service-dependency-test
  (testing "a service can express a dependency on another service"
    (let [result (atom nil)
          init-fn (fn [context]
                    (reset! result
                            (str (service-call FooService :foo)
                                 " moo")))
          bar-service {:dependencies  #{FooService}
                       :init          init-fn}]
      (with-services [foo-service bar-service]
        (is (= "hello from foo moo" @result))))))

(deftest dependency-order-test
  (let [result (atom [])
        service-1 {:implements    FooService
                   :service-fns   {:foo (fn [context]
                                          "Hello again")}
                   :init          (fn [context]
                                    (swap! result conj 1))}
        service-2 {:dependencies  #{FooService}
                   :init          (fn [context]
                                    (swap! result conj 2))}]

    (with-services [service-1 service-2]
      (testing "1 should have booted before 2"
        (is @result [1 2])))

    (testing "the order in which the services are specified should not matter"
      (reset! result [])
      (with-services [service-1 service-2]
        (testing "1 should have booted before 2"
          (is @result [1 2]))))))

(deftest optional-dependency-test
  (testing "A service should be able to specify optional dependencies"
    (let [my-service {:implements            TestService
                      :optional-dependencies #{FooService}
                      :service-fns           {:test (fn [context]
                                                      (service-call FooService :foo))}}]
      (testing "service calls work as normal when the dependency is available"
        (with-services [my-service foo-service]
          (is (= "hello from foo" (service-call TestService :test)))))

      (testing "service calls return return nil when the dependency is unavailable"
        (is (nil? (service-call TestService :test)))))))
