package com.eigenity.kpermute

/**
 * Reversible permutation over a 64-bit integer domain.
 *
 * A [LongPermutation] defines a bijection on either:
 * - a finite domain `[0, size)` when `size >= 0L`, or
 * - the full signed 64-bit space when `size == -1L`.
 *
 * Implementations are deterministic: the same instance always maps the same
 * input to the same output, and [decode] is the exact inverse of [encode].
 * They are suitable for repeatable shuffling, masking, and index remapping
 * without full lookup tables.
 *
 * Security note:
 * These permutations are not cryptographic. They are not PRPs and are not
 * intended to resist adversarial inversion or analysis.
 *
 * Contract:
 * - For finite domains (`size >= 0L`), valid inputs to [encode] and [decode]
 *   are in `[0, size)`. Out-of-range values trigger [IllegalArgumentException].
 * - [encodeUnchecked] and [decodeUnchecked] skip range checks and must only
 *   be called with valid domain values when `size >= 0L`.
 * - [iterator] yields `encode(i)` for all valid `i` in index order.
 *
 * Use [longPermutation] to construct concrete implementations.
 */
interface LongPermutation : Iterable<Long> {

    /**
     * Domain size of the permutation.
     *
     * - `size >= 0L`: finite domain `[0, size)`.
     * - `size == -1L`: full signed 64-bit domain.
     */
    val size: Long

    /**
     * Encodes a [value] without range checks.
     */
    fun encodeUnchecked(value: Long): Long

    /**
     * Decodes a previously [encoded] value without range checks.
     */
    fun decodeUnchecked(encoded: Long): Long

    /**
     * Encodes a long in the permutation domain into its permuted value.
     *
     * For finite domains (`size >= 0L`), [value] must be in `[0, size)`.
     */
    fun encode(value: Long): Long {
        if (size >= 0) require(value in 0 until size) {
            "value $value out of range [0, $size)"
        }
        return encodeUnchecked(value)
    }

    /**
     * Decodes a previously encoded long back to its original value.
     *
     * For finite domains (`size >= 0L`), [encoded] must be in `[0, size)`.
     */
    fun decode(encoded: Long): Long {
        if (size >= 0) require(encoded in 0 until size) {
            "encoded $encoded out of range [0, $size)"
        }
        return decodeUnchecked(encoded)
    }

    /**
     * Returns an iterator over `encode(i)` for all `i` in `[0, size)` for
     * finite domains, or over the full 64-bit space when `size == -1L`.
     */
    override fun iterator(): LongIterator = iterator(0)

    /**
     * Returns an iterator over `encode(i)` for indices in `[offset, size)`.
     *
     * For finite domains, [offset] is an index in `0..size`. For full-domain
     * implementations, semantics are defined by the implementation.
     */
    fun iterator(offset: Long): LongIterator
}


/**
 * Returns a view of this permutation that operates on [range] instead of `[0, size)`.
 * Only valid for finite domains where `range.count() == size`.
 *
 * Useful for permuting values within numeric subranges such as dataset shards,
 * sliding windows, or bounded ID segments without manual offset math.
 */
fun LongPermutation.range(range: LongRange): LongPermutation {
    val n = range.last - range.first + 1L
    require(size >= 0L) { "range() requires a finite base permutation" }
    require(size == n) { "base size ($size) must equal range length ($n)" }

    val start = range.first
    return object : LongPermutation {
        override val size: Long = n

        // Unchecked operate on *range values*.
        override fun encodeUnchecked(value: Long): Long =
            start + this@range.encodeUnchecked(value - start)

        override fun decodeUnchecked(encoded: Long): Long =
            start + this@range.decodeUnchecked(encoded - start)

        // Checked wrappers validate range membership.
        override fun encode(value: Long): Long {
            require(value in range) { "value $value out of $range" }
            return encodeUnchecked(value)
        }

        override fun decode(encoded: Long): Long {
            require(encoded in range) { "encoded $encoded out of $range" }
            return decodeUnchecked(encoded)
        }

        // Iterator yields permuted values for inputs in [range.first+offset, range.last].
        // offset is measured in *indices* (0..n), consistent with base contract.
        override fun iterator(offset: Long): LongIterator {
            require(offset in 0..n) { "offset $offset out of [0, $n]" }
            var i = offset
            return object : LongIterator() {
                override fun hasNext() = i < n
                override fun nextLong(): Long {
                    if (!hasNext()) throw NoSuchElementException()
                    return start + this@range.encodeUnchecked(i++)
                }
            }
        }
    }
}
