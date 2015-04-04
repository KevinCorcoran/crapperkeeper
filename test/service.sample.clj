(require '[clojure.string :as string])

;; This file demonstrates the Crapperkeeper API.

;; First things first - Crapperkeeper is heavily based on prismatic/schema.
;; Most things like service implementations and interfaces are just maps
;; with schemas defining their structures.

(require '[schema.core :as schema])
(require '[crapperkeeper.schemas :refer :all])

;; Let's start out with the most basic of Crapperkeeper services ...
;; This is a Crapperkeeper service implementation that implements only the
;; 'init' lifecycle function and does nothing.

(def no-op-service
  {:init (fn [state context]
           nil)})

;; These service implementations are just maps, with a well-defined schema.

(schema/check Service no-op-service) ; => nil

(schema/check Service {:init-oops (fn [state context]
                                    nil)}) ; => {:init-oops disallowed-key}

;; Service interfaces are also just maps.

;; Even though they're just maps, I've been continuing to name them as in
;; Trapperkeeper, even though that's not really conventional anymore since
;; they're no longer protocols.  I'm not sure what I think is the best naming
;; convention ... but right now this seems better than making one/both of them
;; "-interface" or "-impl" or something like that.

(def MyService
  {
   ;; Service interface name - optional - not yet sure if this is necessary or useful
   :name        :my-service
   ;; All service interfaces must define a set of functions -
   ;; these are just the "contracts" / declarations of these functions -
   ;; implementations are left up to the service implementation.
   :service-fns #{
                  ;; Service function specifications are also just maps.
                  {:name :foo
                   :doc  "Does foo-y kinda stuff."
                   :args [:arg1 :arg2]
                   ;; It's also possible to specify multilple arglists
                   ;arglists [[:foo] [:foo :bar]]
                   }
                  ;; The only required piece of data is the function name.
                  {:name :bar}}})

(schema/check ServiceInterface MyService) ; => nil

;; A more realistic service which demonstrates some of the features of Crapperkeeper

(def my-service
  {
   ;; A service implements a service interface by using :implements
   :implements  MyService

   ;; Lifecycle functions

   ;; ... are simply top-level keys in the service map
   ;; They're the same as in TK - take in the current service context,
   ;; return an updated context.
   :init        (fn [state context]
                  (assoc context :something 42))
   :start       (fn [state context]
                  (assert (:something context))
                  (assoc context :started? true))
   :stop        (fn [state context]
                  (println "stopping my service")
                  context)

   ;; Service functions

   ;; ... must contain an implementation of each :service-fn specified in the
   ;; service interface referenced in :implements - i.e., this service must
   ;; implement :foo and :bar from MyService
   :service-fns {
                 ;; Service functions now get the context passed into them
                 ;; as the first argument, and the rest of the args follow.
                 :foo (fn [state context arg1 arg2]
                        (str arg1 " " arg2))
                 ;; Service functions can now use schema/fn or schema/defn
                 :bar (schema/fn []
                        nil)}})


;; Since services (and interfaces) are just maps, you can manipulate them using
;; all of clojure's usual functions.

(def busted-service
  (assoc my-service :implements "not a service interface"))

;; Validation of :implements service interface

(schema/check Service my-service) ; => nil
(schema/check Service busted-service) ; => {:implements (not (map? a-java.lang.String))}


(require '[crapperkeeper :as ck])
(require '[crapperkeeper.test-utils :as test-utils])


;; Start up the framework using a test helper

(test-utils/with-services state [my-service]

  ;; Service calls are now simple function calls.
  ;; NB: service calls can now be made from _anywhere_,
  ;; not just inside a downstream service.
  (ck/service-call state MyService :foo 42 "is the answer")) ; => "42 is the answer"


;; Dependencies

(def AnotherService {:service-fns #{{:name :baz}}})

(def downstream-service
  {
   ;; Depedencies are specified the way you probably expect by now.
   ;; It's a set, and each value in the set must be a service interface
   :dependencies          #{MyService}

   ;; Some dependencies are not absolutely necessary.
   :optional-dependencies #{AnotherService}})

;; Service calls just return 'nil' when the optional dependency doesn't exist.

(test-utils/with-services state [downstream-service]
  (ck/service-call state AnotherService :baz)) ; => nil

;; ... but when an implementation exists, it's availble.

(def another-service {:implements  AnotherService
                      :service-fns {:baz (fn [state context]
                                           "Is this your homework, Larry?")}})

(test-utils/with-services state [downstream-service another-service]
  (ck/service-call state AnotherService :baz)) ; => "Is this your homework, Larry?"



(def AllCapsString
  "A schema for a string containing only capital letters."
  (schema/both String
               (schema/pred
                 (fn [s]
                   (every? #(Character/isUpperCase %) s)))))


;; Extra configuration goodies

(def yet-another-service
  {
   ;; Services can define a schema for their required configuration.
   ;; If the user-specified config data doesn't satisfy this schema, an
   ;; exception will be thrown at startup.
   ;; (Implementation note: I'm thinking this needs to get wrapped with Any -> Any.)
   :config-schema             {:x String
                               :y {:a String
                                   :b [String]}}

   ;; Services can specify a 'config transformer', which is just a function
   ;; that runs on the config data before it becomes available to the rest of the service.
   :config-transformer        (fn [{:keys [x y]}]
                                {:x (string/upper-case x)
                                 :y (string/join " " (cons (:a y) (:b y)))})

   ;; Services can also define an output schema for the config transformer.
   ;; I'm not wild about this key name - maybe just :config-schema* would be better.
   ;; This is a bit redundant - could just use a schema/fn as :config-transformer,
   ;; but it seems nice to have it here for consistency.
   :transformed-config-schema {:x AllCapsString
                               :y String}

   :implements                AnotherService
   :service-fns               {:baz (fn [state context]

                                      ;; Config data is now in context.
                                      ;; No more config service.
                                      (:config context))}})

(test-utils/with-ck state [yet-another-service] {:x "foo"
                                           :y {:a "Hi,"
                                               :b ["I'm" "Adam" "Prince" "of" "Eternia"]}}
  (ck/service-call state AnotherService :baz)) ; => {:x "FOO", :y "Hi I'm Adam Prince of Eternia"}
