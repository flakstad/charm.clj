(ns dev
  (:require [clojure.test :refer [run-all-tests]]))

(run-all-tests #"charm.*-test")
