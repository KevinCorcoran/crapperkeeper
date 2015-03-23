(ns crapperkeeper.fixtures
  (:import (crapperkeeper.schemas ServiceInterface)))

(def FooService
  (ServiceInterface. #{:foo}))

(def foo-service
  {:implements  FooService
   :service-fns {:foo (fn [context]
                          "hello from foo")}})

(def BarService
  (ServiceInterface. #{:bar}))

(def bar-service
  {:implements  BarService
   :service-fns {:bar (fn [context]
                        "hifrom the bar")}})

(def TestService
  (ServiceInterface. #{:test}))
