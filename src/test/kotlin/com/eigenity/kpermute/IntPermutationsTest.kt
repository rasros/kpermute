package com.eigenity.kpermute

import kotlin.random.Random
import kotlin.test.*

class IntPermutationsTest {

    @Test
    fun selectsImplementationBySize() {
        assertTrue(intPermutation(8) is ArrayIntPermutation)
        assertTrue(intPermutation(17) is HalfIntPermutation)
        assertTrue(intPermutation(UInt.MAX_VALUE) is FullIntPermutation)
        assertTrue(intPermutation(-10) is UIntPermutation)
    }

    @Test
    fun factoryRepeatableSeed() {
        val p1 = intPermutation(32, seed = 1234L)
        val p2 = intPermutation(32, seed = 1234L)
        assertEquals(p1.toList(), p2.toList())
    }

    @Test
    fun smallSizesUseArrayPermutationAndAreBijections() {
        for (n in 0..16) {
            val p = intPermutation(n)
            assertTrue(p is ArrayIntPermutation)
            assertEquals(n, p.toList().toSet().size)
        }
    }

    @Test
    fun halfIntPermutationLargeStillBijection() {
        val p = HalfIntPermutation(512, Random(88))
        CommonAsserts.assertBijectionOverDomain(p, 512)
    }

    @Test
    fun respectsRoundsParameterAcrossFactory() {
        val p1 = intPermutation(64, seed = 123, rounds = 1)
        val p2 = intPermutation(64, seed = 123, rounds = 5)
        CommonAsserts.assertDifferentMapping(p1, p2, sample = 10)
    }

    @Test
    fun fullSelectedForMaxUIntExplicit() {
        val p = intPermutation(UInt.MAX_VALUE)
        assertTrue(p is FullIntPermutation)
    }
}
