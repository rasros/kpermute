package com.eigenity.kpermute

import kotlin.test.*

object CommonAsserts {

    /** Verify encode/decode is a bijection on [0, size). */
    fun assertBijectionOverDomain(p: IntPermutation, size: Int) {
        val image = IntArray(size) { p.encode(it) }
        // Range check
        for (v in image) assertTrue(v in 0 until size)
        // Injective/surjective
        assertEquals(size, image.toSet().size)
        // Decode correctness
        for (i in 0 until size) {
            assertEquals(i, p.decode(image[i]))
        }
    }

    /** Verify iterator yields encode(i) for i in [offset, size). */
    fun assertIteratorMatchesEncode(
        p: IntPermutation,
        size: Int,
        offset: Int = 0
    ) {
        val it = p.iterator(offset)
        val actual = it.asSequence().toList()
        val expected = (offset until size).map { p.encode(it) }
        assertEquals(expected, actual)
    }

    /** Verify deterministic behavior for same seed. */
    fun assertDeterministic(factory: () -> IntPermutation) {
        val p1 = factory()
        val p2 = factory()
        // Spot check first 128 values or full domain if smaller
        val n = when (val size = p1.size) {
            -1 -> 128
            else -> minOf(size, 128)
        }
        for (i in 0 until n) assertEquals(p1.encode(i), p2.encode(i))
    }

    /** Verify different seeds or rounds change mapping. */
    fun assertDifferentMapping(
        p1: IntPermutation,
        p2: IntPermutation,
        sample: Int = 42
    ) {
        assertNotEquals(p1.encode(sample), p2.encode(sample))
    }

    /** Verify encode/decode reject out-of-range when a finite size exists. */
    fun assertRangeChecks(p: IntPermutation, size: Int) {
        assertFailsWith<IllegalArgumentException> { p.encode(-1) }
        assertFailsWith<IllegalArgumentException> { p.encode(size) }
        assertFailsWith<IllegalArgumentException> { p.decode(-1) }
        assertFailsWith<IllegalArgumentException> { p.decode(size) }
    }
}
