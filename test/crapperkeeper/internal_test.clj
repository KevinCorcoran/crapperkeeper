(ns crapperkeeper.internal-test
  (:require [clojure.test :refer :all]
            [crapperkeeper.internal :refer :all]
            [crapperkeeper.fixtures :refer :all]
            [loom.graph :as loom]))

(deftest graph-construction-test
  (testing "a single service"
    (let [graph (services->graph [hello-service])]
      (is (= 1 (count (loom/nodes graph))))
      (is (= 0 (count (loom/edges graph))))
      ; TODO um introspect the node a bit more and make sure it's actually correct?
      ))

  (testing "two services"
    (let [service {:dependencies  HelloService
                   :lifecycle-fns {:init identity}}
          graph (services->graph [service hello-service])]
      ; TODO
      )))
