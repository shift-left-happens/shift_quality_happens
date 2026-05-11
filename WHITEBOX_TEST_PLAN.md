# Whitebox Test Plan — Shift Happens

Companion to the blackbox analysis in *Software Quality exam — Shift Happens*. The blackbox section defines **what** must hold (equivalence partitions, BVA, decision tables, state transitions). This plan defines **how** we verify those rules against the actual code — by driving each branch, condition and data-flow path through JUnit 5 + Mockito tests, measured by JaCoCo and gated by SonarQube.

---

## 1. Goals & Coverage Targets

| Metric | Target | Enforcement |
|--------|--------|-------------|
| Line coverage (services) | **≥ 90 %** | JaCoCo `check` goal, build fails below threshold |
| Branch coverage (services) | **≥ 85 %** | JaCoCo `check` goal |
| Line coverage (project overall) | **≥ 75 %** | JaCoCo `check` goal |
| Mutation score (services, optional) | ≥ 70 % | PIT plugin (Phase 4) |
| Sonar Quality Gate | **Pass** on new code | `sonar-project.properties` |

Coverage is a *symptom* — the real target is that **every row of every blackbox decision table and BVA table has a corresponding test case** with an asserted outcome that mirrors the doc.

---

## 2. Whitebox Techniques We Will Apply

| Technique | What it covers | Where we use it |
|-----------|----------------|-----------------|
| **Statement coverage** | Every executable line runs at least once | Floor — JaCoCo line metric |
| **Branch / decision coverage** | Each `if/else`, `?:` and `switch` arm taken | All services with conditionals (LeaveApproval, LeaveRequest, Employee, future Shift logic) |
| **Condition coverage** | Each boolean sub-expression evaluated true and false | Compound guards (e.g. `decision == null || decision.isBlank()`) |
| **MC/DC** *(applied selectively)* | Each condition independently affects the outcome | `LeaveApprovalService.approve` (5-condition guard chain) and the future shift-overlap rule |
| **Path coverage** *(bounded)* | Linearly independent paths through a method | Methods with cyclomatic complexity ≥ 4 (use `mvn jacoco:report` + visual inspection) |
| **Loop coverage** | 0, 1, n iterations | Repositories and any future scheduling loop (overlap detection) |
| **Data-flow** (def-use) | Variables defined and used along realistic paths | Patch-style updates (`EmployeeService.patch`, `LeaveRequestService.patch`) — verifies mutable state transitions |
| **Exception path testing** | Each `throw` reached and the wrapping `try/catch` exercised | `@Transactional` rollbacks once approval flows are wired |

---

## 3. Tools — Already Wired

| Tool | Status | Use |
|------|--------|-----|
| JUnit 5 + AssertJ + Mockito | Active (`spring-boot-starter-test`) | Unit tests |
| JaCoCo 0.8.13 | Configured in `pom.xml` (no thresholds yet) | Coverage report at `target/site/jacoco/index.html` |
| Spotless + palantir-java-format | Active | Style consistency |
| SonarQube | `sonar-project.properties` present | Quality gate, smells, duplication |
| Spring Boot Test slices | Available | `@DataJpaTest` for repository tests, `@WebMvcTest` for controllers |
| **PIT mutation testing** | **To add** | Mutation score on services |
| **Testcontainers (MySQL)** | **To add** | Integration tests against real MySQL (matches blackbox FR contracts) |

Action items (Phase 0):
1. Add JaCoCo `check` execution with the thresholds above.
2. Add PIT (`org.pitest:pitest-maven`) for service mutation testing.
3. Add Testcontainers (`org.testcontainers:mysql`) for end-to-end repository + DB trigger tests.

---

## 4. Code Map → Test Map

The blackbox doc covers nine entities. Of those, only four currently have service-layer logic that can be unit-tested:

