# Performance Tests

Stress performance tests for the Shift Happens API using [Grafana k6](https://k6.io/).

## Prerequisites

Install k6 once:
```bash
brew install k6          # macOS
```

Or run via Docker (no install required):
```bash
docker pull grafana/k6
```

The application must be running on `http://localhost:8080`. Start it with:
```bash
make run-all
```

## Running the Tests

```bash
# All four tests sequentially
make perf

# Individual test
make perf-one TEST=01-load-test
make perf-one TEST=02-stress-test
make perf-one TEST=03-spike-test
make perf-one TEST=04-soak-test

# Live browser dashboard during a test (opens http://127.0.0.1:5665)
k6 run --out web-dashboard performance/scenarios/02-stress-test.js
```

**Via Docker:**
```bash
docker run --rm -i --network host \
  -v $(pwd)/performance:/performance \
  grafana/k6 run /performance/scenarios/01-load-test.js
```

## Reports

HTML and JSON reports are written to `performance/reports/` after each test run:
- `01-load-test.html` / `.json`
- `02-stress-test.html` / `.json`
- `03-spike-test.html` / `.json`
- `04-soak-test.html` / `.json`

Open the HTML file in a browser for the full metrics summary. For a live dashboard (useful for screenshots during the stress test), use `--out web-dashboard` as shown above.

> Note: The `htmlReport` import fetches from GitHub on first run. k6 caches it locally after that. Requires internet access on first run.

---

## Test Scenarios

All scenarios use the seeded test accounts (password: `password123`):
- Employee: `employee1@shift.dk`
- Manager: `employee30@shift.dk`
- Admin: `admin@shift.dk`

### 01 — Load Test: "Monday Morning Handover"

50 hospital staff check their schedules at the start of the morning shift handover — the expected normal peak.

| Parameter | Value |
|-----------|-------|
| Peak VUs | 20 |
| Duration | ~8 min |
| Traffic | 40% schedule view, 25% shift list, 20% assignments, 15% leave |
| Thresholds | p(95) < 500ms, error rate < 1% |

### 02 — Stress Test: "The Hospital Expands: Too Many Staff"

The hospital onboards a new floor and 200 nurses check their reassignments simultaneously. VUs escalate until the system breaks. The real bottleneck here is `maximum-pool-size=2` in `application.properties` — visible degradation starts at ~50 VUs.

| Parameter | Value |
|-----------|-------|
| Peak VUs | 150 |
| Duration | ~12 min |
| Traffic | 3 sequential DB reads per iteration |
| Thresholds | p(50) < 2000ms, error rate < 30% (intentionally loose — test designed to break the system) |

### 03 — Spike Test: "Schedule Published: Everyone Checks at Once"

The manager publishes the weekend schedule at 14:00 Friday. 80 employees open the app within 5 seconds. The recovery phase at the end shows if the system returns to baseline.

| Parameter | Value |
|-----------|-------|
| Spike VUs | 80 (reached in 5 seconds) |
| Duration | ~5 min |
| Traffic | 60% schedule overview, 40% shift detail lookups |
| Thresholds | p(95) < 3000ms, error rate < 10% |

### 04 — Soak Test: "The Night Shift: Running Unattended"

System runs at steady load for 30 minutes to detect memory leaks and connection degradation. Compare response times at minute 5 vs minute 25 — upward drift without increasing VUs indicates a resource leak.

| Parameter | Value |
|-----------|-------|
| Sustained VUs | 15 |
| Duration | 30 min |
| Traffic | Realistic employee session loop with 1–5s reading pauses |
| Thresholds | p(95) < 800ms over full 30 min, error rate < 2% |

**What to look for in the soak test:** If the app logs show `HikariPool-1 - Connection leak detection triggered` (from `leak-detection-threshold=2000ms`), compare those timestamps with this test's timeline.

---

## Key Architectural Finding

The application is configured with `spring.datasource.hikari.maximum-pool-size=2` (`src/main/resources/application.properties`). This is the primary bottleneck under concurrent load:

- At 20 VUs: connection queue begins building
- At 50 VUs: p(95) response time climbs significantly
- At 100–150 VUs: connection timeout (30s) causes 500 errors

This is the expected production configuration and the stress/load tests document its real-world behavior.
