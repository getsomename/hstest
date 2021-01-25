(ns ^:figwheel-hooks hs.main
  (:require
   [reagent.dom :as r.dom]
   [reagent.core :as r]
   [hs.http :as http]))

(def route (r/atom {:path "patient" :id "f64d1806-35e0-42b6-b7ff-25f37576229c"}))

(def patients (r/atom []))

(defn get-patients [] (http/GET "/api/patients" #(reset! patients (:patients %))))


(get-patients)

(defn delete-patient [id] (when (js/confirm "Delete patient?")
                            (http/DELETE (str "/api/patient/" id)
                              (fn [_] (swap! patients (fn [old] (remove (fn [v] (= (:id v) id)) old)))))))

(defn save-patient [patient] (http/POST "/api/patient" (:patient patient) #(js/alert "Patient saved")))

(defn update-patient [patient] (http/PUT (str "/api/patient/" (:id patient)) {:patient patient} #(js/alert "Patient updated")))

(defn patient-table []
  (fn []
    [:div [:h1 "Patients"]
     [:table
      [:thead [:tr [:th "Name"] [:th "Gender"] [:th "Birthday"] [:th "Address"] [:th "Policy"] [:th "Actions"]]]
      [:tbody (map (fn [item]
                     [:tr {:key (:id item)}
                      [:td [:a {:href "heloo"} (:name item)]]
                      [:td (:gender item)]
                      [:td (:birthday item)]
                      [:td (:address item)]
                      [:td (:policy item)]
                      [:td [:button {:on-click #(swap! route assoc :path "patient" :id (:id item))} "Edit"]
                       [:button {:on-click #(delete-patient (:id item))} "Delete"]]])
                   @patients)]]]))


(defn patient-form [p]
  (let [patient (r/atom p)
        change  (fn [key] (fn [input] (swap! patient assoc key (-> input .-target .-value))))]
    (fn []
      (if (nil? patient) [:span "loading"]
          [:form.form
           [:label {:for "Name"} "Name" (:name patient)]
           [:input {:id "Name" :placeholder "Name" :on-change (change :name)  :value (:name @patient)}]
           [:label {:for "Gender"} "Gender"]
           [:select {:id "Gender" :placeholder "Gender" :on-change (change :gender) :value (:gender @patient)}
            [:option {:value "male"} "Male"]
            [:option {:value "female"} "Female"]]
           [:label {:for "Birthday"} "Birthday"]
           [:input {:id "Birthday" :placeholder "Birthday" :type "date" :on-change (change :birthdate) :value (:birthdate @patient)}]
           [:label {:for "Address"} "Address"]
           [:input {:id "Address" :placeholder "Address" :on-change (change :address) :value (:address @patient)}]
           [:label {:for "Policy"} "Policy"]
           [:input {:id "Policy" :placeholder "Policy" :on-change (change :policy) :value (:policy @patient)}]
           [:button {:on-click #(do (.preventDefault %)
                                    (update-patient @patient))} "Save"]]))))

(defn patient-page []
  (fn [] (if (empty? @patients) [:span "Loading"] (patient-form (first (filter #(= (:id %) (:id @route)) @patients))))))

(defn app []
  (let [path  (:path @route)]
    (cond
      (= path "patient") patient-page
      :else patient-table)))

(defn mount []
  (r.dom/render [app] (js/document.getElementById "root")))


(defn ^:after-load re-render []
  (mount))


(defonce start-up (do (mount) true))
