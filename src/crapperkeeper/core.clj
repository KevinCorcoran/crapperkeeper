(ns crapperkeeper.core
  (:require [crapperkeeper.internal :as internal]
            [crapperkeeper.schemas :refer :all]
            [schema.core :as schema])
  (:import (clojure.lang Keyword)
           (crapperkeeper.schemas ServiceInterface)
           (java.util Map)))

(schema/defn ^:always-validate boot!
  "Starts the Trapperkeeper framework with the given list of services
  and configuration data."
  ([services :- [Service]]
    (boot! services {}))
  ([services :- [Service]
    config :- Map]
    (internal/validate-config! services config)
    (let [services* (internal/prepare-services services)
          contexts (->> (internal/initial-contexts services* config)
                        (internal/run-lifecycle-fns :init services*)
                        (internal/run-lifecycle-fns :start services*))]

      ; Services and contexts are now initialized.
      ; We can define the main Trapperkeeper API function now.

      (schema/defn ^:always-validate service-call
        "Inovkes the function named by 'fn-key' on the service-interface
        specified by 'service-interface' using the given arguments."
        [service-interface :- ServiceInterface
         fn-key :- Keyword
         & args]
        (let [id (:id service-interface)
              service (first (filter
                               #(= (:id %) id)
                               services*))
              service-fn (get-in service [:service-fns fn-key])
              context (get contexts id)]
          (apply service-fn context args)))

      (defn shutdown!
        "Stops the Trapperkeeper framework and all services running within it.
        Calls 'stop' on each service."
        []
        (internal/run-lifecycle-fns :stop services* contexts)))))
