(ns crapperkeeper.api-test
  (:require [clojure.test :refer :all]
            [crapperkeeper :refer :all]
            [crapperkeeper.fixtures :refer :all]
            [crapperkeeper.test-utils :refer [with-ck with-services]]
            [slingshot.slingshot :refer [try+]]
            [schema.test :as schema-test]
            [schema.core :as schema]))

(use-fixtures :once schema-test/validate-schemas)


(deftest simplest-service-test
  (with-services state [foo-service]
    (is (= (service-call state FooService :foo) "hello from foo"))))

(deftest lifecycle-test
  (let [results (atom #{})
        service {:init  (fn [state context]
                          (swap! results conj "init ran"))
                 :start (fn [state context]
                          (swap! results conj "start ran"))
                 :stop  (fn [state context]
                          (swap! results conj "stop ran"))}]
    (with-services state [service]
      (is (= @results #{"init ran" "start ran"}))
      (shutdown! state)
      (is (= @results #{"init ran" "start ran" "stop ran"})))))

(deftest context-test
  (let [service {:implements  FooService
                 :init        (fn [state context]
                                (assoc context :init? true))
                 :service-fns {:foo #(-> %2)}}]
    (with-services state [service]
                   (is (:init? (service-call state FooService :foo))))))

(deftest config-test
  (testing "a service can read Trapperkeeper's configuration data"
    (let [result (atom nil)
          extract-config (fn [state context]
                           (reset! result
                                   (get-in context [:config :foo])))
          service {:init extract-config}]
      (with-ck state [service] {:foo "bar"})
      (is (= "bar" @result)))))

(deftest service-dependency-test
  (testing "a service can express a dependency on another service"
    (let [result (atom nil)
          init-fn (fn [state context]
                    (reset! result
                            (str (service-call state FooService :foo)
                                 " moo")))
          bar-service {:dependencies  #{FooService}
                       :init          init-fn}]
      (with-services state [foo-service bar-service]
        (is (= "hello from foo moo" @result))))))

(deftest dependency-order-test
  (let [result (atom [])
        service-1 {:implements    FooService
                   :service-fns   {:foo (fn [state context]
                                          "Hello again")}
                   :init          (fn [state context]
                                    (swap! result conj 1))}
        service-2 {:dependencies  #{FooService}
                   :init          (fn [state context]
                                    (swap! result conj 2))}]

    (with-services state [service-1 service-2]
      (testing "1 should have booted before 2"
        (is @result [1 2])))

    (testing "the order in which the services are specified should not matter"
      (reset! result [])
      (with-services state [service-1 service-2]
        (testing "1 should have booted before 2"
          (is @result [1 2]))))))

(deftest optional-dependency-test
  (testing "A service should be able to specify optional dependencies"
    (let [my-service {:implements            TestService
                      :optional-dependencies #{FooService}
                      :service-fns           {:test (fn [state context]
                                                      (service-call state FooService :foo))}}]
      (testing "service calls work as normal when the dependency is available"
        (with-services state [my-service foo-service]
          (is (= "hello from foo" (service-call state TestService :test)))))

      (testing "service calls return return nil when the dependency is unavailable"
        (with-services state [my-service]
          (is (nil? (service-call state TestService :test))))))))

(deftest config-transformationt-test
  (testing "A service should be able to specify a function to transform its configuration data"
    (let [my-service {:config-schema             {(schema/optional-key :port) Long
                                                  :host                       String}
                      :transformed-config-schema {:port Long
                                                  :host String}
                      :config-transformer        (fn [config]
                                                   (if (:port config)
                                                     config
                                                     (assoc config :port 8080)))
                      :implements                TestService
                      :service-fns               {:test (fn [state context]
                                                          (:config context))}}]
      (with-ck state [my-service] {:host "localhost"}
               (is (= {:host "localhost" :port 8080} (service-call state TestService :test)))))))

; TODO need a lot more tests around config transformation error handling and such
