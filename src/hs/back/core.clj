(ns hs.back.core
  (:require
   [bidi.bidi :as bidi]
   [bidi.ring :refer [make-handler]]
   [hs.back.patient :as patient]
   [clojure.java.io :as io]
   [ring.middleware.json :refer [wrap-json-body
                                 wrap-json-response]]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.logger :refer [wrap-with-logger]]
   [hs.back.spec]
   [hs.back.db :as db]
   [ring.adapter.jetty :as adapter]
   [ring.util.response :as rr]
   [clojure.java.jdbc :as jdbc]
   [clojure.spec.alpha :as s])
  (:gen-class))

(defn start-server [cfg handler]
  (adapter/run-jetty handler {:port (:port cfg) :join? false}))

(defn index-handler [req]
  (-> (rr/resource-response "index.html" {:root "public"})
      (rr/content-type "text/html; charset=utf-8")))

(defn not-found [req] {:status 404 :body "page not found"})

(defn health [req] {:status 200 :body "ok"})

(def routes
  ["/" {"" {:get #'index-handler}
        "patient" {:get #'index-handler}
        ["patient/" :id] {:get #'index-handler}
        "api" {"health" {:get #'health}
               "/patients" {:get #'patient/get-many}
               "/patient" {:post #'patient/create-handler}
               ["/patient/" :id] {:get #'patient/get-one-handler
                                  :put #'patient/update-handler
                                  :delete #'patient/delete-handler}}
        true #'not-found}])

(defn root-handler [{req :request :as ctx}]
  (let [{:keys [uri]} req
        {:keys [handler route-params]} (bidi/match-route* routes uri req)]
    (handler (assoc-in ctx [:request :params] route-params))))

(defn app [ctx]
  (let [naked (fn [req] (root-handler (assoc ctx :request req)))]
    (if (-> ctx :config :handler :naked)
      naked
      (-> naked
          (wrap-cors :access-control-allow-origin [#"http://localhost:9500"] :access-control-allow-methods [:get :put :post :delete])
          (wrap-resource "public")
          wrap-json-response
          (wrap-json-body {:keywords? true})
          wrap-with-logger))))

(defn start [config]
  (let [ctx (atom {:config config})
        db (db/connection @ctx)
        _ (swap! ctx assoc :db db)
        handler (app @ctx)
        _ (swap! ctx assoc :handler handler)
        server (when (:server config) (start-server {:port (get-in config [:server :port])} handler))
        _ (swap! ctx assoc :server server)] ctx))

(defn stop [ctx]
  (when-let [server (:server ctx)] (.stop server)))

(def config {:server {:port 8080}
             :db {:dbname "db_dev"}})

(defn -main [& args]
  (start config))

(comment
  (def ctx (start config))
  (stop @ctx)
  (jdbc/execute! (:db @ctx) "delete from patients;")

  (def patient {:name "hello"
                :gender "female"
                :address "ул. Маяковского, 37 Рязань, Рязанская обл., 390046"
                :policy "1234123412341234"
                :birthdate "2000-01-01"})

  (def patients [{:name "Patient 1"
                  :gender "female"
                  :address "ул. Маяковского, 37 Рязань, Рязанская обл., 390046"
                  :policy "1234123412341234"
                  :birthdate "2000-01-01"}
                 {:name "Patient 2"
                  :gender "female"
                  :address "ул. Маяковского, 37 Рязань, Рязанская обл., 390046"
                  :policy "1234123412341234"
                  :birthdate "2000-01-01"}
                 {:name "Patient 3"
                  :gender "female"
                  :address "ул. Маяковского, 37 Рязань, Рязанская обл., 390046"
                  :policy "1234123412341234"
                  :birthdate "2000-01-01"}
                 {:name "Patient 4"
                  :gender "female"
                  :address "ул. Маяковского, 37 Рязань, Рязанская обл., 390046"
                  :policy "1234123412341234"
                  :birthdate "2000-01-01"}])

  (doseq [p patients]
    (let [conformed (s/conform :hs.back.spec/patient p)]
      (jdbc/insert! (:db @ctx) :patients (patient/with-created conformed))))


  (jdbc/insert! (:db @ctx) :patients patient)
  (def req {:request-method :get :uri "/"})
  (bidi/match-route* routes (:uri req) req)
  (root-handler req))
