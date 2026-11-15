# Q3 - Equality and hashing

## Predictions before execution

1. R1, R1, R2: corrected size 2; the equals-only class normally retains all three separate instances because identity hashes differ (not a universal guaranteed set size).
2. R1, R2, R3: corrected size 3.
3. Aa, BB, Aa: size 2 despite Aa and BB sharing a String hash; unequal colliding keys must coexist.

Also check re-adding the same object, equality across different names, null/type safety, and modifying a hashed field after insertion. A mutable key stored as A and changed to B is searched using its NEW hash while it remains placed using its OLD hash. `contains` and `remove` fail in this deliberately selected fixture; results for arbitrary mutations/bucket coincidences are not guaranteed.

## Explanation and alternatives

HashSet uses a HashMap: a spread hash selects a bucket and stored hashes/equality distinguish entries. Equal objects MUST have equal hashes; the reverse is not required. Without hashCode, overriding equals alone does not reliably de-duplicate separately allocated scans. Make the identity field immutable, base both methods on regNo, and do not include mutable display names. Student is final so subclass equality cannot introduce asymmetry. In real input normalize/validate registration numbers according to an explicit identity policy before construction.

HashSet and HashMap keyed by regNo need O(n) space and expected O(1) per scan with suitable hashing (not a strict worst-case guarantee). HashMap's putIfAbsent explicitly retains the first scan's details. TreeSet with `Comparator.comparing(Student::regNo)` needs O(log n) per scan and O(n) space; comparator equality defines uniqueness and here agrees with equals. String hashing/comparison also depends on registration-number length.

The supplied PDF cuts off the original Complete-the-Code program after the constructor on page 7. Attendance.java preserves the visible private final regNo/name fields and constructor pattern, completes the contract, and reconstructs a three-scan demonstration of the stated `Unique students: 2` output. The broken class is intentionally kept separately for before/after execution; this is not claimed as a verbatim recovery of the missing source.