| Entity | Service exists? | Whitebox tactic |
|--------|-----------------|-----------------|
| Employee | ✅ `EmployeeService` (save, patch, delete, password hash) | Unit + slice tests; verify password is encoded, never persisted raw |
| LeaveRequest | ✅ `LeaveRequestService` (validate, create, patch, delete) | Extend existing test; cover every branch of `validateRequest` |
| LeaveApproval | ✅ `LeaveApprovalService` (rich approval flow) | Highest-priority MC/DC target — 5+ guards in `approve` |
| Department | ✅ `DepartmentService` | Coverage today is low; add patch/delete branches |
| EmployeeJobRole | ✅ `EmployeeJobRoleService` | Inspect, then cover |
| JwtService | ✅ | Token issue + parse + tamper paths |
| CustomUserDetailsService | ✅ | Found / not-found branches |
| **Shift** | ❌ controller + repo only | **Add service** carrying BR-SH-01…08 + BR1–BR7, *then* test |
| **ShiftAssignment** | ❌ | **Add service** for overlap + 11h-rest + role-match rules, then test |
| **ShiftSwap / ShiftSwapApproval** | ❌ | **Add service** for state-transition + approval rules, then test |
| **JobRole / ShiftRequiredJobRole** | ❌ | Add validation, then test |
| **WorkLocation** | ❌ | Add validation, then test |

> The doc's blackbox tables for Shift, Swap, Assignment, JobRole and WorkLocation describe **behaviour that is not yet implemented** in Java. For those, this plan is a **TDD plan**: write the failing whitebox test first (red), implement the service guard (green), refactor.

---

## 5. Mapping Blackbox → Whitebox

For each blackbox artefact we already produced, here is the whitebox follow-through. The pattern is:

> **Each EP class, each BVA boundary, each row of a decision table, each transition in a state diagram → at least one parameterised JUnit test that drives exactly that path through the production code, asserts the documented outcome, *and* contributes to JaCoCo branch coverage.**

### 5.1 Employee — name / email / password (doc §Employees blackbox analyse)

| Blackbox artefact | Whitebox test class | Notes |
|-------------------|---------------------|-------|
| First/last name EP + decision table | `EmployeeValidatorTest` (new) | `@ParameterizedTest` with `@CsvSource` for the 5 cases. Add `EmployeeValidator` helper if validation does not yet live in a service. |
| Password length BVA + composition decision table | `PepperedPasswordEncoderTest` *and* a new `PasswordPolicyTest` | Encoder test asserts hash format and BCrypt cost. Policy test runs all 13 BVA values (0,1,4,6,7,8,9,125,254,255,256,257,300) and the 5 decision-table rows. |
| Email EP + BVA + decision table + edge cases | `EmailValidatorTest` (new) | One parameterised test per table. Edge cases (autocorrect / Unicode / whitespace inside) become their own `@Nested` class. |

### 5.2 Shift (doc §Shift Black-Box analyse)

Service does not exist yet. Whitebox plan:

1. Introduce `ShiftService.create(Shift)` that validates department FK, work-location FK, start/end ordering, duration, name length and status enum.
2. Test class `ShiftServiceTest` covers:
   - EP tables (start/end, dept ID, work-location ID, name, duration, status).
   - BVA table for shift time boundaries (7 values).
   - Decision table — **7 rows × 1 parameterised test method** with `@MethodSource`.
3. Branch coverage target on `ShiftService` ≥ 95 % (small, mostly guards).

### 5.3 ShiftAssignment (doc §ShiftAssignment Black-Box Analysis)

Highest-complexity area — overlap detection + 11 h rest + role match. Plan:

1. `ShiftAssignmentService.assign(...)` consolidates the guards. Stub-able dependencies: `ShiftRepository`, `EmployeeRepository`, `ShiftAssignmentRepository`.
2. `ShiftAssignmentServiceTest`:
   - EP for shift ID, employee ID, status.
   - BVA for check-in / check-out (6 cases) and overlap + rest period (11 cases).
   - Decision table (7 rows) — one `@ParameterizedTest`.
3. **MC/DC** applied to the 6-condition guard. The 7-row decision table is already MC/DC-shaped (each row flips one condition while holding the others at *Yes*).

### 5.4 ShiftSwap + ShiftSwapApproval

State transitions are the key whitebox angle:

- `ShiftSwapStateMachineTest` (table-driven): for each of the 8 transitions in the doc's state diagram, assert that the service either accepts or rejects, **and** that no side effects (assignment updates) leak on rejected transitions.
- Decision tables for *create swap* (6 rows) and *approve swap* (7 rows) → two parameterised tests.
- BVA for reason length (0, 1, 50, 499, 500, 501) and request time (4 cases).
- Use Mockito `verify(..., never())` to assert that assignments are **not** swapped when status is Declined / Cancelled / Approved-again.

### 5.5 LeaveRequest + LeaveApproval

