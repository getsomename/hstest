(ns hs.back.patient
  (:require [clojure.spec.alpha :as s]
            [clojure.java.jdbc :as j]))

(defn now [] (java.time.LocalDateTime/now))

(defn with-created [patient]
  (let [n (now)]
    (assoc patient :created n :updated n)))

(defn with-updated [patient]
  (assoc patient :updated (now)))

(defn- parse-id [val]
  (try (Integer/parseInt val)
       (catch Exception e nil)))

(defn wrap-id [handler]
  (fn [{req :request :as ctx}]
    (if-let [id (-> req :params :id parse-id)]
      (handler (assoc-in ctx [:request :params :id] id))
      {:status 400 :body {:error_message "id is invalid"}})))

(defn wrap-patient [handler]
  (fn [{req :request :as ctx}]
    (let [entity (-> req :body :patient)
          conformed (s/conform :hs.back.spec/patient (dissoc entity :id))]
      (if (not= conformed ::s/invalid)
        (handler (assoc-in ctx [:request :body :patient] conformed))
        {:status 400
         :body {:error_message "patient is invalid"
                :error (s/explain-data :hs.back.spec/patient entity)}}))))

(defn get-many [{req :request db :db}]
  (let [query "select * from patients order by created desc"
        patients (j/query db query)]
    {:status 200
     :body {:patients (vec patients)}}))

(defn- get-one [{req :request db :db}]
  (let [id (-> req :params :id)
        query ["select * from patients where id = ?" id]
        patient (first (j/query db query))]
    (if (nil? patient)
      {:status 404
       :body {:error_message "patient not found"}}
      {:status 200
       :body {:patient patient}})))

(def get-one-handler (-> get-one wrap-id))

(defn- update-patient [{req :request db :db}]
  (let [id (-> req :params :id)
        patient (-> req :body :patient)
        updated (-> (j/update! db :patients (with-updated patient) ["id = ?" id])
                    first zero? not)]
    (if updated
      {:status 200}
      {:status 404
       :error_message "patient not found"})))

(def update-handler (-> update-patient wrap-patient wrap-id))

(defn create-patient [{req :request db :db}]
  (let [patient (-> req :body :patient)
        id (-> (j/insert! db :patients (with-created patient)) first :id)]
    {:status 201 :body {:id id}}))

(def create-handler (-> create-patient wrap-patient))

(defn delete-patient [{req :request db :db}]
  (let [id (-> req :params :id)
        deleted (-> (j/delete! db :patients ["id = ?" id]) first zero? not)]
    (if deleted {:status 200}
        {:status 404
         :body {:error_message "patient not found"}})))

(def delete-handler (-> delete-patient wrap-id))
