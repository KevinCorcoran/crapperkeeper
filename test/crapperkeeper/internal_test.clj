(ns crapperkeeper.internal-test
  (:require [clojure.test :refer :all]
            [crapperkeeper.internal :refer :all]
            [crapperkeeper.fixtures :refer :all]
            [schema.test :as schema-test]))

(use-fixtures :once schema-test/validate-schemas)

(deftest dependency-extraction-test
  (testing "service->dependencies correctly reports the dependencies of a given service"
    (testing "one service, no dependencies"
      (is (empty? (service->dependencies hello-service [hello-service]))))

    (testing "two services w/ a dependency b/w them"
      (let [my-service {:dependencies  #{HelloService}
                        :lifecycle-fns {:init identity}}]
        (is (= [[my-service hello-service]]
               (service->dependencies my-service [hello-service my-service])))
        (is (= [[my-service hello-service]]
               (services->dependencies [hello-service my-service])))))))