These services already exist; this is pure coverage extension.

`LeaveApprovalServiceTest` extension to cover every guard in `approve`:

| Guard line | Whitebox case |
|------------|---------------|
| `leaveRequestId == null` | null request id |
| `approverEmployeeId == null` | null approver |
| `decision == null || decision.isBlank()` | both branches — null AND blank |
| `!APPROVED && !REJECTED` | "PENDING", "Accepted", "" lowercase |
| Request not found | repo returns empty |
| `!PENDING.equalsIgnoreCase(...)` | request already APPROVED, REJECTED |
| approver not found | repo returns empty |
| `role == null` | employee without role |
| `role != Administrator && role != Manager` | employee with Employee role |
| Happy path | both APPROVED and REJECTED, with lowercase + whitespace normalisation |

That single method needs **~12 tests** to reach 100 % branch coverage. Today it is at zero — `LeaveApprovalServiceTest` does not exist.

### 5.6 Department / WorkLocation / JobRole / ShiftRequiredJobRole

Each follows the same skeleton: validate + decision table + BVA → one `@ParameterizedTest` per table. Most logic does not yet exist in services and will be added during Phase 2.

---

## 6. Quality Gates

### JaCoCo `check` (add to pom.xml)

```xml
<execution>
    <id>check</id>
    <phase>verify</phase>
    <goals><goal>check</goal></goals>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit><counter>LINE</counter>   <minimum>0.75</minimum></limit>
                    <limit><counter>BRANCH</counter> <minimum>0.65</minimum></limit>
                </limits>
            </rule>
            <rule>
                <element>PACKAGE</element>
                <includes>
                    <include>dk.ek.shift_happens.*</include>
                </includes>
                <excludes>
                    <exclude>dk.ek.shift_happens.*.dto.*</exclude>
                </excludes>
                <limits>
                    <limit><counter>LINE</counter>   <minimum>0.90</minimum></limit>
                    <limit><counter>BRANCH</counter> <minimum>0.85</minimum></limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</execution>
```

### Sonar Quality Gate

Already configured. Recommended gate on **new code**:

- Coverage ≥ 80 %
- Duplicated lines ≤ 3 %
- Maintainability rating A
- Reliability rating A
- Security rating A
- Security hotspots reviewed = 100 %

### Build pipeline (Makefile target)

Add `make quality`:

```
quality:
	./mvnw -q clean verify
	./mvnw -q org.pitest:pitest-maven:mutationCoverage
	./mvnw -q sonar:sonar
```

---

## 7. Phased Delivery

### Phase 0 — Foundations (½ day)
- [ ] Add JaCoCo `check` thresholds to `pom.xml`.
- [ ] Add PIT mutation plugin (services-only target package).
- [ ] Add Testcontainers MySQL dependency.
- [ ] Document `make test`, `make coverage`, `make mutation`.

### Phase 1 — Cover what exists (1–2 days)
- [ ] **`LeaveApprovalServiceTest`** — ~12 tests, every branch of `approve` + `update`.
- [ ] Extend `LeaveRequestServiceTest` — patch, delete, all `validateRequest` branches, `existsById` false branch.
- [ ] **`EmployeeServiceTest`** — save hashes password, patch updates non-null fields only, delete throws on missing.
- [ ] **`DepartmentServiceTest`** — `findById` 404, update partial, delete missing.
- [ ] **`JwtServiceTest`** — issue → parse round-trip, expired token, tampered signature.
- [ ] **`PepperedPasswordEncoderTest`** — encode, matches, mismatches, pepper-effect.
- [ ] Run JaCoCo, screenshot for exam report.

### Phase 2 — Drive Shift / Swap / Assignment logic into the codebase (2–3 days)
- [ ] Add `ShiftService` with create-time validation; write tests from §5.2.
- [ ] Add `ShiftAssignmentService` overlap + 11 h rest + role match; write tests from §5.3.
- [ ] Add `ShiftSwapService` + `ShiftSwapApprovalService`; write tests from §5.4 including state-machine transitions.
- [ ] Each table row from the blackbox doc maps to a `@ParameterizedTest` case.

### Phase 3 — Validators for the small entities (1 day)
- [ ] `JobRoleValidatorTest`, `ShiftRequiredJobRoleValidatorTest`, `WorkLocationValidatorTest`, `DepartmentValidatorTest`.
- [ ] Email / password / name validators (extracted from EmployeeService).

