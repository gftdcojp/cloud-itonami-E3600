(ns water.facts
  "Per-jurisdiction drinking-water-safety regulatory catalog -- the
  G2-style spec-basis table the Water Safety Governor checks every
  jurisdiction/assess proposal against ('did the advisor cite an
  OFFICIAL public source for this jurisdiction's drinking-water-
  quality/public-reporting requirements, or did it invent one?').

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries.

  Seed values are drawn from each jurisdiction's official drinking-
  water regulator (see `:provenance`); they are a STARTING catalog,
  not a from-scratch survey of all ~194 jurisdictions. Extending
  coverage is additive: add one map to `catalog`, cite a real source,
  done -- never invent a jurisdiction's requirements to make coverage
  look bigger.

  The JPN entry cites the CURRENT (post-April-2024) regulatory split:
  water-SUPPLY-infrastructure oversight transferred from the Ministry
  of Health, Labour and Welfare (MHLW) to the Ministry of Land,
  Infrastructure, Transport and Tourism (MLIT), while drinking-water-
  QUALITY-standard-setting transferred to the Ministry of the
  Environment (MOE) -- both under the same underlying 水道法
  (Waterworks Act), a real, dated regulatory-reorganization fact this
  catalog reflects rather than citing the now-superseded MHLW-only
  structure most pre-2024 sources would show.")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the generic
  site-intake/sensor-calibration-provenance/lab-result-chain-of-
  custody/public-reporting-source evidence set submitted in some form;
  `:legal-basis` / `:owner-authority` / `:provenance` are the G2
  citation the governor requires before any :jurisdiction/assess
  proposal can commit."
  {"JPN" {:name "Japan"
          :owner-authority "国土交通省 (MLIT, water-supply infrastructure) / 環境省 (MOE, water-quality standards)"
          :legal-basis "水道法 (Waterworks Act)"
          :national-spec "水質基準に関する省令・水道施設維持管理基準"
          :provenance "https://www.env.go.jp/"
          :required-evidence ["施設登録記録 (site-intake record)"
                              "センサー校正証明書 (sensor-calibration-provenance certificate)"
                              "検査結果連鎖記録 (lab-result-chain-of-custody record)"
                              "公開報告根拠資料 (public-reporting-source document)"]}
   "USA" {:name "United States"
          :owner-authority "Environmental Protection Agency (EPA)"
          :legal-basis "Safe Drinking Water Act (SDWA, 42 U.S.C. §300f et seq.)"
          :national-spec "National Primary Drinking Water Regulations (NPDWR) monitoring and reporting requirements"
          :provenance "https://www.epa.gov/ground-water-and-drinking-water"
          :required-evidence ["Site-intake record"
                              "Sensor-calibration-provenance certificate"
                              "Lab-result-chain-of-custody record"
                              "Public-reporting-source document"]}
   "GBR" {:name "United Kingdom"
          :owner-authority "Drinking Water Inspectorate (DWI)"
          :legal-basis "Water Industry Act 1991 / Water Supply (Water Quality) Regulations 2016"
          :national-spec "Drinking-water-quality monitoring, sampling and public-reporting standards"
          :provenance "https://www.dwi.gov.uk/"
          :required-evidence ["Site-intake record"
                              "Sensor-calibration-provenance certificate"
                              "Lab-result-chain-of-custody record"
                              "Public-reporting-source document"]}
   "DEU" {:name "Germany"
          :owner-authority "Umweltbundesamt (UBA) / Landesgesundheitsämter"
          :legal-basis "Trinkwasserverordnung (TrinkwV, Drinking Water Ordinance)"
          :national-spec "Trinkwasserüberwachungs- und Berichterstattungspflichten"
          :provenance "https://www.umweltbundesamt.de/"
          :required-evidence ["Anlagenregistrierung (site-intake record)"
                              "Sensorkalibrierungsnachweis (sensor-calibration-provenance certificate)"
                              "Laborergebniskettennachweis (lab-result-chain-of-custody record)"
                              "Veröffentlichungsquellendokument (public-reporting-source document)"]}
   ;; FRA citations independently fetched+read from legifrance.gouv.fr
   ;; (2026-07-22): Art. R1321-2 sets quality limits "définies par arrêté du
   ;; ministre chargé de la santé" (in effect since 01/01/2023); Art. L1321-4
   ;; ("en vigueur depuis" 24 décembre 2022) obliges every "fournisseur d'eau"
   ;; to self-monitor quality, submit to contrôle sanitaire, take corrective
   ;; measures and inform consumers of any health risk; Art. R1321-15 (in
   ;; effect since 01/01/2023) states the contrôle sanitaire mentioned in
   ;; L1321-4 "est exercé par l'agence régionale de santé". Chapter title
   ;; verbatim from the Légifrance section page: "Chapitre Ier : Eaux
   ;; potables (Articles R1321-1 à D1321-106)". Higher-level Livre/Titre
   ;; numbering was NOT independently confirmed (the whole-code
   ;; table-of-contents fetch exceeded the fetch tool's size limit) so it
   ;; is deliberately omitted here rather than guessed.
   "FRA" {:name "France"
          :owner-authority "Agences Régionales de Santé (ARS, contrôle sanitaire) / Ministère chargé de la Santé (limites de qualité par arrêté)"
          :legal-basis "Code de la santé publique, Chapitre Ier \"Eaux potables\" (Art. L.1321-4, R.1321-1 et suivants)"
          :national-spec "Limites et références de qualité microbiologiques et physico-chimiques fixées par arrêté du ministre chargé de la santé (Art. R.1321-2) ; obligations du fournisseur d'eau -- auto-surveillance, contrôle sanitaire, mesures correctives et information des consommateurs en cas de risque sanitaire (Art. L.1321-4) ; contrôle sanitaire exercé par l'agence régionale de santé (Art. R.1321-15)"
          :provenance "https://www.legifrance.gouv.fr/codes/section_lc/LEGITEXT000006072665/LEGISCTA000006178501/"
          :required-evidence ["Enregistrement de l'installation (site-intake record)"
                              "Certificat d'étalonnage des capteurs (sensor-calibration-provenance certificate)"
                              "Chaîne de traçabilité des résultats d'analyse (lab-result-chain-of-custody record)"
                              "Document source d'information des consommateurs (public-reporting-source document)"]}
   ;; NZL citations independently fetched+read this session (2026-07-23).
   ;; taumataarowai.govt.nz was fetched directly and confirms: the regulator's
   ;; site distinguishes "Drinking Water Standards" (composition/MAV limits)
   ;; from the separate "Drinking Water Quality Assurance Rules" (monitoring/
   ;; process), and that its "how-to-guidance/monitoring-and-reporting" page
   ;; states "Reporting the outcomes of your monitoring to us is key to
   ;; ensuring that monitoring and treatment systems, processes and equipment
   ;; are working effectively" -- the same monitoring+reporting angle this
   ;; catalog's other entries cite. legislation.govt.nz returned an AWS WAF
   ;; bot-detection challenge on every direct fetch attempt (HTTP 202,
   ;; `x-amzn-waf-action: challenge` header, confirmed via a plain HTTP
   ;; HEAD/GET, no automated bypass attempted per this fleet's hard safety
   ;; rule); the Act and Regulations text below was instead independently
   ;; fetched and read via the Internet Archive Wayback Machine snapshots
   ;; https://web.archive.org/web/20251119005830/https://legislation.govt.nz/act/public/2021/0036/latest/whole.html
   ;; (Water Services Act 2021) and
   ;; https://web.archive.org/web/20251221120557/https://www.legislation.govt.nz/regulation/public/2022/0168/latest/whole.html
   ;; (Water Services (Drinking Water Standards for New Zealand) Regulations
   ;; 2022). Verbatim confirmed: reg title "Water Services (Drinking Water
   ;; Standards for New Zealand) Regulations 2022" (SL 2022/168), made under
   ;; s 47 of the Water Services Act 2021, in force 14 November 2022, whose
   ;; Schedule sets e.g. "Escherichia coli / Less than 1 in 100 mL of
   ;; sample"; its Explanatory note states the standards "revoke and replace
   ;; the Drinking-water Standards for New Zealand 2005 (revised 2018)". Act
   ;; s 22(1)-(2) verbatim: "A drinking water supplier must ensure that the
   ;; drinking water supplied by the supplier complies with the drinking
   ;; water standards. ... If a supplier's drinking water does not comply
   ;; with the drinking water standards, the supplier must-- ... notify the
   ;; Water Services Authority of the non-compliance ... [and] advise
   ;; affected consumers and drinking water suppliers that drinking water
   ;; does not comply with the drinking water standards". Act s 137(1)/(3)
   ;; verbatim: "the Water Services Authority must ensure that, on an annual
   ;; basis, it prepares a report on ... (c) compliance rates of drinking
   ;; water suppliers with the drinking water standards ... [and] must
   ;; ensure that the report is published". Act s 5 verbatim: "Water
   ;; Services Authority means the Water Services Authority--Taumata Arowai
   ;; established by section 8 of the Water Services Authority--Taumata
   ;; Arowai Act 2020" -- confirming the regulator (branded "Taumata Arowai"
   ;; on its own site) was renamed by the Local Government (Water Services)
   ;; (Repeals and Amendments) Act 2025 (2025 No 43, in force 27 August
   ;; 2025) from its original name "Taumata Arowai--the Water Services
   ;; Regulator Act 2020" (2020 No 52), both forms independently confirmed
   ;; present in the fetched Act text.
   "NZL" {:name "New Zealand"
          :owner-authority "Water Services Authority -- Taumata Arowai, established under the Water Services Authority--Taumata Arowai Act 2020 (renamed 27 August 2025 from Taumata Arowai--the Water Services Regulator Act 2020)"
          :legal-basis "Water Services Act 2021 (ss 22, 47, 137) / Water Services (Drinking Water Standards for New Zealand) Regulations 2022 (SL 2022/168)"
          :national-spec "Maximum acceptable values (MAVs) for microbiological, inorganic, organic and radiological determinands in drinking water (e.g. Escherichia coli: less than 1 per 100 mL sample) set under Act s 47 (Regulations 2022 r 4 and Schedule); a supplier must notify the Water Services Authority and advise affected consumers of any non-compliance (Act s 22); the Water Services Authority must publish an annual public report on national drinking-water compliance and safety (Act s 137)"
          :provenance "https://www.legislation.govt.nz/regulation/public/2022/0168/latest/whole.html"
          :required-evidence ["Site-intake record"
                              "Sensor-calibration-provenance certificate"
                              "Lab-result-chain-of-custody record"
                              "Public-reporting-source document"]}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to publish a
  report or suppress an alert on it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-3600 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `water.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))
