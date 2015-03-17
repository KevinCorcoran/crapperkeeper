(ns crapperkeeper.core
  (:require [crapperkeeper.internal :as internal]
            [schema.core :as schema])
  (:import (clojure.lang Symbol Keyword)))

(defn boot!
  "Starts the Trapperkeeper framework with the given list of services."
  [& services]
  ; TODO validate all :implements values as ServiceInterfaces
  (swap! internal/services conj (first services))
  #_(let [first-service (first services)
        service-symbol (:implements first-service)]
    (swap! internal/services assoc service-symbol first-service)))

(defn service-call
  "Inovkes the function named by 'fn-key' on the service specified by
  'service-symbol' using the given arguments."
  [service-symbol fn-key & args]
  (let [service (first (filter #(= (:implements %) service-symbol)
                               @internal/services))
        service-fn (get-in service [:service-fns fn-key])
        context (get @internal/contexts service-symbol)]
    #_(println "Invoking service function" fn-key
             "on service" service
             "with context" context)
    (apply service-fn context args)))

(schema/defrecord ServiceInterface
  ; For now, a service function is simply a name (keyword)
  [service-fns :- #{Keyword}])