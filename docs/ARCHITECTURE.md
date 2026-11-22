# Architecture & Phase 2 FHIR Integration Guide

> **Disclaimer:** Hospital Discharge Companion is an educational demonstration app. It is not a medical device, not intended for real clinical care, and does not dispatch real emergency services.

---

## 1. Layered Unidirectional Data Flow (UDF)

Every clinical feature follows a strict unidirectional data flow boundary so Phase 1 mock adapters can be replaced by live EHR / FHIR R4 endpoints in Phase 2 without rewriting any Jetpack Compose screen or ViewModel.

```text
Compose Screen (stateless UI + HdcTestTags)
   │  collectAsStateWithLifecycle()
   ▼
Hilt ViewModel (StateFlow<UiState>, UI intents, RBAC guards)
   │  suspend / Flow<T>
   ▼
Repository Interface (domain contract + DomainResult<T>)
   │  bound in core/di/*Module.kt
   ▼
DataSource / MockApi (latency & fault injection via DemoNetworkSimulator)
   │
   ▼
MockClinicalStore (shared singleton in-memory source of truth across all 4 roles)
```

### Key Architectural Guarantees
- **Single Shared Episode State:** Switching between **Patient**, **Family Caregiver**, **Nurse**, and **Physician** reads and mutates the exact same `MockClinicalStore` `StateFlow` snapshots in real time.
- **Session Persistence:** `DataStoreDemoSessionRepository` persists active role, selected episode ID, UI locale (`en`, `pt-BR`, `es`, `fr`), temperature unit (`°C`/`°F`), theme mode (`SYSTEM`/`LIGHT`/`DARK`), quiet-hours toggle, and caregiver alert sharing across app restarts.
- **Typed Navigation & Deep Links:** `HdcRoutes` uses `@Serializable` route objects consumed by `HdcNavHost`. External/in-app `hdc://` links are parsed deterministically by `DeepLinkParser`.

---

## 2. Role-Based Access Control (RBAC) Matrix

Capabilities are enforced via `UserRole` predicates and verified by `RbacCapabilityMatrixTest`:

| Capability | Patient | Family Caregiver | Nurse | Physician |
| :--- | :---: | :---: | :---: | :---: |
| View care plan, meds, vitals, education, appointments | Yes | Yes | Yes | Yes |
| Complete / skip tasks & log doses (with dual attribution) | Yes | Yes | Yes | Yes |
| Escalate open alerts along ladder | Yes | Yes | Yes | Yes |
| Acknowledge / snooze / resolve alerts (`isClinician`) | No | No | Yes | Yes |
| Reopen & reassign care-plan tasks (`canReopenTasks`) | No | No | Yes | Yes |
| Post clinical notes & approve reschedules (`canWriteClinicalNotes`) | No | No | Yes | Yes |
| Sign off versioned Care Plan amendments (`canModifyCarePlan`) | No | No | No | Yes |

---

## 3. Phase 1 Mock to Phase 2 FHIR R4 Migration Map

All EHR payloads are modeled under `core/ehr/FhirDtos.kt` and serialized into an inspectable mock FHIR R4 `Bundle` (`FhirBundleSerializer`):

| Clinical Capability | Phase 1 Mock Class | Target FHIR R4 Resource |
| :--- | :--- | :--- |
| Patient & Encounter Snapshot | `MockDischargeEpisodeApi` | `Patient`, `Encounter` |
| Versioned Care Plan & Tasks | `MockCarePlanApi` | `CarePlan`, `Task` |
| Discharge Medications & Adherence | `MockMedicationApi` | `MedicationRequest`, `MedicationStatement` |
| Vitals & Symptom Telemonitoring | `MockTelemonitoringApi` | `Observation` (vital-signs panel) |
| Clinical Alerts & Escalation Audit | `MockAlertApi` | `DetectedIssue`, `Flag`, `AuditEvent` |
| Care Team Messaging | `MockMessagingApi` | `Communication` |
| Follow-Up Visits & Rescheduling | `MockAppointmentApi` | `Appointment` |
| Outbound/Inbound EHR Sync | `MockEhrSyncApi` | `Bundle` (collection / transaction) |

To migrate to Phase 2:
1. Implement Retrofit / OkHttp FHIR clients satisfying the existing repository interfaces in `core/*/*Repository.kt`.
2. Swap the Hilt `@Binds` implementations inside `core/di/*Module.kt`.
3. Map HTTP `401`, `409`, timeout, and offline errors to the existing `DomainError` sealed hierarchy (`Unauthorized`, `Conflict`, `Timeout`, `NetworkUnavailable`).
4. Replace `LocalReminderDispatcher` triggers with Firebase Cloud Messaging (FCM) push handlers while reusing `NotificationPolicy` quiet-hours gating.

---

## 4. Internationalization & Testing Strategy

- **4 UI Locales:** `values/` (`en`, default), `values-pt-rBR/` (`pt-BR`), `values-es/` (`es`), and `values-fr/` (`fr`).
- **Automated Parity Enforcement:** `StringResourceParityTest` parses all four `strings.xml` files in `testDebugUnitTest` and fails the build if any key is missing or orphaned in any locale.
- **Localized Seed Overlays:** `LocalizedClinicalPack` overlays translated discharge diagnoses, summaries, and education guidance alongside `ClinicalFormatter` locale-aware number, date, and temperature formatting.
- **Test Pyramid:**
  - **JVM Unit Tests (`app/src/test`):** 62 deterministic tests covering repositories, alert/missed-check-in rules, ViewModels, RBAC matrix, FHIR Bundle serialization, notification quiet-hours policy, and full 4-role clinical loop.
  - **Compose UI Instrumented Tests (`app/src/androidTest`):** 13 end-to-end instrumented UI tests covering all 14 screens using stable `HdcTestTags` and `hdc://screen/*` deep links.
