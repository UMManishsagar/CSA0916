# Q5 - Generic ranking component

## Design before execution

Rank by points descending, goal difference descending, then name ascending. A chain of total field orders is lexicographic: the first unequal field decides, preserving antisymmetry and transitivity. All equal field values compare as zero. Two distinct Team objects can be ranking-equivalent; this is a list ordering, not a deduplicating set. If used in a TreeSet, define equals consistently with ranking identity or add a unique team identifier as a final key.

Choose injected Comparator objects because teams may have several valid rankings and the same Leaderboard<T> can rank players or colleges. Comparator<? super T> accepts an ordering for T or its supertype; List<? extends T> supplies T or subtype items. T needs no artificial Comparable bound when the comparator is injected. The natural helper demonstrates the meaningful bounded alternative `<U extends Comparable<? super U>>`. Predict O(n log n) comparison work and O(n) working/copy space for ranking.

## Correctness and alternative designs

The ties fixture tests every key and checks original insertion order/copy isolation. The adversarial fixture contains Integer.MIN_VALUE/MAX_VALUE to break subtraction comparators and equivalent duplicate field values to check comparison zero. Exhaustive small-fixture pair/triple checks cover symmetry, transitivity and zero-equivalence substitution. Integer.compare/comparingInt avoid arithmetic overflow. Reversing the entire completed chain would incorrectly reverse name order too; reverse each descending key separately.

If Team implemented Comparable<Team>, compareTo could delegate to the same ORDER chain, and Collections.sort(copy) would use that natural order. This couples one default ranking to Team; injected comparators remain useful for alternatives. Three stable sorts, applied from least to most significant (name, goal difference, points), also work. One chain uses one O(n log n) sort; three stable sorts use three such sorts, still O(n log n) asymptotically but with more passes. Java object-list sorting can require O(n) temporary storage; a returned sorted copy also uses O(n). Peak extra space remains O(n), not O(n^2). The timings exclude copying in both designs to isolate the sort passes.

The supplied PDF cuts off Leaderboard.ranking() and the following driver on page 11. The visible Team and Leaderboard fields/API are reconstructed, the sorted-copy TODO is completed, and a new explicit driver supplies missing tests. It is not a claim to preserve an unseen original driver.
