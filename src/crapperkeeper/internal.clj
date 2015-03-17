(ns crapperkeeper.internal
  (:require [crapperkeeper.schemas :refer :all]
            [schema.core :as schema])
  (:import (clojure.lang Keyword)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; state

(def services (atom nil))

(def contexts (atom nil))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; internal schemas (public schemas defined in crapperkeeper.schemas)

(def ServiceWithId
  (merge Service {:id Keyword}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn run-lifecycle-fn!
  [fn-key]
  (doseq [service @services]
    (when-let [lifecycle-fn (get-in service [:lifecycle-fns fn-key])]
      (let [context (get @contexts (:id service))]
        (lifecycle-fn context)))))

(defn run-lifecycle-fns!
  []
  (run-lifecycle-fn! :init)
  (run-lifecycle-fn! :start)
  (run-lifecycle-fn! :stop))

(schema/defn service->id :- Keyword
  "Returns the ID of the service if it has inherited one by implementing a
  service interface, otherwise generates a new ID."
  [service :- Service]
  (or
    (get-in service [:implements :id])
    (keyword (gensym "trapperkeeper-service-"))))

(schema/defn with-service-id :- ServiceWithId
  [service :- Service]
  (assoc service :id (service->id service)))

(schema/defn sort-services :- [ServiceWithId]
  [services :- [Service]]
  ; TODO this is not actually sorting yet, currently just returning in input order
  (map with-service-id services))
