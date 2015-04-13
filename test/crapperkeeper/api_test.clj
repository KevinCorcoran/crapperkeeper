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
  (with-services app [foo-service]
    (is (= (service-call app FooService :foo) "hello from foo"))))

(deftest lifecycle-test
  (let [results (atom #{})
        service {:init  (fn [app context]
                          (swap! results conj "init ran"))
                 :start (fn [app context]
                          (swap! results conj "start ran"))
                 :stop  (fn [app context]
                          (swap! results conj "stop ran"))}]
    (with-services app [service]
      (is (= @results #{"init ran" "start ran"}))
      (shutdown! app)
      (is (= @results #{"init ran" "start ran" "stop ran"})))))

(deftest context-test
  (let [service {:implements  FooService
                 :init        (fn [app context]
                                (assoc context :init? true))
                 :service-fns {:foo #(-> %2)}}]
    (with-services app [service]
                   (is (:init? (service-call app FooService :foo))))))

(deftest config-test
  (testing "a service can read Trapperkeeper's configuration data"
    (let [result (atom nil)
          extract-config (fn [app context]
                           (reset! result
                                   (get-in context [:config :foo])))
          service {:init extract-config}]
      (with-ck app [service] {:foo "bar"})
      (is (= "bar" @result)))))

(deftest service-dependency-test
  (testing "a service can express a dependency on another service"
    (let [result (atom nil)
          init-fn (fn [app context]
                    (reset! result
                            (str (service-call app FooService :foo)
                                 " moo")))
          bar-service {:dependencies  #{FooService}
                       :init          init-fn}]
      (with-services app [foo-service bar-service]
        (is (= "hello from foo moo" @result))))))

(deftest dependency-order-test
  (let [result (atom [])
        service-1 {:implements    FooService
                   :service-fns   {:foo (fn [app context]
                                          "Hello again")}
                   :init          (fn [app context]
                                    (swap! result conj 1))}
        service-2 {:dependencies  #{FooService}
                   :init          (fn [app context]
                                    (swap! result conj 2))}]

    (with-services app [service-1 service-2]
      (testing "1 should have booted before 2"
        (is @result [1 2])))

    (testing "the order in which the services are specified should not matter"
      (reset! result [])
      (with-services app [service-1 service-2]
        (testing "1 should have booted before 2"
          (is @result [1 2]))))))

(deftest optional-dependency-test
  (testing "A service should be able to specify optional dependencies"
    (let [my-service {:implements            TestService
                      :optional-dependencies #{FooService}
                      :service-fns           {:test (fn [app context]
                                                      (service-call app FooService :foo))}}]
      (testing "service calls work as normal when the dependency is available"
        (with-services app [my-service foo-service]
          (is (= "hello from foo" (service-call app TestService :test)))))

      (testing "service calls return return nil when the dependency is unavailable"
        (with-services app [my-service]
          (is (nil? (service-call app TestService :test))))))))

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
                      :service-fns               {:test (fn [app context]
                                                          (:config context))}}]
      (with-ck app [my-service] {:host "localhost"}
               (is (= {:host "localhost" :port 8080} (service-call app TestService :test)))))))

; TODO need a lot more tests around config transformation error handling and such
