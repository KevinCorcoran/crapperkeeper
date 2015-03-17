(ns crapperkeeper.core
  (:require [crapperkeeper.internal :as internal]
            [schema.core :as schema])
  (:import (clojure.lang Keyword IFn)))

(schema/defrecord ServiceInterface
  [id :- Keyword
   service-fns :- #{Keyword}]) ; For now, a service function is simply a name (keyword)

(def Service
  "A schema which describes a Trapperkeeper service."
  {(schema/optional-key :implements)  ServiceInterface
   (schema/optional-key :service-fns) {Keyword IFn}})

(schema/defn ^:always-validate boot!
  "Starts the Trapperkeeper framework with the given list of services."
  [& services :- [Service]]
  (reset! internal/services
          (into {}
                (for [service services]
                  {(get-in service [:implements :id]) service}))))

(schema/defn ^:always-validate service-call
  "Inovkes the function named by 'fn-key' on the service specified by
  'service-id' using the given arguments."
  [service :- ServiceInterface
   fn-key :- Keyword
   & args]
  (let [context (get @internal/contexts (:id service))
        service (get @internal/services (:id service))
        service-fn (get-in service [:service-fns fn-key])]
    #_(println "services are" @internal/services)
    #_(println "Invoking service function" fn-key
             "on service" service
             "with context" context)
    (apply service-fn context args)))
