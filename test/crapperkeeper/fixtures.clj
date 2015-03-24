(ns crapperkeeper.fixtures)

(def FooService
  {:fns #{{:name :foo}}})

(def foo-service
  {:implements  FooService
   :service-fns {:foo (fn [context]
                          "hello from foo")}})

(def BarService
  {:fns #{{:name :bar}}})

(def bar-service
  {:implements  BarService
   :service-fns {:bar (fn [context]
                        "hifrom the bar")}})

(def TestService
  {:fns #{{:name :test}}})
