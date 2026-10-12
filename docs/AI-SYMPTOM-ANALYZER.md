# AI symptom analyzer: implementation and learning guide

`POST /api/ai/symptom-analysis` provides AI-generated clinical decision support. It does not diagnose, prescribe medication, or replace a healthcare professional. The result always includes a server-owned disclaimer. JSON validation checks structure, not clinical correctness; model output still requires professional review.

## Gemini free-tier setup

The backend now calls **Google Gemini**, using `gemini-2.5-flash-lite` by default. Google lists this model as eligible for its Free Tier, subject to availability and quota. Keep the Google project on the Free Tier; the application cannot enforce billing settings for a paid Google project. Use fictional patient data: Google's free-tier pricing page says data may be used to improve its products.

1. Create a Gemini key in [Google AI Studio](https://aistudio.google.com/apikey). Do not put the key in source code or Postman.
2. In STS/Eclipse, stop the app and open **Run → Run Configurations → your application → Environment**.
3. Add or edit `AI_API_KEY` to contain your **Gemini key**. The variable name is still `AI_API_KEY`.
4. Remove any old `AI_MODEL=gpt-4o-mini` override, or set `AI_MODEL=gemini-2.5-flash-lite`.
5. Click **Apply → Run**. Log in again at `/auth/login` because this application's JWT signing key changes on restart.
6. Put the newly issued patient JWT in Postman's **Authorization → Bearer Token** and resend the same symptom-analysis request with the patient's actual ID.

The existing `spring-ai-openai` dependency and Java class names implement the **wire protocol**, not the hosting provider. `AIConfig` explicitly targets `https://generativelanguage.googleapis.com/v1beta/openai/chat/completions`. It does not call OpenAI or fall back to a paid provider. No dependency or Spring Boot upgrade is needed for this switch.

If the response says **Gemini usage limit reached**, wait for the applicable quota to reset and check your AI Studio limits. If it says **Gemini rejected the request**, check the key, model override, and project/model access. Provider error bodies and credentials are never returned to clients. Automated tests mock Google's HTTP endpoint; they do not verify your personal API key or live quota.

## Existing architecture and reuse

The application runs Java 17 / Spring Boot 3.4.0. REST controllers use services and Spring Data JPA repositories. MySQL holds `Patient`, `Doctor`, `Admin`, `HealthMetrics`, and appointments. Lombok supplies ordinary DTO/entity accessors. There is no shared `User` entity: `CustomUserDetailsService` resolves separate account tables, and `JwtRequestFilter` installs their roles in Spring Security. Existing `GlobalExceptionHandler` maps exceptions to REST responses.

The feature reuses `Patient`, `HealthMetrics`, `PatientRepository`, `HealthMetricsRepository`, the existing JWT filter and user details service, and global exception handling. The new entity belongs in the project's `model` package. Existing metric services return personal identifiers and treat absent metrics as an error; the analyzer uses the existing repositories directly to construct a minimal clinical record and allow missing metrics.

Doctors already have role-wide access to patient records and health metrics. The analyzer preserves this rule: it does not invent an appointment-based assignment rule. Patients must match their authenticated username to their own database patient ID. Admin-only accounts cannot use this endpoint.

## Stage 1 — Dependencies and configuration

**Changed:** `pom.xml`, `config/AIProperties.java`, `config/AIConfig.java`, `src/main/resources/ai.properties`.

**Why/how:** Spring AI BOM 1.0.9 manages AI dependency versions. The `spring-ai-openai` library supplies the model client; a small explicit bean configures this single provider without auto-configuring unused AI services. Boot stays at **3.4.0**. Spring AI's 1.0 documentation supports Boot 3.4.x and 3.5.x. Springdoc 1.8.0 (Boot 2 generation) is replaced with `springdoc-openapi-starter-webmvc-ui:2.7.0`, compatible with Boot 3.4.0. A newer 2.8.17 patch failed the application's startup test, so 2.7.0 is used and tested. Validation adds Hibernate Validator; H2 is test scope only.

`AIConfig` loads the separate `ai.properties` file, preserving existing local edits to `application.properties`. The model bean is lazy, so other application features start without an AI key. The missing-key path returns 503. No real API key is committed.

| Environment variable | Default | Purpose |
| --- | --- | --- |
| `AI_API_KEY` | empty | Google AI Studio Gemini API credential |
| `AI_MODEL` | `gemini-2.5-flash-lite` | Gemini model supporting structured JSON output |
| `AI_CONNECT_TIMEOUT_SECONDS` | 5 | Connection timeout, 1–60 seconds |
| `AI_READ_TIMEOUT_SECONDS` | 30 | Socket read timeout, 1–120 seconds |
| `AI_SAVE_HISTORY` | false | Persist validated consultations |

Read timeout is a transport timeout, not a strict wall-clock deadline. Retries are disabled to avoid repeated delays and duplicate charges. Model options use temperature 0 and a 1,800-token output limit; selecting another model requires compatibility with these options and structured output.

**Test:** `./mvnw.cmd -DskipTests compile`. Start without `AI_API_KEY` and confirm the existing app starts; an authorized valid analysis request returns 503. Set environment variables in the IDE run configuration or terminal session before starting the app. Spring Boot does not automatically read a `.env` file.

## Stage 2 — Request and response DTOs

**Changed:** `dto/SymptomAnalysisRequest.java`, `dto/SymptomAnalysisResponse.java`.

**Why/how:** Jakarta validation requires a positive nonnull `patientId`, 1–20 symptoms, and nonblank symptom strings up to 200 characters. DTOs separate HTTP input/output from database entities. Risk level is an enum: `LOW`, `MODERATE`, `HIGH`, `EMERGENCY`. Response arrays must be present and have no more than 10 bounded, nonblank items, but may be empty. Specialist/explanation must be nonblank and bounded. The server adds a disclaimer the model cannot replace.

**Test:** submit `{"patientId":1,"symptoms":[]}`, a zero ID, or a blank symptom as an authenticated patient; expect 400 and no AI call. See `AIClinicalServiceTest` and `AIClinicalControllerTest`.

## Stage 3 — AI service and provider boundary

**Changed:** `service/AIClinicalService.java`, `service/ClinicalAIClient.java`, `service/impl/AIClinicalServiceImpl.java`, `service/impl/SpringAIClinicalClient.java`.

**Why/how:** `ClinicalAIClient` is the small mockable provider boundary. The adapter sends a system safety prompt and a separate JSON user message through Spring AI. Native strict JSON schema limits output fields and risk levels. Non-STOP completion, empty output, refusal text, malformed JSON, duplicate keys, trailing JSON, unexpected fields, numeric enum/string coercion, missing fields, and out-of-bounds values are rejected. Raw AI text is never returned as a fallback. Error messages never include the provider body or key.

The prompt prohibits definitive diagnosis, medications and dosages, and fabrication. It requests possibilities, professional review, and emergency warning signs. Symptom/history strings are explicitly untrusted data, not instructions. These prompt instructions and structural checks do not guarantee clinical correctness or eliminate prompt injection.

**Test:** `./mvnw.cmd '-Dtest=AIClinicalServiceTest,SpringAIClinicalClientTest' test`. These tests mock the provider; no paid AI call is made.

## Stage 4 — Controller and Swagger

**Changed:** `controller/AIClinicalController.java`, `config/ClinicalOpenApiConfig.java`.

**Why/how:** The controller validates the body, passes the existing authenticated principal to the service, and returns the typed response. OpenAPI annotations describe the endpoint, role/ownership rules, example request/response, bearer JWT, and failure codes. Swagger documentation/UI paths are public; application endpoints retain their authentication checks.

**Test:** open `/swagger-ui/index.html` on the application's configured port. Authenticate via `/auth/login`, copy only the token after `JWT Token: `, and use Swagger's **Authorize** button. Submit:

```json
{"patientId":1,"symptoms":["fever","cough","shortness of breath"]}
```

Example only; actual output varies:

```json
{
  "riskLevel":"HIGH",
  "possibleConditions":["Possible respiratory infection"],
  "recommendedTests":["Clinical examination"],
  "recommendedSpecialist":"Primary care clinician",
  "warningSigns":["Worsening difficulty breathing"],
  "explanation":"AI-generated decision support; these symptoms may require evaluation by a qualified healthcare professional.",
  "disclaimer":"AI-generated decision support only; not a diagnosis. Review by a qualified healthcare professional is required. If you believe you are experiencing an emergency, seek immediate medical assistance; do not wait for this analysis."
}
```

## Stage 5 — Existing patient and metrics context

**Changed:** one derived query on `HealthMetricsRepository`; context construction in the clinical service.

**Why/how:** `findTop5ByPatientOrderByTimestampDescIdDesc` retrieves a bounded, deterministic recent history. The prompt includes age when greater than zero, `medicalCondition` (bounded to 4,000 characters), symptoms, and explicitly selected metric fields and timestamps. Nulls mean unknown. No name, username, email, password, patient ID, or entity relationships are serialized into the prompt. Free-text symptoms/history can still contain personal information entered by a user.

Age is a primitive integer in the existing schema, so zero cannot reliably distinguish unknown age from an infant and is sent as unknown. The existing metrics schema stores no units; the prompt explicitly avoids assuming ambiguous units. Historical records are not presented as current observations. No metrics is a valid case.

**Test:** service tests inspect the outgoing prompt for clinical values and absence of account identifiers. The integration test inserts seven metrics and verifies only the newest five, in timestamp/ID order, are used.

## Stage 6 — Optional consultation history

**Changed:** `model/AIConsultation.java`, `repository/AIConsultationRepository.java` and optional service save.

**Why/how:** When `AI_SAVE_HISTORY=true`, the service saves the patient foreign key, normalized symptoms JSON, risk level, validated response JSON including disclaimer, and creation time. It never saves failed or malformed provider responses. The existing `ddl-auto=update` setting creates the new `ai_consultations` table on startup; no existing table columns change. A new foreign key uses `ON DELETE CASCADE` so deleting a patient removes their consultations and keeps existing deletion behavior working.

The network call is outside a service database transaction; repository operations use short transactions. History save failure returns a safe 503 instead of reporting a successful save. There is no history-reading REST endpoint in this scope. Disabling history prevents new rows but does not remove old ones. The entity/table is still registered when history is off.

**Test:** enable history, make a successful request, then inspect `ai_consultations` in your local database. H2 integration tests verify persisted fields, timestamps, and cascading deletion. H2 exercises JPA behavior; it does not replace deployment validation against MySQL. For environments using `ddl-auto=validate`, create the new table/foreign key through your normal database migration process first.

## Stage 7 — JWT and authorization

**Changed:** `SecurityConfig`, ownership check in the service, invalid-token handling in `JwtRequestFilter`.

**Why/how:** `/api/ai/**` requires `PATIENT` or `DOCTOR`. Method security also protects the controller. The service checks a patient's authenticated username against the requested patient ID before loading target data or calling AI. A patient querying someone else's ID receives 403 even if the target does not exist, avoiding an ID-existence disclosure. An authorized doctor querying a missing patient receives 404. Malformed, expired, or incorrectly signed JWTs now return safe 401 responses from the existing filter.

**Test:** no token → 401; invalid token → 401; admin → 403; another patient's ID → 403; own ID or doctor → eligible for analysis. Unit and integration tests use the existing JWT implementation, not a replacement authentication system.

## Stage 8 — Error handling

**Changed:** `exception/AIServiceException.java`, handlers added to `GlobalExceptionHandler`.

**Why/how:** Exceptions flow through the existing global handler. New provider exceptions carry fixed, safe public messages. Invalid bodies do not reflect request contents. Existing unrelated handlers remain intact.

| HTTP status | Meaning |
| --- | --- |
| 400 | Invalid/missing request body or field values |
| 401 | Missing, malformed, expired, or invalid JWT |
| 403 | Role or patient ownership denied |
| 404 | Patient absent for an authorized doctor |
| 502 | Invalid, refused, or incomplete AI result |
| 503 | Missing key, provider unavailable, or history save failed |
| 504 | AI transport timeout |

**Test:** `AIClinicalControllerTest` checks REST mappings. `SpringAIClinicalClientTest` checks safe provider and timeout translation. `GeminiClientConfigurationTest` checks Google's exact HTTP endpoint, authorization header, model, JSON schema, response parsing, and sanitized HTTP errors with no retries. Tests do not call a live AI provider.

## Stage 9 — Automated tests

**Changed:** service/client/controller/integration tests, test-only H2 configuration, and a mocked scheduler in the existing context smoke test.

**Why/how:** Mockito isolates provider behavior, MockMvc verifies HTTP validation and the actual security filter chain, and full application tests exercise JWT → controller → real service → JPA with a mocked AI boundary. Test resources replace developer MySQL/SMTP/AI configuration; scheduled tasks are mocked to prevent accidental emails.

**Run all checks:**

```powershell
.\mvnw.cmd test
```

No live AI calls or MySQL server are required. Java source compatibility remains 17; the local verification runtime is JDK 21.

## Stage 10 — Postman testing

**Changed:** `docs/AI-Symptom-Analyzer.postman_collection.json`.

**Why/how:** Import the collection. Set local collection variables: `baseUrl` (use your configured application port), `username`, `password`, `patientId`, `otherPatientId`, and a known-unused `missingPatientId`. Use demo data. The login request saves the token by removing the existing `JWT Token: ` prefix. Credentials and token are blank in the committed collection.

1. Start MySQL and the application with your existing database settings. Start with `AI_SAVE_HISTORY=false`.
2. Run **Login**, then **Analyze own patient** after configuring `AI_API_KEY`.
3. Run the invalid-body, missing-token, invalid-token, and cross-patient checks with a patient login.
4. Log in as a doctor before running **Doctor: missing patient**. The ID must be absent in your database.
5. Run **AI unavailable (key unset)** after restarting without the key. Restore the key to test successful analysis again.
6. Enable history and repeat a successful request; inspect the new table to confirm a saved consultation.

Requests include status assertions. Do not run every request as an unattended sequence: some require different identities or server configuration. Timeout and malformed-provider-response scenarios are covered by automated mocks rather than relying on unpredictable live failures.

The collection is supplied for manual live testing. No live Gemini/Postman run or developer MySQL mutation is performed during implementation.

## Interview discussion

- Why DTOs? They validate API boundaries and avoid leaking JPA relationships or credentials.
- Why a client interface? It separates business rules from the remote provider and enables deterministic tests.
- Why check authorization in the service? Ownership is a business rule; a role check alone cannot prevent cross-patient access.
- Why validate structured output again? A provider can refuse, truncate, or return data that violates application constraints.
- Why avoid a long transaction? Network latency should not hold a database transaction open.
- Why optional history? It demonstrates JPA persistence while allowing a stateless demo response when storage is unnecessary.

## Sources

- [Spring AI 1.0 Boot compatibility](https://docs.spring.io/spring-ai/reference/1.0/getting-started.html)
- [Spring AI OpenAI chat integration](https://docs.spring.io/spring-ai/reference/1.0/api/chat/openai-chat.html)
- [Springdoc Boot compatibility](https://springdoc.org/v2/)
- [Gemini OpenAI-compatible endpoint and structured output](https://ai.google.dev/gemini-api/docs/openai)
- [Gemini free-tier pricing and data use](https://ai.google.dev/gemini-api/docs/pricing)
