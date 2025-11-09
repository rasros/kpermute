package com.eigenity.kpermute

import kotlin.random.Random

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
