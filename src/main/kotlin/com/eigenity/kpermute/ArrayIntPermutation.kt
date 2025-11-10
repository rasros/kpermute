package com.eigenity.kpermute

import kotlin.random.Random

/**
 * Finite in-memory integer permutation backed by an `IntArray` lookup table.
 *
 * Builds a random bijection over `[0, size)` by shuffling the identity mapping
 * and precomputing the inverse. Encode and decode are O(1) array lookups.
 *
 * @param [size] Size of the permutation domain; valid inputs are `[0, size)`.
 * @param [rng] Random generator used to create the shuffled mapping.
 */
class ArrayIntPermutation(
    override val size: Int,
    rng: Random
) : IntPermutation {
    private val array = IntArray(size) { it }
    private val inverse: IntArray

    init {
        array.shuffle(rng)
        inverse = IntArray(size)
        for (i in 0..<size) {
            inverse[array[i]] = i
        }
    }

    override fun encodeUnchecked(value: Int): Int = array[value]

    override fun decodeUnchecked(encoded: Int): Int = inverse[encoded]

    override fun iterator(offset: Int): IntIterator =
        if (offset == 0) array.iterator()
        else array.sliceArray(offset..<size).iterator()

    override fun toString(): String = "ArrayIntPermutation(size=$size)"
}
