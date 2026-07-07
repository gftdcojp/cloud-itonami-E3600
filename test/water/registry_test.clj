(ns water.registry-test
  (:require [clojure.test :refer [deftest is]]
            [water.registry :as r]))

;; ----------------------------- contaminant-level-out-of-range? -----------------------------

(deftest not-out-of-range-when-within-bounds
  (is (not (r/contaminant-level-out-of-range? {:contaminant-level 2.0 :contaminant-min 0.0 :contaminant-max 4.0})))
  (is (not (r/contaminant-level-out-of-range? {:contaminant-level 0.0 :contaminant-min 0.0 :contaminant-max 4.0})))
  (is (not (r/contaminant-level-out-of-range? {:contaminant-level 4.0 :contaminant-min 0.0 :contaminant-max 4.0}))))

(deftest out-of-range-when-below-minimum-or-above-maximum
  (is (r/contaminant-level-out-of-range? {:contaminant-level -0.5 :contaminant-min 0.0 :contaminant-max 4.0}))
  (is (r/contaminant-level-out-of-range? {:contaminant-level 6.0 :contaminant-min 0.0 :contaminant-max 4.0})))

(deftest out-of-range-is-false-on-missing-fields
  (is (not (r/contaminant-level-out-of-range? {})))
  (is (not (r/contaminant-level-out-of-range? {:contaminant-level 6.0}))))

;; ----------------------------- register-report-publication -----------------------------

(deftest report-is-a-draft-not-a-real-publication
  (let [result (r/register-report-publication "site-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest report-assigns-report-number
  (let [result (r/register-report-publication "site-1" "JPN" 7)]
    (is (= (get result "report_number") "JPN-RPT-000007"))
    (is (= (get-in result ["record" "site_id"]) "site-1"))
    (is (= (get-in result ["record" "kind"]) "report-publication-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest report-validation-rules
  (is (thrown? Exception (r/register-report-publication "" "JPN" 0)))
  (is (thrown? Exception (r/register-report-publication "site-1" "" 0)))
  (is (thrown? Exception (r/register-report-publication "site-1" "JPN" -1))))

;; ----------------------------- register-alert-suppression -----------------------------

(deftest suppression-is-a-draft-not-a-real-suppression
  (let [result (r/register-alert-suppression "site-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest suppression-assigns-suppression-number
  (let [result (r/register-alert-suppression "site-1" "JPN" 3)]
    (is (= (get result "suppression_number") "JPN-SUP-000003"))
    (is (= (get-in result ["record" "site_id"]) "site-1"))
    (is (= (get-in result ["record" "kind"]) "alert-suppression-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest suppression-validation-rules
  (is (thrown? Exception (r/register-alert-suppression "" "JPN" 0)))
  (is (thrown? Exception (r/register-alert-suppression "site-1" "" 0)))
  (is (thrown? Exception (r/register-alert-suppression "site-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-report-publication "site-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-report-publication "site-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-RPT-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-RPT-000001" (get-in hist2 [1 "record_id"])))))
