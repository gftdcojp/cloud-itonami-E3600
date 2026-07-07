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
                              "Veröffentlichungsquellendokument (public-reporting-source document)"]}})

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
