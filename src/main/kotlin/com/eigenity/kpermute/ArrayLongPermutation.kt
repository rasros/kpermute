package com.eigenity.kpermute

import kotlin.random.Random

/**
 * Finite in-memory long permutation backed by a `LongArray` lookup table.
 *
 * Builds a random bijection over `[0, size)` by shuffling the identity mapping
 * and precomputing the inverse. Encode and decode are O(1) array lookups.
 *
 * @param [size] Size of the permutation domain; valid inputs are `[0, size)`.
 * @param [rng] Random generator used to create the shuffled mapping.
 */
class ArrayLongPermutation(
    override val size: Long,
    rng: Random
) : LongPermutation {
    private val array = LongArray(size.toInt()) { it.toLong() }
    private val inverse: LongArray

    init {
        array.shuffle(rng)
        inverse = LongArray(size.toInt())
        for (i in 0..<size.toInt()) {
            inverse[array[i].toInt()] = i.toLong()
        }
    }

    override fun encodeUnchecked(value: Long): Long = array[value.toInt()]

    override fun decodeUnchecked(encoded: Long): Long = inverse[encoded.toInt()]

    override fun iterator(offset: Long): LongIterator =
        if (offset == 0L) array.iterator()
        else array.sliceArray(offset.toInt()..<size.toInt()).iterator()

    override fun toString(): String = "ArrayLongPermutation(size=$size)"
}
