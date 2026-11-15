# Q2 - Music playlist editor

## Prediction and experiment plan

Test 100, 10,000 and 100,000 preconstructed Song objects. Time append batches, 200 insert/remove pairs at front and middle, 200 middle-index reads, and one full iterator traversal. A pair keeps size constant; its ns/op is the combined insertion AND removal, not either separately. Test 10,000 Up Next cycles (two additions and two removals) in both deques.

## Analysis and decision rule

ArrayList append is amortized O(1), indexed access O(1), front/middle insert and remove O(n), traversal O(n), space O(n) with spare capacity. LinkedList append and front edits are O(1), indexed middle operations O(n) because locating the node dominates; relinking is O(1) ONLY once a node/iterator is already located. Its traversal is O(n), but nodes add reference/allocation overhead and reduce locality. Indexed `for` traversal would make LinkedList O(n^2), so use its iterator.

Default to ArrayList for append/read/iterate playlists. Define an illustrative interactive edit budget of 1 ms per insert/remove pair; the generated report checks tested sizes against it. This is a measured average boundary for these workloads, not a universal maximum size or tail-latency guarantee. Even below that threshold, sustained front-edit traffic makes ArrayDeque a better API for Up Next. ArrayDeque supports amortized O(1) additions/removals at both ends, O(n) storage, and no random indexed access; it cannot replace a full editor requiring arbitrary k-th access.

For cursor-local repeated middle edits a retained LinkedList ListIterator can justify LinkedList, whereas repeated index-based middle edits do not. Timing ratios and absolute budgets must be interpreted alongside actual usage frequency.
