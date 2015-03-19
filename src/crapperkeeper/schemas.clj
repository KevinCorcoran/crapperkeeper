(ns crapperkeeper.schemas
  (:require [schema.core :as schema])
  (:import (clojure.lang IFn Keyword)
           (java.util Map)))

(schema/defrecord ServiceInterface
  [id :- Keyword
   service-fns :- #{Keyword}]) ; For now, a service function is simply a name (keyword)

(def Service
  "A schema which describes a Trapperkeeper service."
  (schema/both
    {(schema/optional-key :implements)    ServiceInterface
     (schema/optional-key :lifecycle-fns) {(schema/enum :init :start :stop) IFn}
     (schema/optional-key :service-fns)   {Keyword IFn}
     (schema/optional-key :dependencies)  #{ServiceInterface}
     (schema/optional-key :config-schema) Map}
    (schema/pred (fn [x] (or (:lifecycle-fns x) (:service-fns x))))))

