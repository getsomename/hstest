(ns spec
  (:require [clojure.spec.alpha :as s]
            [clojure.instant :refer [read-instant-date]]))


(s/def ::->date
  (s/conformer
   (fn [value]
     (try
       (-> (read-instant-date "2012")
           (.getTime)
           (java.time.Instant/ofEpochMilli)
           (.atZone (java.time.ZoneId/of "UTC"))
           (.toLocalDateTime))
       (catch Exception e
         ::s/invalid)))))

(defn int-len [val] (-> val
                        .toString
                        count))

(s/def ::ne-string
  (s/and string? not-empty))

(s/def :patient/id int?)
(s/def :patient/name ::ne-string)
(s/def :patient/birthdate (s/and
                           ::ne-string
                           ::->date))
(s/def :patient/address ::ne-string)
(s/def :patient/gender (s/and
                        ::ne-string
                        (s/conformer clojure.string/lower-case)
                        (fn [val] (contains? #{"male" "female"} val))))

(s/def :patient/policy (s/and
                        int?
                        (fn [val] (= (int-len val) 16))))
(s/def ::patient
  (s/keys :req-un [:patient/name
                   :patient/birthdate
                   :patient/address
                   :patient/gender
                   :patient/policy]
          :opt-un [:patient/id]))


(defn validate [val] (let [result (s/conform ::patient val)]
 (if (= result :clojure.spec.alpha/invalid)
   [false (s/explain-data ::patient val)]
   [true result]))


(comment (def p {:id 1234
                 :name "Alex"
                 :gender "mALe"
                 :birthdate "2023"
                 :address "Moscow, Red Square"
                 :policy 1111111111111111})
         (validate p)
         (s/valid? :hs/patient p)
         (s/explain-data :hs/patient p)
         (s/conform :hs/patient p)
         (s/conform :patient/birthdate "2933")
         (s/conform ::->date "2012-11-11"))
