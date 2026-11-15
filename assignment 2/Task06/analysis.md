# Q6 - Emergency triage queue

## Prediction and design

A FIFO Queue serves arrival order even if a later patient is more severe. Use PriorityQueue with severity ascending and a monotonic arrival sequence ascending. Hand-derived order for severities [3,1,2,1,5,2,4,3] is P2, P4, P3, P6, P1, P8, P7, P5. Equal-severity arrival ties are checked explicitly. Printing a PriorityQueue exposes heap-array iteration order: only its minimum is guaranteed at the head, not a globally sorted sequence. Poll a copy for a report that must not consume the real queue.

Predict O(log n) worst-case add/poll and O(1) peek, with O(n) space. Each benchmark admits n preconstructed seeded patients or polls an already prepared n-patient queue for n=100, 10,000, 100,000. A batch mean averages changing queue occupancy; it is not a measurement of one operation at exactly n elements. Random admissions may be cheaper than worst case, and finite timings alone do not prove Big-O. Growth fixtures separately verify every adjacent treatment pair.

## Fairness

Continuous critical arrivals can starve severity 5. A candidate teaching policy promotes effective severity by one level for each aging interval, floor 1, then uses original arrival order. Rebuild the queue on a fixed schedule using a frozen now; the provided aged() implementation rebuilds by insertion in O(n log n) worst case, O(n) extra space. Do not put a live clock into a heap comparator and expect the heap to reorder itself. Do not mutate ordering fields while an element remains enqueued. Under a finite number of earlier arrivals, eventual promotion plus the arrival tie-break prevents later arrivals from indefinitely overtaking an old patient. Clinical rules and reassessment require hospital approval; this is an algorithm demonstration.

## Alternatives and deployment

A sorted LinkedList admits in O(n) by walking a ListIterator and serves its head in O(1); repeated get(i) would worsen insertion to O(n^2). Five FIFO ArrayDeques admit in amortized O(1), and checking five queue heads takes O(5)=O(1) to serve when severities are fixed 1..5. Both require O(n) storage. Both alternatives are executed against the hand fixture. Aging requires explicit transfers/reclassification in a bucket design.

5,000 arrivals/day averages one arrival every 17.28 seconds; that daily average does not bound bursts or waiting-queue size. The report estimates CPU time for 5,000 adds and polls from measured batch averages and compares it with a 1-second algorithm-only budget. A low estimate supports computational feasibility under tested conditions, not a claim of clinical/system readiness: locking, persistence, UI, outages and fairness maintenance are outside these timings. PriorityQueue is not thread-safe.

The PDF cuts off the original Triage.main on page 14. The visible Patient fields and toString, Triage counter/admit/next structure and priority-queue TODO are retained; the missing driver is reconstructed and input validation/tests are added explicitly.
