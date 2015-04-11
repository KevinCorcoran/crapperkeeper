(ns crapperkeeper.test-utils
  "Utilities for testing Crapperkeeper services"
  (:require [crapperkeeper.internal :as internal]))

(defmacro with-ck
  "Test helper for bootstrapping Crapperkeeper and cleaning up afterward."
  [app services config & body]
  `(let [~app (internal/boot! ~services ~config)]
     ~@body))

(defmacro with-services
  "Test helper for bootstrapping Crapperkeeper without any config data."
  [app services & body]
  `(let [~app (internal/boot! ~services {})]
     ~@body))
