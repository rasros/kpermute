# KPermute

[![Maven Central](https://img.shields.io/maven-central/v/com.eigenity/kpermute.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.eigenity/kpermute/1.0.0)
[![Build](https://github.com/Eigenity/kpermute/actions/workflows/build.yml/badge.svg)](https://github.com/Eigenity/kpermute/actions/workflows/build.yml)
[![codecov](https://codecov.io/gh/Eigenity/kpermute/branch/main/graph/badge.svg)](https://codecov.io/gh/Eigenity/kpermute)
[![License](https://img.shields.io/github/license/Eigenity/kpermute)](https://github.com/Eigenity/kpermute/blob/main/LICENSE)

> **Fast, deterministic integer permutation library for Kotlin.**  
> Shuffle or obfuscate large integer domains efficiently using bijective,
> reversible hash mixing.
>
> ⚠️ **Not intended for cryptographic use.**  
> Suitable for data masking, sampling, and reproducible pseudo-randomization
> where reversibility is required. You decide if your use-case is cryptographic.

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

---

## Example Usage

```kotlin
fun main() {
    // Example 1: Obfuscate numeric IDs reproducibly
    val idPerm = longPermutation(seed = 1L)
    val longId = 49102490812045L
    val intIdEncoded = idPerm.encode(longId)
    println("encoded: $intIdEncoded (always prints 3631103739497407856)")

    // Example 2: Shuffle a large list lazily
    val largeList = object : AbstractList<Int>() {
        override val size: Int get() = Int.MAX_VALUE
        override fun get(index: Int) = index
    }
    val perm = intPermutation(largeList.size)
    val shuffled = largeList.permuted(perm)
    println("shuffled: ${shuffled.take(20)}")
    val unshuffled = shuffled.unpermuted(perm)
    println("unshuffled: ${unshuffled.take(20)}")

    // Example 3: Custom range permutation with negatives
    val rangePerm = intPermutation(-100..199)
    println("encode(-50): ${rangePerm.encode(-50)}")
    println("decode(...): ${rangePerm.decode(rangePerm.encode(-50))}")

    // Example 4: Full 2^32-bit domain permutation
    val fullPerm = intPermutation(-1, seed = 1L)
    println(fullPerm.encode(0)) // 1339315335
    println(fullPerm.encode(1)) // -897806455
}
```

---

## How It Works

KPermute builds **keyed, reversible permutations** over integer domains using
xor-shift-multiply mixers and cycle-walking.
It never stores lookup tables and always supports decoding back to the original
value.

### Domains and Implementations

Each permutation has a `size`:

* `size > 0` → finite domain `[0, size)`
* `size == -1` / `-1L` → full 32- or 64-bit domain
* `size < 0` (not `-1`) → unsigned variants via `UIntPermutation` /
  `ULongPermutation`

Factory functions select implementations:

| Domain Type       | Implementation               | Description                     |
|-------------------|------------------------------|---------------------------------|
| Tiny (`≤16`)      | `Array[Int/Long]Permutation` | Uses shuffled array and inverse |
| Finite            | `Half[Int/Long]Permutation`  | Uses cycle-walking              |
| Full bit-width    | `Full[Int/Long]Permutation`  | No cycle-walking                |
| Unsigned variants | `U[Int/Long]Permutation`     | Modulo `2^32` or `2^64`         |                                 |

Range factories like `intPermutation(range)` and `longPermutation(range)` wrap
these with a `range(...)` view, so you can permute directly on intervals such as
`-100..199`.

---

### Mixing and Cycle-Walking

Each permutation round:

1. Multiplies by an odd constant.
2. Adds or xors a secret per-round key.
3. Applies xor-shift steps (`x ^= x >>> s`) to diffuse bits.

All steps are invertible using modular inverses and xor-shift
inversion [1] [3] [4] [5].
For non-power-of-two domains, KPermute uses **cycle-walking** [1] [2]: permute
in
the next power-of-two space and retry until the output falls in `[0, size)`.

---

## References

[1]: https://web.cs.ucdavis.edu/~rogaway/papers/subset.pdf

[2]: https://csrc.nist.gov/csrc/media/projects/block-cipher-techniques/documents/bcm/proposed-modes/ffx/ffx-spec.pdf

[3]: https://www-cs-faculty.stanford.edu/~knuth/taocp.html

[4]: http://burtleburtle.net/bob/hash/integer.html

[5]: https://arxiv.org/pdf/1402.6246.pdf

[6]: https://github.com/Cyan4973/xxHash

1. P. Rogaway and T. Shrimpton,
   “Ciphers with Arbitrary Finite Domains,” *CT-RSA 2002*. [PDF][1]
2. M. Bellare, P. Rogaway, and T. Spies,
   “The FFX Mode of Operation for Format-Preserving Encryption,” *NIST
   submission, 2010.* [Spec][2]
3. D. E. Knuth,
   *The Art of Computer Programming, Vol. 2: Seminumerical Algorithms,* 3rd ed.,
    1997. [Info][3]
4. B. Jenkins,
   “Integer Hash Functions,” 1997. [Web][4]
5. S. Vigna,
   “An Experimental Exploration of Marsaglia’s Xorshift Generators, Scrambled,”
   *TOMS 42(4), 2016.* [Preprint][5]
6. Y. Collet,
   “xxHash – Extremely fast hash algorithm,” 2014. [GitHub][6]

---
