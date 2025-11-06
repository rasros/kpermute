package com.eigenity.kpermute

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayIntPermutationTest {

    @Test
    fun bijectionSmallDomains() {
        for (n in 1..16) {
            val p = ArrayIntPermutation(n, Random(123))
            CommonAsserts.assertBijectionOverDomain(p, n)
        }
    }

    @Test
    fun iteratorCoversAllInOrderOfEncode() {
        val n = 10
        val p = ArrayIntPermutation(n, Random(1))
        CommonAsserts.assertIteratorMatchesEncode(p, n)
    }

    @Test
    fun iteratorWithOffset() {
        val n = 12
        val off = 5
        val p = ArrayIntPermutation(n, Random(2))
        CommonAsserts.assertIteratorMatchesEncode(p, n, off)
    }

    @Test
    fun deterministicForSameSeed() {
        val factory = { ArrayIntPermutation(16, Random(42)) }
        CommonAsserts.assertDeterministic(factory)
    }

    @Test
    fun decodeIsInverseOfEncodeSample() {
        val p = ArrayIntPermutation(32, Random(99))
        for (x in 0 until 32) assertEquals(x, p.decode(p.encode(x)))
    }

    @Test
    fun rejectsOutOfRange() {
        val n = 7
        val p = ArrayIntPermutation(n, Random(3))
        CommonAsserts.assertRangeChecks(p, n)
    }
}
