(ns crapperkeeper.schemas
  (:require [schema.core :as schema])
  (:import (clojure.lang IFn Keyword)
           (java.util Map)))

(def ServiceFn
  {:name                           Keyword
   (schema/optional-key :doc)      String
   (schema/optional-key :args)     [Keyword]
   (schema/optional-key :arglists) [[Keyword]]})

(def ServiceInterface
  {(schema/optional-key :name) Keyword
   :service-fns                #{ServiceFn}})

(def Service
  "A schema which describes a Trapperkeeper service."
  {(schema/optional-key :implements)                ServiceInterface
   (schema/optional-key :init)                      IFn
   (schema/optional-key :start)                     IFn
   (schema/optional-key :stop)                      IFn
   (schema/optional-key :service-fns)               {Keyword IFn}
   (schema/optional-key :dependencies)              #{ServiceInterface}
   (schema/optional-key :optional-dependencies)     #{ServiceInterface}
   (schema/optional-key :config-schema)             Map
   (schema/optional-key :transformed-config-schema) Map
   (schema/optional-key :config-transformer)        IFn})

