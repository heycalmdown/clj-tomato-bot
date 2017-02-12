(ns tomato.s3
  (:require [amazonica.aws.s3 :as s3]
            [clojure.edn :as edn]
            [tomato.config :as config]))

(defn put! [k v]
  (when (config/get! :aws)
    (s3/put-object (config/get! :aws) "clj-tomato-bot" k (pr-str v))))

(defn get! [k]
  (when (config/get! :aws)
    (try (edn/read-string
           (slurp
             (:object-content (s3/get-object (config/get! :aws) "clj-tomato-bot" k))))
         (catch com.amazonaws.services.s3.model.AmazonS3Exception _ ()))))

(defn update! [k f x y & args]
  (let [object (tomato.s3/get! k)]
    (put! k (apply f (concat [object x y] args)))))
