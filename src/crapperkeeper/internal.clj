(ns crapperkeeper.internal
  (:require [crapperkeeper.schemas :refer :all]
            [schema.core :as schema])
  (:import (clojure.lang Keyword)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; state

(def services-atom (atom nil))

(def contexts-atom (atom {}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; internal schemas (public schemas defined in crapperkeeper.schemas)

(def ServiceWithId
  (merge Service {:id Keyword}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(schema/defn run-lifecycle-fn!
  [fn-key :- (schema/enum :init :start :stop)]
  (doseq [service @services-atom]
    (when-let [lifecycle-fn (get-in service [:lifecycle-fns fn-key])]
      (let [context (get @contexts-atom (:id service))]
        (lifecycle-fn context)))))

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

(schema/defn ^:always-validate sort-services :- [ServiceWithId]
  [services :- [Service]]
  ; TODO this is not actually sorting yet, currently just returning in input order
  (map with-service-id services))

(defn initialize-contexts!
  [config]
  (doseq [service @services-atom]
    (let [id (:id service)]
      (swap! contexts-atom assoc id {:config config}))))

(defn initialize-services!
  [services]
  (->> services
       (sort-services)
       (reset! services-atom)))
