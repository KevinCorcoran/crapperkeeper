(ns crapperkeeper.test-utils
  "Utilities for testing Crapperkeeper services"
  (:require [crapperkeeper.internal :as internal]))

(defmacro with-tk
  "Test helper for bootstrapping Crapperkeeper and cleaning up afterward."
  [services config & body]
  `(try
     (internal/boot! ~services ~config)
     ~@body
     (finally
       (reset! internal/services-atom nil)
       (reset! internal/contexts-atom nil))))

(defmacro with-services
  "Test helper for bootstrapping Crapperkeeper without any config data."
  [services & body]
  `(try
     (internal/boot! ~services {})
     ~@body
     (finally
       (reset! internal/services-atom nil)
       (reset! internal/contexts-atom nil))))


