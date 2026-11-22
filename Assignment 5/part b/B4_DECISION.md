# B4.3 Production Decision (fill the numbers from YOUR runs)

**Decision: use a relational database (MySQL) shared by all 12 campuses.**

| Requirement | Text file | Serialization | Database |
|---|---|---|---|
| Find by ID at ~100x scale (~800,000 passes/yr) | O(n) scan: B1.4 measured ___ ms at 100k rows, grows linearly | must load whole object graph: ___ ms (A4 numbers) | O(log n) with PRIMARY KEY index |
| Several counters at once | appends can interleave; no row locks | whole-file rewrite => lost updates | row locks + transactions (B3.3: all-or-nothing) |
| Searching (route list, validity, reports) | full scan each time | full load each time | `WHERE route_no=?` + index |
| Safety (partial failure) | damaged lines (B2); torn writes | version break (A4.2) | rollback proved in B3.3 (0 rows left) |
| Bulk import | - | - | Batch+txn: ___ ms for 100k vs Statement ___ ms (B3.2) |

Justify with your measured table from B1.3, B1.4, B3.2, B3.3. Keep text files only for export/backup; keep serialization only for local caches.
