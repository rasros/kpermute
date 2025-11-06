package com.eigenity.kpermute

import kotlin.random.Random

/**
 * Represents a reversible integer permutation over a finite or full 32-bit domain.
 *
 * Implementations provide deterministic bijections for integer sets, allowing
 * repeatable shuffling, masking, or indexing without storing lookup tables.
 *
 * ## Security note
 * These permutations are **not cryptographic**. They use lightweight avalanche
 * and cycle-walking techniques for uniform dispersion, but provide no
 * pseudorandom permutation (PRP) or resistance to inversion by an adversary.
 *
 * ## Domain semantics
 * - For finite domains, `size` defines the valid range `[0, size)`.
 * - A `size` of `-1` represents the full signed 32-bit integer space.
 * - `encodeUnchecked`/`decodeUnchecked` skip bounds checks for performance;
 *   callers must ensure arguments are within the domain when `size >= 0`.
 *
 * Implementations are iterable and yield `encode(i)` for all valid `i`.
 *
 * Use the factory method [intPermutation] for instantiation.
 */
interface IntPermutation : Iterable<Int> {

    /**
     * Domain of the permutation.
     */
    val size: Int


    /** Fast path. No range checks. Precondition: if size >= 0 then arg ∈ [0, size). */
    fun encodeUnchecked(value: Int): Int

    /** Fast path. No range checks. Precondition: if size >= 0 then arg ∈ [0, size). */
    fun decodeUnchecked(encoded: Int): Int

    /**
     * Encode an integer in [0, [size]) into its permuted value.
     */
    fun encode(value: Int): Int {
        if (size >= 0) require(value in 0 until size) {
            "value $value out of range [0, $size)"
        }
        return encodeUnchecked(value)
    }

    /**
     *  Decode a previously encoded integer back to its original value.
     */
    fun decode(encoded: Int): Int {
        if (size >= 0) require(encoded in 0 until size) {
            "encoded $encoded out of range [0, $size)"
        }
        return decodeUnchecked(encoded)
    }

    /**
     * Iterator that yields encoded values for [0, size).
     */
    override fun iterator(): IntIterator = iterator(0)

    /**
     * Iterator that yields encoded values for [offset, size).
     */
    fun iterator(offset: Int): IntIterator
}

/**
 * Returns a new list whose elements are permuted by [perm].
 * The original list is not modified.
 *
 * Example:
 * ```
 * val perm = intPermutation(5, seed = 42)
 * val shuffled = listOf("a","b","c","d","e").permutedBy(perm)
 * ```
 */
fun <T> List<T>.permutedBy(perm: IntPermutation): List<T> {
    val n = size
    require(perm.size >= 0 && perm.size == n) {
        "Permutation domain (${perm.size}) must equal list size ($n)"
    }
    return List(n) { index -> this[perm.decode(index)] }
}

/**
 * Applies the inverse of [perm] to reorder this list back to original order.
 */
fun <T> List<T>.unpermutedBy(perm: IntPermutation): List<T> {
    val n = size
    require(perm.size >= 0 && perm.size == n) {
        "Permutation domain (${perm.size}) must equal list size ($n)"
    }
    return List(n) { index -> this[perm.encode(index)] }
}

/**
 * Returns a new list whose elements are permuted by a permutation initialized
 * by [rng] and [rounds].
 * The original list is not modified.
 *
 * Example:
 * ```
 * val perm = intPermutation(5, seed = 42)
 * val shuffled = listOf("a","b","c","d","e").permutedBy(perm)
 * ```
 */
fun <T> List<T>.permuted(rng: Random = Random.Default, rounds: Int = 0) =
    permutedBy(intPermutation(size, rng, rounds))

/**
 * Returns a view of this permutation that operates on [range] instead of `[0, size)`.
 * Only valid for finite domains where `range.count() == size`.
 *
 * Useful for permuting values within numeric subranges such as dataset shards,
 * sliding windows, or bounded ID segments without manual offset math.
 */
fun IntPermutation.range(range: IntRange): IntPermutation {
    val n = range.count()
    require(size >= 0) { "range() requires a finite base permutation" }
    require(size == n) { "base size ($size) must equal range length ($n)" }

    val start = range.first
    return object : IntPermutation {
        override val size: Int = n

        // Unchecked operate on *range values*.
        override fun encodeUnchecked(value: Int): Int =
            start + this@range.encodeUnchecked(value - start)

        override fun decodeUnchecked(encoded: Int): Int =
            start + this@range.decodeUnchecked(encoded - start)

        // Checked wrappers validate range membership.
        override fun encode(value: Int): Int {
            require(value in range) { "value $value out of $range" }
            return encodeUnchecked(value)
        }

        override fun decode(encoded: Int): Int {
            require(encoded in range) { "encoded $encoded out of $range" }
            return decodeUnchecked(encoded)
        }

        // Iterator yields permuted values for inputs in [range.first+offset, range.last].
        // offset is measured in *indices* (0..n), consistent with base contract.
        override fun iterator(offset: Int): IntIterator {
            require(offset in 0..n) { "offset $offset out of [0, $n]" }
            var i = offset
            return object : IntIterator() {
                override fun hasNext() = i < n
                override fun nextInt(): Int {
                    if (!hasNext()) throw NoSuchElementException()
                    return start + this@range.encodeUnchecked(i++)
                }
            }
        }
    }
}
