(ns crapperkeeper.core
  (:require [crapperkeeper.internal :as internal]
            [crapperkeeper.schemas :refer :all]
            [schema.core :as schema])
  (:import (clojure.lang Keyword)
           (crapperkeeper.schemas ServiceInterface)))

(schema/defn ^:always-validate boot!
  "Starts the Trapperkeeper framework with the given list of services
  and configuration data."
  ([services :- [Service]]
    (boot! services {}))
  ([services :- [Service]
    config]
    (internal/validate-config! services config)
    (internal/initialize! services config)
    (internal/run-lifecycle-fns! :init)
    (internal/run-lifecycle-fns! :start)))

(defn shutdown!
  "Stops the Trapperkeeper framework and all services running within it.
  Calls 'stop' on each service."
  []
  (internal/run-lifecycle-fns! :stop))

(schema/defn ^:always-validate service-call
  "Inovkes the function named by 'fn-key' on the service-interface specified by
  'service-interface' using the given arguments."
  [service-interface :- ServiceInterface
   fn-key :- Keyword
   & args]
  (let [id (:id service-interface)
        service (first (filter
                         #(= (:id %) id)
                         @internal/services-atom))
        service-fn (get-in service [:service-fns fn-key])
        context (get @internal/contexts-atom id)]
    (apply service-fn context args)))
