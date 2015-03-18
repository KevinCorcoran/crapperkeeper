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
    (let [services-with-ids (map internal/with-service-id services)
          contexts (->> (internal/initial-contexts services-with-ids config)
                        (internal/run-lifecycle-fns :init services-with-ids)
                        (internal/run-lifecycle-fns :start services-with-ids))]

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
                               services-with-ids))
              service-fn (get-in service [:service-fns fn-key])
              context (get contexts id)]
          (apply service-fn context args)))

      (defn shutdown!
        "Stops the Trapperkeeper framework and all services running within it.
        Calls 'stop' on each service."
        []
        (internal/run-lifecycle-fns :stop services-with-ids contexts)))))
