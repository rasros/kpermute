package com.eigenity.kpermute

interface IntPermutation : Iterable<Int> {

    /**
     * Domain of the permutation.
     */
    val size: Int

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
