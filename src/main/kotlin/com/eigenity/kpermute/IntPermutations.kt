package com.eigenity.kpermute

/**
 * Reversible permutation over a 32-bit integer domain.
 *
 * An [IntPermutation] defines a bijection on either:
 * - a finite domain `[0, size)` when `size >= 0`, or
 * - the full signed 32-bit space when `size == -1`.
 *
 * Implementations are deterministic: the same instance always maps the same
 * input to the same output, and [decode] is the exact inverse of [encode].
 * They are designed for tasks such as data shuffling, masking, and index
 * remapping without storing full lookup tables.
 *
 * Security note:
 * These permutations are not cryptographic. They are not PRPs and are not
 * intended to resist adversarial inversion or analysis.
 *
 * Contract:
 * - For finite domains (`size >= 0`), valid inputs to [encode] and [decode]
 *   are in `[0, size)`. Out-of-range values trigger [IllegalArgumentException].
 * - [encodeUnchecked] and [decodeUnchecked] skip range checks and must only
 *   be called with valid domain values when `size >= 0`.
 * - [iterator] yields `encode(i)` for all valid `i` in index order.
 *
 * Use [intPermutation] to construct concrete implementations.
 */
interface IntPermutation : Iterable<Int> {

    /**
     * Domain size of the permutation.
     *
     * - `size >= 0`: finite domain `[0, size)`.
     * - `size == -1`: full signed 32-bit domain.
     */
    val size: Int

    /**
     * Encodes a [value] without range checks.
     */
    fun encodeUnchecked(value: Int): Int

    /**
     * Decodes a previously [encoded] value without range checks.
     */
    fun decodeUnchecked(encoded: Int): Int

    /**
     * Encodes an integer in the permutation domain into its permuted value.
     *
     * For finite domains (`size >= 0`), [value] must be in `[0, size)`.
     */
    fun encode(value: Int): Int {
        if (size >= 0) require(value in 0 until size) {
            "value $value out of range [0, $size)"
        }
        return encodeUnchecked(value)
    }

    /**
     * Decodes a previously encoded integer back to its original value.
     *
     * For finite domains (`size >= 0`), [encoded] must be in `[0, size)`.
     */
    fun decode(encoded: Int): Int {
        if (size >= 0) require(encoded in 0 until size) {
            "encoded $encoded out of range [0, $size)"
        }
        return decodeUnchecked(encoded)
    }

    /**
     * Returns an iterator over `encode(i)` for all `i` in `[0, size)` for
     * finite domains, or over the full 32-bit space when `size == -1`.
     */
    override fun iterator(): IntIterator = iterator(0)

    /**
     * Returns an iterator over `encode(i)` for indices in `[offset, size)`.
     *
     * For finite domains, [offset] is an index in `0..size`. For full-domain
     * implementations, semantics are defined by the implementation.
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
 * val shuffled = listOf("a","b","c","d","e").permuted(perm)
 * ```
 */
fun <T> List<T>.permuted(perm: IntPermutation = intPermutation(size)): List<T> {
    val n = size
    require(perm.size == n) {
        "Permutation domain (${perm.size}) must equal list size ($n)"
    }
    return object : AbstractList<T>() {
        override val size: Int get() = n
        override fun get(index: Int): T = this@permuted[perm.decode(index)]
    }
}

/**
 * Applies the inverse of [perm] as a view, restoring the original order.
 */
fun <T> List<T>.unpermuted(perm: IntPermutation): List<T> {
    val n = size
    require(perm.size == n) {
        "Permutation domain (${perm.size}) must equal list size ($n)"
    }
    return object : AbstractList<T>() {
        override val size: Int get() = n
        override fun get(index: Int): T = this@unpermuted[perm.encode(index)]
    }
}

/**
 * Returns a view of this permutation that operates on [range] instead of `[0, size)`.
 * Only valid for finite domains where `range.count() == size`.
 *
 * Useful for permuting values within numeric subranges such as dataset shards,
 * sliding windows, or bounded ID segments without manual offset math.
 */
fun IntPermutation.range(range: IntRange): IntPermutation {
    val n = range.last - range.first + 1
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
