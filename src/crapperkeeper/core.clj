(ns crapperkeeper.core)

(def simplest-service
  {:lifecycle {:init (fn [context]
                       :foo)}})

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
