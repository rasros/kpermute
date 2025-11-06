package com.eigenity.kpermute

import kotlin.random.Random

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

    override fun encode(value: Int): Int {
        require(value in 0..<size)
        return array[value]
    }

    override fun decode(encoded: Int): Int {
        require(encoded in 0..<size)
        return inverse[encoded]
    }

    override fun iterator(offset: Int): IntIterator =
        if (offset == 0) array.iterator()
        else array.sliceArray(offset..<size).iterator()
}
