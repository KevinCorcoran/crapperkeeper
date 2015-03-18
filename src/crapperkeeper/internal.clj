(ns crapperkeeper.internal
  (:require [crapperkeeper.schemas :refer :all]
            [schema.core :as schema]
            [slingshot.slingshot :refer [throw+]])
  (:import (clojure.lang Keyword)
           (java.util Map)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; internal schemas (public schemas defined in crapperkeeper.schemas)

(def ServiceWithId
  (assoc Service :id Keyword))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(schema/defn validate-config!
  [services :- [Service]
   config :- Map]
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

(schema/defn with-id :- ServiceWithId
  "Wraps the given service with an ID."
  [service :- Service]
  (assoc service :id (->id service)))

(schema/defn initial-contexts :- Map
  "Returns the initial context for each service in 'services'.
  The initial context is simply the config data and nothing more."
  [services :- [ServiceWithId]
   config :- Map]
  (into {} (for [service services]
             {(:id service) {:config config}})))

(schema/defn run-lifecycle-fns :- Map
  "Invokes the lifecycle function specified by 'fn-key' on every service in
  'services'.  Returns the updated contexts."
  [fn-key :- (schema/enum :init :start :stop)
   services :- [ServiceWithId]
   contexts :- Map]
  (into {}
        (for [service services]
          (let [lifecycle-fn (get-in service [:lifecycle-fns fn-key])
                id (:id service)
                context (get contexts id)]
            {id (if lifecycle-fn (lifecycle-fn context) context)}))))

(schema/defn with-optional-dependencies :- [ServiceWithId]
  [services :- [ServiceWithId]]
  ; TODO
  services)

(schema/defn sort-dependencies :- [ServiceWithId]
  "Given a list of services, returns a list of services in a dependency-order."
  [services :- [ServiceWithId]]
  ; TODO
  services)

(schema/defn prepare-services :- [ServiceWithId]
  "Given the user-defined list of services,
  returns a list of services ready for use by Trapperkeeper."
  [services :- [Service]]
  (->> services
       (map with-id)
       (with-optional-dependencies)
       (sort-dependencies)))
