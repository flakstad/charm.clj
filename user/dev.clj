(ns dev)

(comment
  (require '[clojure.test :refer [run-all-tests]])
  (run-all-tests #"charm.*-test"))

(comment
  (require '[portal.api :as p])
  (def p (p/open)) ; Open a new inspector
  (add-tap #'p/submit)) ; Add portal as a tap> target
