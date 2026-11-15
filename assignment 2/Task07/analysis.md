# Q7 - Diagnose, redesign and validate

## A. Diagnose

Select Q4's LinkedHashMap code-order report under a changed requirement: 20 code-order reports between structural inventory changes. The original report creates a key copy and sorts all n keys on EVERY request, although their order has not changed. Q4 supplies the original one-report timings; Q7 repeats the exact same code-order algorithm under the changed workload and instruments actual comparison calls. Sorting and formatting are distinct costs, and the report compares both operation counts and elapsed time. This design targets repeated sorting; it does not assume sorting dominates at every size.

## B. Objective

Preserve exact stock values, code order and insertion order, including updates/deletions/reinsertions. Reduce sorts from 20 to at most 1 and comparator calls by at least 90% between structural changes. Seek a lower mean elapsed time per report; report any failure rather than assuming a speedup. Include cold cache construction in the cold benchmark.

## C. Revised design and D. Prediction

Keep the LinkedHashMap. Add a lazy sorted-key snapshot, invalidated ONLY when a code is inserted/deleted. A stock-only update retains key order; reports read CURRENT stock from the map instead of caching stale report text. Removing/re-adding a code places it at the end of insertion order. The map and its cache must be synchronized consistently in a concurrent application.

Pseudocode: put(code,stock): if code is new, invalidate keys; map.put. remove(code): if present, invalidate keys; map.remove. report(): if keys invalid, copy and sort map keys; emit each key with current map value.

Original r reports cost O(r n log n + r L) including output of total length L per report; the redesign costs O(n log n + r(n+L)) between invalidations. Hot reports are O(n+L), NOT O(1), since every medicine must still be emitted. Permanent cache space adds O(n) references; both approaches also allocate O(L) report strings. A structural change causes the next report to pay the sorting cost again. Predict lower comparison/allocation work and usually lower elapsed time; formatting and GC may limit or obscure the speedup.

## E. Comparable validation

Both methods use the same seeded medicines, same LinkedHashMap insertion order, same format, same 20 reports, 3 warmups and 7 recorded repetitions at n=100, 10,000, 100,000. Both consume full output hashes. Setup is excluded; a cold cache is newly empty for EACH timed repetition. The warm-cache variant is labelled separately and prepopulated outside timing. Independently verify full output strings and actual comparator counts, and test stock changes, insertion, deletion and reinsertion.

## F. Defence

REPORT.md computes actual cold/warm speedup ratios and whether the time objective was met. A deterministic 20-to-1 comparison reduction establishes the targeted eliminated work even if noisy timings fail to improve. The original may be preferable for one nightly report, frequent structural changes, very small inventories or constrained memory. The cache adds invalidation complexity and must not be exposed for external mutation. This is a change in work performed, not a collection substitution.
