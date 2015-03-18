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
  (assoc Service :id Keyword))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(schema/defn run-lifecycle-fn!
  "Invokes the lifecycle function specified by 'fn-key'."
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
  "Wraps the given service with an ID."
  [service :- Service]
  (assoc service :id (service->id service)))

(defn initialize-contexts!
  [config]
  (doseq [service @services-atom]
    (let [id (:id service)]
      (swap! contexts-atom assoc id {:config config}))))

(schema/defn initialize-services!
  [services :- [Service]]
  (->> services
       (map with-service-id)
       (reset! services-atom)))
