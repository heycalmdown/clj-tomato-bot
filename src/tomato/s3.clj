(ns tomato.s3
  (:require [clojure.edn :as edn]
            [tomato.config :as config]
            [aws.sdk.s3 :as s3]))

(defn reset! [k v]
  (s3/put-object (config/get :s3) "clj-tomato-tokyo" k (pr-str v)))

(defn read [k]
  (try (edn/read-string
         (slurp
           (:content (s3/get-object (config/get :s3) "clj-tomato-tokyo" k))))
       (catch com.amazonaws.services.s3.model.AmazonS3Exception _ ())))
