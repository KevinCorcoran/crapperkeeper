(ns crapperkeeper.fixtures
  (:import (crapperkeeper.schemas ServiceInterface)))

(def FooService
  (ServiceInterface. #{:foo}))

(def foo-service
  {:implements  FooService
   :service-fns {:foo (fn [context]
                          "hello from foo")}})
