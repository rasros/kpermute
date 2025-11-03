# KPermute

[![Maven Central](https://img.shields.io/maven-central/v/io.github.rasros/kpermute.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.rasros/kpermute/1.0.0)

> ⚙️ Kotlin library for deterministic, bijective integer and long permutations  
> built on a fast cycle-walking hash mixer.  
> **Not cryptographic — but fast, lightweight, and repeatable.**

---

### ✨ Overview

`kpermute` provides stable, deterministic **pseudo-random permutations** over integer domains using a simple **cycle-walking hash** algorithm.  It behaves like a keyed shuffler: every RNG seed defines a new bijection between `[0, size)`.

Use it when you need a *repeatable shuffle*, *non-cryptographic obfuscation*, or *format-preserving ID remapping*.

---

### 🚀 Example

```kotlin
import io.github.rasros.kpermute.*

fun main() {
    val perm = IntPermutation(size = 1000)
    println(perm.encode(42))      // deterministic remapping of 42
    println(perm.toList().take(10)) // first 10 permuted values
}
```

### 🧪 Use cases
 
- Generating repeatable pseudo-random shuffles
- Obfuscating integer IDs (user IDs, session numbers)
- Sampling or load-balancing without collisions
- Data masking for non-sensitive identifiers

### 🔗 Sources
The original source of this implementation is unknown but is presumed to be in the public domain.
This version includes modifications and refinements by me.
If you recognize or can identify the original source, please contact me.
- [Ciphers with Arbitrary Finite Domains (2002)](https://web.cs.ucdavis.edu/~rogaway/papers/subset.pdf):
  Introduces the cycle-walking method for mapping a permutation over a power-of-two space into a smaller domain.  
- [Format-Preserving Encryption (FFX) (2009)](https://csrc.nist.gov/csrc/media/projects/block-cipher-techniques/documents/bcm/proposed-modes/ffx/ffx-spec.pdf):
  Defines standard format-preserving encryption constructions that also rely on cycle-walking.
- [Integer Hash Functions (1997)](http://burtleburtle.net/bob/hash/integer.html):
  Overview of multiply–xor–shift integer mixers that influenced `KPermute`’s round function.
- [An Experimental Exploration of Marsaglia’s Xorshift Generators (2016)](https://arxiv.org/pdf/1402.6246.pdf):
  Analyzes xor/shift mixers and their statistical properties.  
- [xxHash (2014)](https://github.com/Cyan4973/xxHash):
  Fast non-cryptographic hash function using constant multipliers and bit-scrambling, closely related in design to `KPermute`’s mixing logic.  
  
