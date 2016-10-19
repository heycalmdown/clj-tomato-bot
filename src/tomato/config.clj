(ns tomato.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def config (edn/read-string (slurp (io/resource "config.edl"))))

(defn- private-get [config keys]
  (if (seq keys)
    (private-get ((first keys) config) (rest keys))
    config))

(defn get [& keys]
  (private-get config keys))
