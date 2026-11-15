# Q4 - Pharmacy inventory maps

## Prediction and experimental design

Use 100, 10,000 and 100,000 synthetic medicine codes, inserted in a seeded shuffled order. Each collection handles exactly the same 100,000 seeded successful queries and creates both complete formatted reports. Also verify absent keys. Lookup setup, query generation and initial construction are outside timing. Each report sample includes formatting and correctness comparison, so this is end-to-end in-memory report generation, not just sorting. No console/disk printing is timed.

Predict expected O(1) HashMap/LinkedHashMap lookup versus O(log n) TreeMap/binary-search lookup. Set an illustrative average in-memory budget of 10 microseconds per lookup; the report checks that budget using mean batch time / 100,000. This does not establish a p99 or user-facing network/database response guarantee.

## Design choice

LinkedHashMap preserves insertion order and expected constant-time stock lookups. Sort a copy of its keys for the evening code-order report: O(n log n) comparisons and O(n) temporary references. TreeMap keeps code order at O(log n) insertion/lookup and O(n) traversal; it still needs an additional insertion-history list for the second report. HashMap needs both explicit sorting and insertion-history metadata. In the implementation all maps use common report functions (with map.get); that adds O(n log n) lookup work to a TreeMap report, although an optimized entrySet traversal would produce it in O(n). The experiment should not confuse that implementation choice with a TreeMap lower bound.

For lookup-heavy traffic and infrequent evening reports, deploy LinkedHashMap if measured averages meet the budget. Sorting/formatting, memory allocation and potentially persistence/I/O remain costs; O(1) lookup does not mean zero latency. Hashtable synchronizes each method, has legacy API constraints and does not make multi-step transactions atomic. Prefer ordinary maps under suitable external locking or ConcurrentHashMap for an explicitly concurrent requirement; LinkedHashMap itself is not thread-safe.

## Sorted-array alternative

Store medicines sorted by code, use Arrays.binarySearch(keys, code), and access a parallel stock array at the returned index. Lookup uses O(log n) comparisons, insertion/deletion uses O(n) shifts and occasional resizing, code-order output is O(n), and space is O(n). The measured alternative demonstrates lookup, and checks missing keys; preserving insertion order requires a separate timestamp/history index. HashMap/LinkedHashMap expected insertion and lookup are O(1); TreeMap insert/lookup are O(log n). All store O(n) data, with different node/reference overhead. Code string length affects hash and comparison work.
