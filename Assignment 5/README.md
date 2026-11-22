# Bus-Pass Assignment – Java code for every task

JDK 11+ (tested on 21). Build: `./build.sh`   Run: `./run.sh <Class> [args]`

## Needs NO database
| Task | File | Run |
|---|---|---|
| A1 formats, ops, damaged data, RandomAccessFile alt. | partA/A1_BusPassFormat.java | `./run.sh A1_BusPassFormat` |
| A2 line counter (unbuffered / buffered / readAllLines) | partA/A2_LineCounter.java | `./run.sh A2_LineCounter 1` (MB) |
| A4 serialization + CSV comparison | partA/A4_Serialization.java | `./run.sh A4_Serialization` |
| A4.2 versioning (same vs different serialVersionUID) | partA/A4/v1, v2 | `./run_a4_versioning.sh` |
| B1 completed `PassStore` (TODO 1) | partB/PassStore.java | `./run.sh PassStore` → `[P1003, 192311003, Divya, 7, 3800.0]` / `null` |
| B1.1 test table | partB/B1_PassStoreTest.java | `./run.sh B1_PassStoreTest` |
| B1.3 buffering benchmark (1/10/50 MB) | partB/B1_LineCountBenchmark.java | `./run.sh B1_LineCountBenchmark` |
| B1.4 growth + HashMap alternative | partB/B1_GrowthBenchmark.java | `./run.sh B1_GrowthBenchmark` |
| B2 damaged file adversarial test | partB/B2_DamagedFileDemo.java | `./run.sh B2_DamagedFileDemo` |
| B4 alt: 12 serialized campus files | partB/B4_CampusSerializedDemo.java | `./run.sh B4_CampusSerializedDemo` |

## Needs MySQL (local TEST database only)
1. `mysql -u root -p < setup.sql`
2. Download MySQL Connector/J (https://dev.mysql.com/downloads/connector/j/) and put the jar in `lib/`.
3. Set `DB_URL`, `DB_USER`, `DB_PASS` env vars if yours differ from the defaults in `DbConfig.java`.

| Task | File | Run |
|---|---|---|
| A3 generic DAO + StudentDao | partA/A3_DataAccess.java | `./run.sh A3_DataAccess` |
| A5 transaction + injection | partA/A5_Transaction_Injection.java | `./run.sh A5_Transaction_Injection` |
| B2 unsafe vs PreparedStatement | partB/B2_AdversarialLogin.java | `./run.sh B2_AdversarialLogin` |
| B3 completed `loadAll` (TODO 1-3) | partB/B3_PassLoader.java | used by the programs below |
| B3.1 correctness (10 rows + ResultSetMetaData) | partB/B3_Correctness.java | `./run.sh B3_Correctness` |
| B3.2/B3.3 timing + forced failure | partB/B3_InsertBenchmark.java | `./run.sh B3_InsertBenchmark 10000` first, then no arg (100k) |
| B4.1 migration + failure report | partB/B4_Migration.java | `./run.sh B4_Migration [file.csv]` |
| B4.2 DatabaseMetaData | partB/B4_DbInspector.java | `./run.sh B4_DbInspector` |
| B4.3 decision | partB/B4_DECISION.md | fill in your measured numbers |

Notes
* Pseudocode answers (A1.2, A5.1, damaged-data design, complexity analyses) are in the header comments of each file.
* "Execution screenshots alone are insufficient": copy the printed tables into your report and write your interpretation. Numbers will differ per machine – use YOUR measurements.
* The `-- ` in the injection input needs a trailing space for MySQL.
