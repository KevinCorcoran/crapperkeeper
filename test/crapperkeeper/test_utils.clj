(ns crapperkeeper.test-utils
  "Utilities for testing Crapperkeeper services"
  (:require [crapperkeeper.internal :as internal]))

(defmacro with-ck
  "Test helper for bootstrapping Crapperkeeper and cleaning up afterward."
  [state services config & body]
  `(let [~state (internal/boot! ~services ~config)]
     ~@body))

(defmacro with-services
  "Test helper for bootstrapping Crapperkeeper without any config data."
  [state services & body]
  `(let [~state (internal/boot! ~services {})]
     ~@body))
