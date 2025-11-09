# KPermute

[![Maven Central](https://img.shields.io/maven-central/v/com.eigenity/kpermute.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.eigenity/kpermute/1.0.0)

> **Fast, deterministic integer permutation library for Kotlin.**  
> Shuffle or obfuscate large integer domains efficiently using bijective,
> reversible hash mixing.
>
> ⚠️ **Not intended for cryptographic use.**  
> Suitable for data masking, sampling, and reproducible pseudo-randomization
> where reversibility is required.

---

## Overview

`kpermute` generates stable, deterministic **pseudo-random permutations** over
integer ranges.  
Each seed defines a unique bijection between `[0, size)`.  
The result acts like a **keyed shuffle**, repeatable, memory-efficient, and
invertible.

Typical use cases:

- Repeatable pseudo-random shuffles
- Obfuscating integer IDs (e.g., user IDs, session numbers)
- Collision-free sampling or load balancing
- Data masking for non-sensitive identifiers

---

## Installation

Add the dependency from Maven Central:

```kotlin
implementation("com.eigenity:kpermute:1.0.0")
```

## Example Usage

```kotlin
fun main() {
// Example 1: Obfuscate numeric IDs reproducibly
// The range is 0-Int.MAX_VALUE so no negative values as in/out
    val intIdPerm = intPermutation(seed = 1L)
    val intId = 49102490
    val intIdEncoded = intIdPerm.encode(intId)
    println("encoded: $intIdEncoded (always prints 1394484051)")


    // Example 2: Obfuscate UUID-v7 IDs
    // hiding timestamp and UUID-version
    val uuidPerm1 = longPermutation(-1, seed = 1L)
    val uuidPerm2 = longPermutation(-1, seed = 3L)
    val uuid = Uuid.parse("019a67e6-02a0-7646-a5cd-ddcb69d3b71c")
    val encoded = uuid.toLongs { l1, l2 ->
        Uuid.fromLongs(
            uuidPerm1.encode(l1),
            uuidPerm2.encode(l2)
        )
    }
    println("encoded: $encoded")


    // Example 3: Shuffle a large list
    val largeList = object : AbstractList<Int>() {
        override val size: Int get() = Int.MAX_VALUE
        override fun get(index: Int) = index
    }
    val perm = intPermutation(largeList.size)
    val shuffled = largeList.permuted(perm) // does not load anything
    println("shuffled: ${shuffled.take(20)}}")
    val unshuffled = shuffled.unpermuted(perm)
    println("unshuffled: ${unshuffled.take(20)}")


    // Example 4: Custom range permutation and negative values
    val rangePerm = intPermutation(-100..199)
    println("encode(-50): ${rangePerm.encode(-50)}")
    println("decode(...): ${rangePerm.decode(rangePerm.encode(-50))}")

    // Example 5: Full 2^32 bit range permutation
    // Half the values will be negative
    val fullPerm = intPermutation(-1, seed = 1L)
    println(fullPerm.encode(0)) // 1339315335
    println(fullPerm.encode(1)) // -897806455

}
```

### How it works

KPermute builds **keyed, reversible permutations** over integer domains using
simple xor–shift–multiply mixers plus cycle-walking. It never stores lookup
tables and always supports decoding back to the original value.

#### Domains and implementations

A permutation has a `size`:

- `size > 0` → finite domain `[0, size)`
- `size == -1` / `-1L` → full 32- or 64-bit domain
- `size < 0` (not `-1`) → "unsigned" variants via `UIntPermutation` /
  `ULongPermutation` (internally modulo `2^32` / `2^64`)

Factories pick an implementation:

- `Array[Int|Long]Permutation` for tiny domains (`size <= 16`) using a shuffled
  array and its inverse.
- `Half[Int|Long]Permutation` for general finite domains using cycle-walking.
- `Full[Int|Long]Permutation` for the full bit-width (no cycle-walking).
- `UIntPermutation` / `ULongPermutation` for negative `size` (unsigned-style
  behavior).

Range factories `intPermutation(range)` and `longPermutation(range)` wrap these
with a `range(...)` view so you can work directly on e.g. `-100..199`.

#### Mixing and cycle-walking

For non-array variants, each round:

1. Multiplies by an odd constant.
2. Adds or xors a secret per-round key.
3. Applies xor-shift steps (`x ^= x >>> s`) to diffuse bits.

All operations are invertible using modular inverses and xor-shift inversion
[1][3][4][5]. For domains that are not powers of two, KPermute applies
cycle-walking [1][2]: it permutes in the next power-of-two space and re-applies
the permutation until the result falls in `[0, size)`.

---

### References

[1] P. Rogaway and T. Shrimpton,  
"Ciphers with Arbitrary Finite Domains," CT-RSA 2002.  
<https://web.cs.ucdavis.edu/~rogaway/papers/subset.pdf>

[2] M. Bellare, P. Rogaway, T. Spies,  
"The FFX Mode of Operation for Format-Preserving Encryption," NIST
submission, 2010.
<https://csrc.nist.gov/csrc/media/projects/block-cipher-techniques/documents/bcm/proposed-modes/ffx/ffx-spec.pdf>

[3] D. E. Knuth,  
*The Art of Computer Programming, Volume 2: Seminumerical Algorithms,* 3rd ed., 1997.

[4] B. Jenkins,  
"Integer Hash Functions," 1997.
<http://burtleburtle.net/bob/hash/integer.html>

[5] S. Vigna,  
"An Experimental Exploration of Marsaglia's Xorshift Generators, Scrambled,"
TOMS 42(4), 2016.  
Preprint: <https://arxiv.org/pdf/1402.6246.pdf>

[6] Y. Collet,  
"xxHash – Extremely fast hash algorithm," 2014.  
<https://github.com/Cyan4973/xxHash>
