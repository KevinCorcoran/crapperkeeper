(ns crapperkeeper.fixtures
  (:import (crapperkeeper.schemas ServiceInterface)))

(def HelloService
  (ServiceInterface. :hello-service #{:hello}))

(def hello-service
  {:implements  HelloService
   :service-fns {:hello (fn [context]
                          "hello world")}})
