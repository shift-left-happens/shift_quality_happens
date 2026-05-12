# Black Box Test Analysis - Shift Happens

This document contains black box test designs for the Shift-Happens system, based on the requirements defined in `SRS_Shift_Happens.docx.md`. 

The analysis includes:
- **Equivalence Partitioning (EP)**
- **Boundary Value Analysis (BVA)** (including 2-point and 3-point analysis)
- **Decision Tables**

---

## 1. Employee Age & Birth Date
Based on:
- **BR-EM-01**: Employee must be at least 16 years old.
- **BR-EM-02**: Employee cannot be older than 100 years old.
- **BR-EM-03**: Birth dates cannot be in the future.
- **Section 5.1**: Year between 1926 and 2010 (relative to a current year of 2026).

### Equivalence Partitioning & BVA (3-point)
Target Year: 2026

| Field | Partition Type | Partition Range | Test Value | Boundary Values (3-point) | Test Case Values |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Age** | Invalid (Too young) | 0 - 15 | 10 | 15, 16 | 14, 15, 16 |
| | Valid | 16 - 100 | 30 | 16, 100 | 15, 16, 17, 99, 100, 101 |
| | Invalid (Too old) | 101+ | 110 | 100, 101 | 100, 101, 102 |
| **Birth Year** | Invalid (Future/Too young) | 2011 - 2026 | 2015 | 2010, 2011 | 2009, 2010, 2011 |
| | Valid | 1926 - 2010 | 1980 | 1926, 2010 | 1925, 1926, 1927, 2009, 2010, 2011 |
| | Invalid (Too old) | < 1926 | 1900 | 1925, 1926 | 1924, 1925, 1926 |

**Explanation:**
- **3-point BVA** tests the boundary itself, one step below, and one step above. This ensures the "off-by-one" errors are caught at both edges of the transition.
- For `Age 16`, we test `15` (Invalid), `16` (Valid), and `17` (Valid).
- For `Age 100`, we test `99` (Valid), `100` (Valid), and `101` (Invalid).

---

## 2. Shift Duration & Timing
Based on:
- **FR-SH-07 / BR-SH-01**: End time must be after start time.
- **BR-WT-02**: At least 11 hours of rest between shifts.

### Decision Table: Shift Creation (Timing & Rules)

| Condition | Rule 1 | Rule 2 | Rule 3 | Rule 4 | Rule 5 |
| :--- | :---: | :---: | :---: | :---: | :---: |
| Start Time < End Time? | T | F | T | T | T |
| Overlaps with existing shift? | F | - | T | F | F |
| Rest period >= 11 hours? | T | - | - | F | T |
| Matches Job Role? | T | - | - | - | F |
| **Result** | **Valid** | **Error** | **Error** | **Error** | **Error** |

**Explanation:**
- **Rule 2**: Rejects if the user tries to end a shift before it starts.
- **Rule 3**: Prevents double-booking an employee (BR-SH-02).
- **Rule 4**: Enforces the 11-hour rest rule (BR-WT-02).
- **Rule 5**: Ensures the employee is qualified for the role (FR-JR-05).

---

## 3. Weekly Working Hours
Based on:
- **BR-WT-01**: Max 37 hours per week on average.
- **FR-CON-05**: Contract defines weekly hours.

### Equivalence Partitioning & BVA (2-point)
Assuming a standard contract of 37 hours.

| Value | Partition Type | Range | Test Value | Boundary | Test Case Values |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Weekly Hours** | Valid | 0.0 - 37.0 | 20.5 | 0.0, 37.0 | 0.0, 0.1, 36.9, 37.0 |
| | Invalid (Overtime) | > 37.0 | 40.0 | 37.1 | 37.1, 38.0 |

---

## 4. Consecutive Work Days
Based on:
- **BR-WT-04**: Max 6 consecutive days.
- **BR-WT-05**: Max 3 consecutive night shifts.

### Equivalence Partitioning & BVA (3-point)

| Field | Partition Type | Range | Test Value | Boundary | Test Case Values |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Consecutive Days** | Valid | 1 - 6 | 3 | 1, 6 | 0, 1, 2, 5, 6, 7 |
| | Invalid | 7+ | 8 | 7 | 6, 7, 8 |
| **Night Shifts** | Valid | 1 - 3 | 2 | 1, 3 | 0, 1, 2, 3, 4 |
| | Invalid | 4+ | 5 | 4 | 3, 4, 5 |

---

## 5. Shift Swap Approvals
Based on:
- **BR-AP-01 to BR-AP-05**: Approval rules.

### Decision Table: Swap Approval

| Condition | Case 1 | Case 2 | Case 3 | Case 4 | Case 5 | Case 6 |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| User is Admin/Manager? | T | F | T | T | T | T |
| Approver is not the Requester? | T | - | F | T | T | T |
| Request Status is 'Pending'? | T | - | - | F (Cancl) | F (Decl) | T |
| Shift is not cancelled? | T | - | - | - | - | F |
| Causes Overlap? | F | - | - | - | - | - |
| **Result** | **Approve** | **Deny** | **Deny** | **Deny** | **Deny** | **Deny** |

**Explanation:**
- **Case 1**: The happy path.
- **Case 2**: Regular employees cannot approve swaps.
- **Case 3**: (BR-AP-02) Prevents self-approval.
- **Case 4/5**: (BR-AP-03, BR-AP-04) Cannot approve if already cancelled or declined.
- **Case 6**: (BR-AP-05) Cannot swap a shift that no longer exists.

