(ns water.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a configuration
  change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the sibling
  actor."
  (:require [clojure.test :refer [deftest is testing]]
            [water.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "Sakura Community Well" (:site-name (store/site s "site-1"))))
      (is (= "JPN" (:jurisdiction (store/site s "site-1"))))
      (is (= 2.0 (:contaminant-level (store/site s "site-1"))))
      (is (= 0.0 (:contaminant-min (store/site s "site-1"))))
      (is (= 4.0 (:contaminant-max (store/site s "site-1"))))
      (is (false? (:threshold-breach-unresolved? (store/site s "site-1"))))
      (is (= 6.0 (:contaminant-level (store/site s "site-3"))))
      (is (true? (:threshold-breach-unresolved? (store/site s "site-4"))))
      (is (false? (:report-published? (store/site s "site-1"))))
      (is (false? (:alert-suppressed? (store/site s "site-1"))))
      (is (= ["site-1" "site-2" "site-3" "site-4"]
             (mapv :id (store/all-sites s))))
      (is (nil? (store/threshold-screen-of s "site-1")))
      (is (nil? (store/assessment-of s "site-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/report-history s)))
      (is (= [] (store/suppression-history s)))
      (is (zero? (store/next-report-sequence s "JPN")))
      (is (zero? (store/next-suppression-sequence s "JPN")))
      (is (false? (store/site-already-published? s "site-1")))
      (is (false? (store/site-already-suppressed? s "site-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :site/upsert
                                 :value {:id "site-1" :site-name "Sakura Community Well"}})
        (is (= "Sakura Community Well" (:site-name (store/site s "site-1"))))
        (is (= 2.0 (:contaminant-level (store/site s "site-1"))) "unrelated field preserved"))
      (testing "assessment / threshold-screen payloads commit and read back"
        (store/commit-record! s {:effect :assessment/set :path ["site-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/assessment-of s "site-1")))
        (store/commit-record! s {:effect :threshold-screen/set :path ["site-1"]
                                 :payload {:site-id "site-1" :verdict :resolved}})
        (is (= {:site-id "site-1" :verdict :resolved} (store/threshold-screen-of s "site-1"))))
      (testing "report publication drafts a report record and advances the sequence"
        (store/commit-record! s {:effect :site/mark-published :path ["site-1"]})
        (is (= "JPN-RPT-000000" (get (first (store/report-history s)) "record_id")))
        (is (= "report-publication-draft" (get (first (store/report-history s)) "kind")))
        (is (true? (:report-published? (store/site s "site-1"))))
        (is (= 1 (count (store/report-history s))))
        (is (= 1 (store/next-report-sequence s "JPN")))
        (is (true? (store/site-already-published? s "site-1")))
        (is (false? (store/site-already-published? s "site-2"))))
      (testing "alert suppression drafts a record and advances the sequence"
        (store/commit-record! s {:effect :site/mark-suppressed :path ["site-1"]})
        (is (= "JPN-SUP-000000" (get (first (store/suppression-history s)) "record_id")))
        (is (= "alert-suppression-draft" (get (first (store/suppression-history s)) "kind")))
        (is (true? (:alert-suppressed? (store/site s "site-1"))))
        (is (= 1 (count (store/suppression-history s))))
        (is (= 1 (store/next-suppression-sequence s "JPN")))
        (is (true? (store/site-already-suppressed? s "site-1")))
        (is (false? (store/site-already-suppressed? s "site-2"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/site s "nope")))
    (is (= [] (store/all-sites s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/report-history s)))
    (is (= [] (store/suppression-history s)))
    (is (zero? (store/next-report-sequence s "JPN")))
    (is (zero? (store/next-suppression-sequence s "JPN")))
    (store/with-sites s {"x" {:id "x" :site-name "n" :contaminant-level 2.0
                             :contaminant-min 0.0 :contaminant-max 4.0
                             :threshold-breach-unresolved? false
                             :report-published? false :alert-suppressed? false
                             :jurisdiction "JPN" :status :intake}})
    (is (= "n" (:site-name (store/site s "x"))))))
