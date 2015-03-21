(ns crapperkeeper.core-test
  (:require [clojure.test :refer :all]
            [crapperkeeper.core :refer :all]
            [crapperkeeper.fixtures :refer :all]
            [crapperkeeper.test-utils :refer [with-tk with-services]]
            [slingshot.slingshot :refer [try+]]
            [schema.test :as schema-test]))

(use-fixtures :once schema-test/validate-schemas)


(deftest simplest-service-test
  (with-services [hello-service]
    (is (= (service-call HelloService :hello)
           "hello world"))))

(deftest lifecycle-test
  (let [results (atom #{})
        service {:lifecycle-fns {:init  (fn [context]
                                          (swap! results conj "init ran"))
                                 :start (fn [context]
                                          (swap! results conj "start ran"))
                                 :stop  (fn [context]
                                          (swap! results conj "stop ran"))}}]
    (with-services [service]
      (is (= @results #{"init ran" "start ran"}))
      (shutdown!)
      (is (= @results #{"init ran" "start ran" "stop ran"})))))

(deftest context-test
  (let [service {:implements HelloService
                 :lifecycle-fns {:init (fn [context]
                                         (assoc context :init? true))}
                 :service-fns {:hello identity}}]
    (with-services [service]
      (is (:init? (service-call HelloService :hello))))))

(deftest config-test
  (testing "a service can read Trapperkeeper's configuration data"
    (let [result (atom nil)
          extract-config (fn [context]
                           (reset! result
                                   (get-in context [:config :foo])))
          service {:lifecycle-fns {:init extract-config}}]
      (with-tk [service] {:foo "bar"})
      (is (= "bar" @result)))))

(deftest service-dependency-test
  (testing "a service can express a dependency on another service"
    (let [result (atom nil)
          init-fn (fn [context]
                    (reset! result
                            (str (service-call HelloService :hello)
                                 " and mars")))
          bar-service {:dependencies  #{HelloService}
                       :lifecycle-fns {:init init-fn}}]
      (with-services [hello-service bar-service]
        (is (= "hello world and mars" @result))))))

(deftest dependency-order-test
  (let [result (atom [])
        service-1 {:implements HelloService
                   :service-fns {:hello (fn [context]
                                          "Hello again")}
                   :lifecycle-fns {:init (fn [context]
                                           (swap! result conj 1))}}
        service-2 {:dependencies #{HelloService}
                   :lifecycle-fns {:init (fn [context]
                                           (swap! result conj 2))}}]

    (with-services [service-1 service-2]
      (testing "1 should have booted before 2"
        (is @result [1 2])))

    (testing "the order in which the services are specified should not matter"
      (reset! result [])
      (with-services [service-1 service-2]
        (testing "1 should have booted before 2"
          (is @result [1 2]))))))
