(ns crapperkeeper.internal
  (:require [crapperkeeper.schemas :refer :all]
            [schema.core :as schema]
            [slingshot.slingshot :refer [throw+]])
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

(defn validate-config!
  [services config]
  (doseq [service services]
    (when-let [config-schema (:config-schema service)]
      (when-let [schema-error (schema/check config-schema config)]
        (throw+ {:type :crapperkeeper/invalid-config-error
                 :error schema-error})))))

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

(defn initialize!
  "Initializes 'services-atom' and 'contexts-atom'."
  [services config]
  (->> services
       (map with-service-id)
       (reset! services-atom))
  (doseq [service @services-atom]
    (let [id (:id service)]
      (swap! contexts-atom assoc id {:config config}))))

(schema/defn ^:always-validate run-lifecycle-fns!
  "Invokes the lifecycle function specified by 'fn-key' on
  every service in the application."
  [fn-key :- (schema/enum :init :start :stop)]
  (doseq [service @services-atom]
    (when-let [lifecycle-fn (get-in service [:lifecycle-fns fn-key])]
      (let [context (get @contexts-atom (:id service))]
        (lifecycle-fn context)))))
