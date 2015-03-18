(ns crapperkeeper.internal
  (:require [crapperkeeper.schemas :refer :all]
            [schema.core :as schema]
            [slingshot.slingshot :refer [throw+]])
  (:import (clojure.lang Keyword)))


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

(schema/defn ->id :- Keyword
  "Returns the ID of the service if it has inherited one by implementing a
  service interface, otherwise generates a new ID."
  [service :- Service]
  (or
    (get-in service [:implements :id])
    (keyword (gensym "trapperkeeper-service-"))))

(schema/defn with-service-id :- ServiceWithId
  "Wraps the given service with an ID."
  [service :- Service]
  (assoc service :id (->id service)))

(schema/defn ^:always-validate run-lifecycle-fns!
  "Invokes the lifecycle function specified by 'fn-key' on every service in
  'services'."
  [fn-key :- (schema/enum :init :start :stop)
   services :- [ServiceWithId]
   contexts]
  (doseq [service services]
    (when-let [lifecycle-fn (get-in service [:lifecycle-fns fn-key])]
      (let [context (get contexts (:id service))]
        (lifecycle-fn context)))))
