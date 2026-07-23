(ns water.facts-test
  (:require [clojure.test :refer [deftest is]]
            [water.facts :as facts]))

(deftest jpn-has-a-spec-basis
  (is (some? (facts/spec-basis "JPN")))
  (is (string? (:provenance (facts/spec-basis "JPN")))))

(deftest fra-has-a-spec-basis
  (is (some? (facts/spec-basis "FRA")))
  (is (string? (:provenance (facts/spec-basis "FRA"))))
  (is (= "France" (:name (facts/spec-basis "FRA"))))
  (is (re-find #"Code de la santé publique" (:legal-basis (facts/spec-basis "FRA")))))

(deftest nzl-has-a-spec-basis
  (is (some? (facts/spec-basis "NZL")))
  (is (string? (:provenance (facts/spec-basis "NZL"))))
  (is (= "New Zealand" (:name (facts/spec-basis "NZL"))))
  (is (re-find #"Water Services Act 2021" (:legal-basis (facts/spec-basis "NZL")))))

(deftest irl-has-a-spec-basis
  (is (some? (facts/spec-basis "IRL")))
  (is (string? (:provenance (facts/spec-basis "IRL"))))
  (is (= "Ireland" (:name (facts/spec-basis "IRL"))))
  (is (re-find #"European Union \(Drinking Water\) Regulations 2023" (:legal-basis (facts/spec-basis "IRL")))))

(deftest unknown-jurisdiction-has-no-fabricated-spec-basis
  (is (nil? (facts/spec-basis "ATL"))))

(deftest coverage-never-reports-a-missing-jurisdiction-as-covered
  (let [report (facts/coverage ["JPN" "ATL" "GBR" "FRA" "NZL"])]
    (is (= 4 (:covered report)))
    (is (= ["ATL"] (:missing-jurisdictions report)))
    (is (= ["FRA" "GBR" "JPN" "NZL"] (:covered-jurisdictions report)))))

(deftest required-evidence-satisfied-needs-every-item
  (let [all (facts/evidence-checklist "JPN")]
    (is (facts/required-evidence-satisfied? "JPN" all))
    (is (not (facts/required-evidence-satisfied? "JPN" (rest all))))
    (is (not (facts/required-evidence-satisfied? "ATL" all)) "no spec-basis -> never satisfied")))

(deftest fra-required-evidence-satisfied-needs-every-item
  (let [all (facts/evidence-checklist "FRA")]
    (is (= 4 (count all)))
    (is (facts/required-evidence-satisfied? "FRA" all))
    (is (not (facts/required-evidence-satisfied? "FRA" (rest all))))))

(deftest nzl-required-evidence-satisfied-needs-every-item
  (let [all (facts/evidence-checklist "NZL")]
    (is (= 4 (count all)))
    (is (facts/required-evidence-satisfied? "NZL" all))
    (is (not (facts/required-evidence-satisfied? "NZL" (rest all))))))

(deftest irl-required-evidence-satisfied-needs-every-item
  (let [all (facts/evidence-checklist "IRL")]
    (is (= 4 (count all)))
    (is (facts/required-evidence-satisfied? "IRL" all))
    (is (not (facts/required-evidence-satisfied? "IRL" (rest all))))))
