(ns crapperkeeper.fixtures)

(def FooService
  {:service-fns #{{:name :foo}}})

(def foo-service
  {:implements  FooService
   :service-fns {:foo (fn [state context]
                          "hello from foo")}})

(def BarService
  {:service-fns #{{:name :bar}}})

(def bar-service
  {:implements  BarService
   :service-fns {:bar (fn [state context]
                        "hifrom the bar")}})

(def TestService
  {:service-fns #{{:name :test}}})
