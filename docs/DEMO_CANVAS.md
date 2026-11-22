# Live Demo Canvas & Clinical Scenario Runbook

> **Educational Demo Notice:** Hospital Discharge Companion is a simulated educational prototype. It is not a medical device, does not provide medical advice, and never initiates real emergency dispatch.

---

## 1. Seeded Clinical Discharge Episodes

| Episode ID | Patient | Diagnosis | Risk Tier | Unit | Care-Circle Code |
| :--- | :--- | :--- | :---: | :--- | :--- |
| `ep-hf-001` | João Silva (68) | Heart Failure (HFrEF) Exacerbation | High | Cardiology Step-Down 4B | `HDC-HF-402` |
| `ep-ps-002` | Maria Oliveira (54) | Post-Operative Laparoscopic Cholecystectomy | Medium | Surgical Recovery 2A | `HDC-PS-719` |
| `ep-pn-003` | Arthur Bennett (72) | Community-Acquired Pneumonia (Recovery) | Low | Internal Medicine 3C | `HDC-PN-285` |

---

## 2. One-Tap Clinical Scenarios (Demo Controls)

Open **Demo Controls & About** (`hdc://screen/demo`) to jump directly into any showcase state during a live presentation:

| Preset Chip | Seeded State Produced | Best Screen to Open Next |
| :--- | :--- | :--- |
| **Full Adherence** | Marks all active discharge medications taken and tasks completed on `ep-hf-001`. | `Patient Home` / `Care Plan` |
| **Missed Diuretic** | Logs skipped Furosemide 40 mg dose and triggers an urgent adherence alert. | `Medications` / `Alerts` |
| **Critical BP Alert** | Submits abnormal BP `174/106 mmHg` + `79.2 kg` dry-weight surge, firing critical alerts. | `Telemonitoring` / `Nurse Triage` |
| **EHR 409 Conflict** | Configures HTTP 409 fault mode and generates an open FHIR `Observation` conflict. | `EHR Sync` |
| **Caregiver Confirmed** | Logs morning meds with caregiver dual attribution (`Maria Silva on behalf of João`). | `Family Caregiver Hub` / `Medications` |
| **Physician Plan v2** | Signs off Care Plan revision v2 with an evening dry-weight check and earlier follow-up. | `Physician Escalations` / `Care Plan` |

---

## 3. Recommended 5–7 Minute Live Walkthrough

1. **Reset & Locale Check (0:30):** Open **Demo Controls**, tap **Reset Demo**, confirm **English** (or demonstrate live switching across `en`, `pt-BR`, `es`, `fr`) and **System / Light / Dark** theme mode.
2. **Patient Self-Care Loop (1:30):** Enter **Patient** (`João Silva`), inspect **Medications** reconciliation counts (`NEW` / `CHANGED` / `CONTINUED` / `STOPPED`), confirm morning Furosemide, then open **Vitals & Check-In**, tap **Simulate High BP**, and submit.
3. **Family Caregiver Escalation (1:00):** Switch to **Family Caregiver**, point out **Consent Verified** and dual attribution, review the triggered alert, and tap **Escalate to Nurse**.
4. **Nurse Triage & Handoff (1:30):** Switch to **Nurse**, inspect the **Triage Alert Queue**, open **Export Handoff** for the shift summary, document a structured note via **Note Templates**, and tap **Escalate MD**.
5. **Physician Sign-Off (1:00):** Switch to **Physician**, inspect the **Clinical Sheet** (vitals, inline medications, revision history), choose **Earlier Follow-Up**, and tap **Sign Off New Care Plan Revision** (`v2`).
6. **FHIR R4 Bundle & 409 Conflict Resolution (1:00):** Open **EHR Sync**, tap **Inspect FHIR Bundle** to view the mock FHIR R4 JSON payload, toggle **Conflict (409)**, tap **Push Vitals & Adherence**, and reconcile with **Keep Local Home Data**.

---

## 4. Deep Link Reference (`hdc://`)

```bash
# Jump to a specific discharge episode or highlighted alert/dose
adb shell am start -a android.intent.action.VIEW -d "hdc://episode/ep-hf-001" com.hospitaldischargecompanion/.MainActivity
adb shell am start -a android.intent.action.VIEW -d "hdc://dose/med-hf-furosemide/ep-hf-001" com.hospitaldischargecompanion/.MainActivity
adb shell am start -a android.intent.action.VIEW -d "hdc://alert/alt-hf-001/ep-hf-001" com.hospitaldischargecompanion/.MainActivity

# Direct screen routes used for demos and automated UI tests
# profile | patient | family | nurse | physician | careplan | medications |
# telemonitoring | alerts | messaging | appointments | education | ehr | demo
adb shell am start -a android.intent.action.VIEW -d "hdc://screen/nurse" com.hospitaldischargecompanion/.MainActivity
```
