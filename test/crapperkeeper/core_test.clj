(ns crapperkeeper.core-test
  (:require [clojure.test :refer :all]
            [crapperkeeper.core :refer :all]
            [crapperkeeper.internal :as internal]
            [slingshot.slingshot :refer [try+]]
            [schema.test :as schema-test])
  (:import (crapperkeeper.schemas ServiceInterface)))

(use-fixtures :once schema-test/validate-schemas)

; This may be terrible
(defn reset-tk-state!
  []
  (reset! internal/services-atom nil)
  (reset! internal/contexts-atom {}))

(defmacro with-tk
  [services config & body]
  `(try
     (boot! ~services ~config)
     ~@body
     (finally
       (reset-tk-state!))))

(def HelloService
  (ServiceInterface. :hello-service #{:hello}))

(def hello-service
  {:implements  HelloService
   :service-fns {:hello (fn [context]
                        "hello world")}})

(def InvalidService nil)

(deftest simplest-service-test
  (with-tk [hello-service] {}
           (is (= (service-call HelloService :hello)
                  "hello world"))))

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
    (try
      (boot! [service])
      (is (= @results #{"init ran" "start ran"}))
      (shutdown!)
      (is (= @results #{"init ran" "start ran" "stop ran"}))
      (finally
        (reset-tk-state!)))))

(deftest config-test
  (testing "a service can read Trapperkeeper's configuration data"
    (let [result (atom nil)
          extract-config (fn [context]
                           (reset! result
                                   (get-in context [:config :foo])))
          service {:lifecycle-fns {:init extract-config}}]
      (with-tk [service] {:foo "bar"}
               (is (= "bar" @result))))))

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
      (with-tk [hello-service bar-service] {}
               (is (= "hello world and mars" @result))))))

(deftest dependency-order-test
  (let [result (atom {})
        service-1-init (fn [context]
                         (swap! result assoc :service-1
                                {:init?            true
                                 :service-2-init?  (get-in @result [:service-2 :init?])}))
        service-2-init (fn [context]
                         (swap! result assoc :service-2
                                {:init?            true
                                 :service-1-init?  (get-in @result [:service-1 :init?])}))
        service-1 {:implements HelloService
                   :service-fns {:hello (fn [context]
                                          "Hello again")}
                   :lifecycle-fns {:init service-1-init}}
        service-2 {:dependencies #{HelloService}
                   :lifecycle-fns {:init service-2-init}}]
    (with-tk [service-1 service-2] {}
             (testing "both services should have booted"
               (is (get-in @result [:service-1 :init?]))
               (is (get-in @result [:service-2 :init?])))
             (testing "1 should have booted before 2"
               (is (not (get-in @result [:service-1 :service-2-init?])))
               (is (get-in @result [:service-2 :service-1-init?]))))

    (testing "the order in which the services are specified should not matter"
      (reset! result {})
      (with-tk [service-2 service-1] {}
               (testing "both services should have booted"
                 (is (get-in @result [:service-1 :init?]))
                 (is (get-in @result [:service-2 :init?])))
               (testing "1 should have booted before 2"
                 (is (not (get-in @result [:service-1 :service-2-init?])))
                 (is (get-in @result [:service-2 :service-1-init?])))))))
