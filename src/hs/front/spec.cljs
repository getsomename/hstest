(ns hs.front.spec
  (:require [clojure.spec.alpha :as s]
            [hs.shared.spec :as base]))

(s/def ::->date
  (s/conformer
   (fn [value]
     (let [parsed (js/Date.parse value)]
       (if (js/isNaN parsed) :clojure.spec.alpha/invalid
           (js/Date. parsed))))))

(s/def :patient/birthdate (s/and
                           ::base/ne-string
                           ::->date))

(s/def ::patient
  (s/keys :req-un [:patient/name
                   :patient/birthdate
                   :patient/address
                   :patient/gender
                   :patient/policy]
          :opt-un [:patient/id]))

(comment
  (s/conform ::->date "2000")
  (s/conform ::->date "inv")
  (s/valid? :patient/birthdate "inv"))