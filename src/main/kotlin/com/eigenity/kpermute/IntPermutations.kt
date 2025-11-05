package com.eigenity.kpermute

import kotlin.random.Random

interface IntPermutation : Iterable<Int> {

    /**
     * Domain of the permutation.
     */
    val size: Int

    /**
     * Number of mixing rounds
     */
    val rounds: Int

    /**
     * Encode an integer in [0, [size]) into its permuted value.
     */
    fun encode(value: Int): Int

    /**
     *  Decode a previously encoded integer back to its original value.
     */
    fun decode(encoded: Int): Int

    /**
     * Iterator that yields encoded values for [0, size).
     */
    override fun iterator(): IntIterator = iterator(0)

    /**
     * Iterator that yields encoded values for [offset, size).
     */
    fun iterator(offset: Int): IntIterator
}

object SingletonIntPermutation : IntPermutation {
    override val size: Int get() = 1
    override val rounds: Int get() = 0
    override fun encode(value: Int): Int {
        require(value == 0)
        return 0
    }

    override fun decode(encoded: Int): Int {
        require(encoded == 0)
        return 0
    }

    private val array = intArrayOf(0)
    override fun iterator(offset: Int): IntIterator = array.iterator()
}

class ArrayIntPermutation(
    override val size: Int,
    rng: Random
) : IntPermutation {
    private val array = IntArray(size) { it }

    init {
        array.shuffle(rng)
    }

    override val rounds: Int get() = 0
    override fun encode(value: Int): Int = array[value]
    override fun decode(encoded: Int): Int = array[array[encoded]]
    override fun iterator(offset: Int): IntIterator = array.iterator()
}
