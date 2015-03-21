(ns crapperkeeper.schemas
  (:require [schema.core :as schema])
  (:import (clojure.lang IFn Keyword)
           (java.util Map)))

(schema/defrecord ServiceInterface
  [id :- Keyword
   service-fns :- #{Keyword}]) ; For now, a service function is simply a name (keyword)

(defn with-service-or-lifecycle-fns
  [x]
  (schema/both
    x
    (schema/pred
      (fn [x] (or (:lifecycle-fns x) (:service-fns x))))))

(def Service
  "A schema which describes a Trapperkeeper service."
  (with-service-or-lifecycle-fns
    {(schema/optional-key :implements)    ServiceInterface
     (schema/optional-key :lifecycle-fns) {(schema/enum :init :start :stop) IFn}
     (schema/optional-key :service-fns)   {Keyword IFn}
     (schema/optional-key :dependencies)  #{ServiceInterface}
     (schema/optional-key :config-schema) Map}))

