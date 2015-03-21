(ns crapperkeeper.internal
  (:require [crapperkeeper.schemas :refer :all]
            [schema.core :as schema]
            [slingshot.slingshot :refer [throw+]]
            [loom.graph :as loom]
            [loom.alg :as loom-alg])
  (:import (clojure.lang Keyword)
           (java.util Map)
           (crapperkeeper.schemas ServiceInterface)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; internal schemas (public schemas defined in crapperkeeper.schemas)

(def ServiceWithId
  (assoc Service :id Keyword))

(def Dependency
  [ServiceWithId ServiceWithId])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(schema/defn validate-config!
  [services :- [Service]
   config :- Map]
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

(schema/defn with-id :- ServiceWithId
  [service :- Service]
  (assoc service :id (service->id service)))

(schema/defn with-ids :- [ServiceWithId]
  "Wraps each of the given services with an ID."
  [services :- [Service]]
  (map services with-id))

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

(schema/defn validate-services! :- [Service]
  "Validates the user-specified services.  Checks for:

    * missing dependencies
    * multiple implementations of a service interface

  Throws an error if the services are invalid, otherwise returns the services."
  [services :- [Service]]
  ; TODO
  services)

(schema/defn with-optional-dependencies :- [ServiceWithId]
  [services :- [ServiceWithId]]
  ; TODO
  services)

(schema/defn implementation-of
  [interface :- ServiceInterface
   services :- [ServiceWithId]]
  (first (filter
           #(= (:implements %) interface)
           services)))

(schema/defn services->dependencies :- [Dependency]
  "Returns the list of dependencies expressed in the list of services."
  [services :- [ServiceWithId]]
  (->> services
       (filter :dependencies)
       (map (fn [service]
              (map (fn [dependency]
                     [service (implementation-of dependency services)])
                   (:dependencies service))))
       (flatten))

  (map
    (fn [service] {:id (:id service) {:dependencies}} (:dependencies service))
    services))

(schema/defn sort-dependencies :- [ServiceWithId]
  "Given a list of services, returns a list of services in a dependency-order."
  [services :- [ServiceWithId]]
  ; Construct a graph.  Each node in the graph represents a service.
  ; Initially, there are no edges in the graph.
  (let [graph (loom/digraph services)
        dependencies (services->dependencies services)]
    (-> graph
        ; Add an edge in the graph for each dependency between services.
        (loom/add-edges dependencies)
        ; Perform a topological sort of the graph.
        ; This returns the list of services in dependency-order.
        (loom-alg/topsort))))

(schema/defn prepare-services :- [ServiceWithId]
  "Given the user-defined list of services,
  returns a list of services ready for use by Trapperkeeper."
  [services :- [Service]]
  (->> services
       (validate-services!)
       (with-ids)
       (with-optional-dependencies)
       (sort-dependencies)))
