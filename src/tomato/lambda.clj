(ns tomato.lambda
  (:require [clojure.string :as s]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [tomato.core :as tomato]
            [tomato.handler :as handler])
  (:gen-class
    :implements [com.amazonaws.services.lambda.runtime.RequestStreamHandler]))

(defn key->keyword [key-string]
  (-> key-string
      (s/replace #"([a-z])([A-Z])" "$1-$2")
      (s/replace #"([A-Z]+)([A-Z])" "$1-$2")
      (s/lower-case)
      (keyword)))

(defn -handleRequest [_this is _os _context]
  (let [req (json/parse-string (slurp (io/reader is)) key->keyword)
        text (:text (:message req))]
    (if (s/starts-with? text "/")
      (condp = (subs text 1)
        "go" (handler/handle-start-session!)
        "check" (handler/handle-check-remaining! (tomato/get-state!))
        "count" (handler/handle-send-counted! (tomato/get-counted!))
        "cancel" (handler/handle-cancel-session!)
        "pause" (handler/handle-pause-session!)
        "resume" (handler/handle-resume-session (tomato/get-state!))
        "state" (handler/handle-get-state! (tomato/get-state!))
        "watch" (handler/handle-watch)
        "unwatch" (handler/handle-unwatch)
        (println text)))))
