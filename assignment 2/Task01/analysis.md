# Q1 - Building text under load

## Prediction and experiment plan (written before execution)

Compare one 6-field ticket, 10,000 tickets of 50 fields, and 100 unusually long tickets of 500 fields. Fields have equal length. Repeated `+` should copy growing prefixes; builders should scale approximately with final text length. At tiny sizes timer noise can exceed differences. StringBuffer may cost more because it synchronizes, but a JVM can remove uncontended synchronization, so its relative timing is not guaranteed.

## Design and complexity

For n fields of fixed length k, repeated concatenation copies k(1+2+...+n), giving O(kn^2) total copying/allocation; amortized StringBuilder growth gives O(kn). Peak live space can be O(kn) for both when earlier prefixes are collectible; cumulative temporary allocation is the quadratic difference, not necessarily quadratic live heap. The printed String estimate counts n-1 intermediate results plus one final result per ticket, not literal JVM allocations or backing arrays; modern concatenation uses implementation-dependent optimizations. Heap snapshots are noisy occupancy readings, not allocation measurements.

Use a local StringBuilder per ticket. For a shared StringBuffer log, synchronize around the WHOLE booking record, not merely individual append calls, to prevent interleaving. An alternative is build a full record locally and append it under a shared lock; a logging queue/single consumer can reduce contention. StringBuffer alone does not make arbitrary compound operations atomic.

`String.join("\n", fields) + "\n"` and `StringJoiner` are implemented and checked against the builders. For total output length L they use O(L) work/storage under normal library implementations. `String.format("PNR: %s%nPassenger: %s%n", pnr, name)` is convenient for fixed templates but incurs format parsing and conversions; a single pass is proportional to the formatted output plus formatting costs. Repeated `format` plus concatenation inside a loop reintroduces prefix copying.

## Interpretation

Use the generated measurements in REPORT.md. Timing includes the same final String hash traversal for all modes and excludes field creation. Do not generalize a single tiny workload or declare a universal StringBuffer slowdown.
