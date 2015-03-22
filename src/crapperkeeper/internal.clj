(ns crapperkeeper.internal
  (:require [crapperkeeper.schemas :as schemas]
            [schema.core :as schema]
            [slingshot.slingshot :refer [throw+]]
            [loom.graph :as loom]
            [loom.alg :as loom-alg])
  (:import (clojure.lang Keyword)
           (java.util Map)
           (crapperkeeper.schemas ServiceInterface)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; state
(def services-atom (atom nil))
(def contexts-atom (atom nil))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; internal schemas (public schemas defined in crapperkeeper.schemas)

(def Service
  "Interally, this schema is used instead of crapperkeeper.core/Service
  because services have IDs but most functions don't actually require that,
  so using the ServiceWithId schema limits testability."
  (assoc schemas/Service (schema/optional-key :id) Keyword))

(def ServiceWithId
  (assoc schemas/Service :id Keyword))

(def Dependency
  (schema/both
    [Service]
    (schema/pred #(= (count %) 2))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(schema/defn validate-config!
  [services :- [Service]
   config :- Map]
  (doseq [service services]
    (when-let [config-schema (:config-schema service)]
      (when-let [schema-error (schema/check config-schema config)]
        (throw+ {:type :crapperkeeper/invalid-config-error
                 :error schema-error})))))

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

(schema/defn with-id :- ServiceWithId
  [service :- Service]
  (assoc service :id (keyword (gensym "trapperkeeper-service-"))))

(schema/defn with-ids :- [ServiceWithId]
  "Wraps each of the given services with an ID."
  [services :- [Service]]
  (map with-id services))

; TODO
#_(schema/defn validate-services! :- [Service]
  "Validates the user-specified services.  Throws an error on:

    * invalid service defitions (beyond the 'Service' schema) - e.g.,
      must define at least one lifecycle-fn or service-fn
    * missing dependencies
    * multiple implementations of a service interface"
  [services :- [Service]]
  )

; TODO
#_(schema/defn with-optional-dependencies :- [Service]
  [services :- [Service]]
  )

(schema/defn implementation-of
  [interface :- ServiceInterface
   services :- [Service]]
  (first (filter
           #(= (:implements %) interface)
           services)))

(schema/defn service->dependencies :- [Dependency]
  "Given all of the services and a particular services,
  return the dependencies for that service."
  [service :- Service
   services :- [Service]]
  (let [dependencies (:dependencies service)]
    (when (not-empty dependencies)
      (map (fn [dependency]
             ; TODO could clean this up by collecting implementations AOT
             [service (implementation-of dependency services)])
           dependencies))))

(schema/defn services->dependencies :- [Dependency]
  "Returns the list of dependencies expressed in the list of services."
  [services :- [Service]]
  (->> services
       (map #(service->dependencies % services))
       (remove nil?)
       (apply concat)))

(schema/defn sort-services :- [Service]
  "Given a list of services, returns a list of services in a dependency-order."
  [services :- [Service]]
  (let [add-nodes (partial apply loom/add-nodes)
        add-edges (partial apply loom/add-edges)
        graph (->
                ; Construct a graph.
                (loom/digraph)
                ; Each node in the graph represents a service.
                ; Initially, there are no edges in the graph.
                (add-nodes services)
                ; Add an edge in the graph for each dependency between services.
                (add-edges (services->dependencies services)))]
    ; Perform a topological sort of the graph.
    ; This returns the services in dependency-order.
    ; Concat this list with any of the "loner" services
    ; (no dependencies either way),
    ; as they will have been left-out of the topological sort.
    (concat
      (loom-alg/topsort graph)
      (loom-alg/loners graph))))

(schema/defn prepare-services :- [ServiceWithId]
  "Given the user-defined list of services,
  returns a list of services ready for use by Trapperkeeper."
  [services :- [Service]]
  ;(validate-services! services)
  (->> services
       (with-ids)
       ;(with-optional-dependencies)
       (sort-services)))

(schema/defn initial-contexts :- Map
  "Returns the initial context for each service in 'services'.
  The initial context is simply the config data and nothing more."
  [services :- [ServiceWithId]
   config :- Map]
  (into {} (for [service services]
             {(:id service) {:config config}})))

(schema/defn ^:always-validate boot!
  "Starts the Trapperkeeper framework with the given list of services
  and configuration data."
  [services :- [Service]
   config :- Map]
  (validate-config! services config)
  (let [services* (prepare-services services)]
    (reset! services-atom services*)
    (let [contexts (->> (initial-contexts services* config)
                        (run-lifecycle-fns :init services*)
                        (run-lifecycle-fns :start services*))]
      (reset! contexts-atom contexts))))
