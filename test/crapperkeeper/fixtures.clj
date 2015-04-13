(ns crapperkeeper.fixtures)

(def FooService
  {:service-fns #{{:name :foo}}})

(def foo-service
  {:implements  FooService
   :service-fns {:foo (fn [app context]
                        "hello from foo")}})

(def BarService
  {:service-fns #{{:name :bar}}})

(def bar-service
  {:implements  BarService
   :service-fns {:bar (fn [app context]
                        "hifrom the bar")}})

(def TestService
  {:service-fns #{{:name :test}}})