---

## 6. Email Uniqueness (FR-EMP-06)
Since uniqueness is a boolean state (exists/doesn't exist), we use a simple decision table.

| Condition | Case 1 | Case 2 |
| :--- | :---: | :---: |
| Email exists in DB? | Yes | No |
| Format is valid? | Yes | Yes |
| **Result** | **Reject** | **Accept** |

---

## 7. Password Strength (Refined)
Based on: **Section 5.1** and **BR-04**.
Length 8-64 characters. Must have Lower, Upper, Number.

### 3-Point BVA for Password Length

| Range Type | Range | Boundary | Test Values |
| :--- | :--- | :--- | :--- |
| Lower Boundary | 8 | 7, 8, 9 | "Abc1234" (7), "Abc12345" (8), "Abc123456" (9) |
| Upper Boundary | 64 | 63, 64, 65 | String of 63, 64, and 65 valid chars |
| **Email Length** | 320 | 319, 320, 321 | a@long-domain...com (319), a@long-domain...com (320), a@long-domain...com (321) |

---

## 8. Deletion Constraints & Dependencies
Based on:
- **FR-EMP-05**: Prevent deletion of employees with future assigned shifts.
- **FR-DEP-04**: Prevent deleting departments linked to active employees or shifts.
- **FR-LOC-04**: Prevent deleting locations linked to active shifts.
- **FR-CON-04**: Prevent deleting contracts linked to active employees.
- **FR-JR-04**: Prevent deleting job roles linked to active employees or shifts.

### Decision Table: Resource Deletion

| Condition | Case 1 | Case 2 | Case 3 | Case 4 |
| :--- | :---: | :---: | :---: | :---: |
| Has active shifts/assignments? | Yes | No | No | No |
| Linked to active employees? | - | Yes | No | No |
| Resource type is "Contract"? | - | - | Yes | No |
| **Result** | **Block** | **Block** | **Block** | **Delete** |

**Explanation:**
- Tests the referential integrity rules. If any dependency exists (active shift or employee), the system must block deletion to prevent orphaned records.

---

## 9. Overnight Shifts & ISO 8601
Based on:
- **FR-SH-08**: Overnight shift support.
- **BR-EM-04**: ISO 8601 format (YYYY-MM-DD).
- **BR-EM-05**: Real calendar date.

### Equivalence Partitioning: Date & Time Formats

| Field | Partition | Test Value | Result |
| :--- | :--- | :--- | :--- |
| **Date Format** | Valid ISO 8601 | 2024-05-12 | Valid |
| | Invalid Format | 12-05-2024 | Invalid |
| | Invalid Date | 2024-02-30 | Invalid (Non-existent date) |
| **Overnight Shift** | Same day (Start < End) | 08:00 - 16:00 | Valid |
| | Next day (Start > End) | 22:00 - 06:00 | Valid (Overnight) |
| | Across months | 2024-01-31 22:00 to 2024-02-01 06:00 | Valid |

---

## 10. Contract Validity
Based on:
- **FR-CON-06**: Start date and optional end date.

### BVA (3-point) for Contract Dates
Assuming today is 2026-05-12.

| Boundary | Point | Test Date | Result |
| :--- | :--- | :--- | :--- |
| **Start Date** | -1 Day | 2026-05-11 | Valid (Active) |
| | Current | 2026-05-12 | Valid (Starts Today) |
| | +1 Day | 2026-05-13 | Valid (Future Start) |
| **End Date** | -1 Day (Before Start) | 2026-05-11 | Invalid |
| | Same as Start | 2026-05-12 | Valid (1-day contract) |
| | +1 Day (After Start) | 2026-05-13 | Valid |

---

## 11. Shift Status Transitions
Based on:
- **FR-SH-05**: Statuses: Open, Assigned, Pending Swap, Cancelled, Completed.

### State Transition Table

| Current State | Event | Next State | Valid? |
| :--- | :--- | :--- | :---: |
| Open | Assign Employee | Assigned | Yes |
| Assigned | Request Swap | Pending Swap | Yes |
| Pending Swap | Approve Swap | Assigned (New Emp) | Yes |
| Pending Swap | Decline Swap | Assigned (Old Emp) | Yes |
| Assigned | Cancel Shift | Cancelled | Yes |
| Assigned | Time Passes End | Completed | Yes |
| Cancelled | Assign Employee | - | No |
| Completed | Request Swap | - | No |

---

## 12. Input Validation: Names (FR-EMP-01 / Section 5.1)
Refining the name rules.

### Decision Table: Name Formatting

| Condition | Case 1 (Simple) | Case 2 (Initial) | Case 3 (Numbers) | Case 4 (Spaces) |
| :--- | :---: | :---: | :---: | :---: |
| Letters Only? | Yes | Yes | No | No |
| Initial Pattern (X.)? | No | Yes | No | No |
| No Leading Space? | Yes | Yes | Yes | Yes |
| **Result** | **Valid** | **Valid** | **Invalid** | **Invalid (Last Name)** |

**Test Values:**
- Case 1: "Jensen"
- Case 2: "Peter A."
- Case 3: "Jens3n"
- Case 4: "Jensen Jens" (Invalid for Last Name per Section 5.1)
