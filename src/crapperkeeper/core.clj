(ns crapperkeeper.core
  (:require [crapperkeeper.internal :as internal]
            [crapperkeeper.schemas :refer :all]
            [schema.core :as schema])
  (:import (clojure.lang Keyword)
           (crapperkeeper.schemas ServiceInterface)))

(schema/defn ^:always-validate service-call
  "Inovkes the function named by 'fn-key' on the service-interface
  specified by 'service-interface' using the given arguments."
  [service-interface :- ServiceInterface
   fn-key :- Keyword
   & args]
  (let [service (first (filter
                         #(= (:implements %) service-interface)
                         @internal/services-atom))
        service-fn (get-in service [:service-fns fn-key])
        context (get @internal/contexts-atom (:id service))]
    (apply service-fn context args)))

(defn shutdown!
  "Stops the Trapperkeeper framework and all services running within it.
  Calls 'stop' on each service."
  []
  (internal/run-lifecycle-fns
    :stop
    @internal/services-atom
    @internal/contexts-atom))