### Phase 4 — Integration & mutation (1–2 days)
- [ ] `@DataJpaTest` slice tests that hit the real schema (Testcontainers MySQL 8.0).
- [ ] One test per DB constraint mentioned in the README: `leave_approval` trigger, `app_user` privileges, `audit_log` SELECT-only.
- [ ] PIT mutation pass — fix surviving mutants on services.
- [ ] Sonar baseline scan, fix any *Blocker* or *Critical* findings.

### Phase 5 — Report
- [ ] Generate `target/site/jacoco/index.html` and PIT report; embed screenshots in the exam doc's empty **Whitebox / Integration test / Quality gates** sections.

---

## 8. Test Naming Convention

```
methodName_stateUnderTest_expectedBehavior
```

Examples:
- `approve_decisionIsLowercaseApproved_normalisesAndSaves`
- `create_endDateBeforeStartDate_throwsIllegalArgument`
- `assign_employeeHasOverlappingShift_rejects`

This makes JaCoCo reports and Sonar findings self-documenting and lines up directly with the blackbox table rows.

---

## 9. Sample — Whitebox Tests for `LeaveApprovalService.approve`

Listed in execution order; covers every branch + the two normalisation paths:

| # | Method | Drives branch |
|---|--------|----------------|
| 1 | `approve_nullLeaveRequestId_throws` | `leaveRequestId == null` |
| 2 | `approve_nullApprover_throws` | `approverEmployeeId == null` |
| 3 | `approve_nullDecision_throws` | first half of OR |
| 4 | `approve_blankDecision_throws` | second half of OR |
| 5 | `approve_unknownDecision_throws` | not APPROVED, not REJECTED |
| 6 | `approve_leaveRequestNotFound_throws` | repo.findById empty |
| 7 | `approve_leaveRequestNotPending_throws` | status is APPROVED |
| 8 | `approve_leaveRequestRejected_throws` | status is REJECTED |
| 9 | `approve_approverNotFound_throws` | employee repo empty |
| 10 | `approve_approverWithNullRole_throws` | role == null |
| 11 | `approve_approverIsEmployee_throws` | role neither Admin nor Manager |
| 12 | `approve_managerApproves_savesAndUpdatesRequestStatus` | happy path APPROVED |
| 13 | `approve_managerRejects_savesAndUpdatesRequestStatus` | happy path REJECTED |
| 14 | `approve_lowercaseDecision_normalisedToUppercase` | normalisation branch |

That single class should land branch coverage of `LeaveApprovalService` at ~100 % and serves as the template for every other service test.

---

## 10. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Blackbox rules expect validation that does not exist in the code | Whitebox tests for absent code are vacuous | Phase 2 explicitly adds the missing services *before* claiming coverage |
| Mocking the JPA repository hides real persistence bugs (FK, triggers) | False sense of correctness | Phase 4 Testcontainers slice tests against real MySQL + the existing init scripts |
| 90 % coverage chased by trivial getter tests | Inflated metric, no real signal | Exclude generated Lombok via `lombok.addLombokGeneratedAnnotation`; use PIT mutation score as the *quality* check |
| Decision tables drift from production code | Doc and code disagree silently | Use `@ParameterizedTest` with `@MethodSource` that returns rows of the exact decision table — drift becomes a compile/test failure |

---

## 11. Definition of Done (per service)

A service is "whitebox-tested" when:

1. JaCoCo line coverage ≥ 90 %, branch coverage ≥ 85 % for that class.
2. Every row of every blackbox decision table for that entity has a corresponding `@ParameterizedTest` case with the documented expected result.
3. Every BVA value listed in the doc is a test argument.
4. Every state transition (where applicable) has a positive and negative test.
5. PIT mutation score ≥ 70 % on that class.
6. Sonar reports no `Critical` or `Blocker` issue on the class.

---

## 12. Getting Started — Day 1 Checklist

1. `LeaveApprovalServiceTest` (template — see §9). Run, screenshot JaCoCo report on that one class.
2. Add JaCoCo thresholds; run `./mvnw verify` — confirm it fails the gate (it should — coverage is low).
3. Extend `LeaveRequestServiceTest` to cover `patch` and `delete`.
4. Open `target/site/jacoco/index.html` and pick the next red class.
