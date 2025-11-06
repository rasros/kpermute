package com.eigenity.kpermute

import kotlin.random.Random
import kotlin.test.*

@OptIn(ExperimentalUnsignedTypes::class)
class UIntPermutationTest {

    @Test
    fun bijectionVariousSizes() {
        for (n in listOf(7, 15, 64, 1000)) {
            val p = UIntPermutation(n, Random(55))
            CommonAsserts.assertBijectionOverDomain(p, n)
        }
    }

    @Test
    fun iteratorFullAndOffsetsEdgeCases() {
        val n = 5
        val p = UIntPermutation(n, Random(1))
        // full
        CommonAsserts.assertIteratorMatchesEncode(p, n)
        // offset == last index → one element then end
        val itLast = p.iterator(n - 1)
        assertTrue(itLast.hasNext())
        assertEquals(p.encode(n - 1), itLast.nextInt())
        assertFalse(itLast.hasNext())
        assertFailsWith<NoSuchElementException> { itLast.nextInt() }
        // offset == size → empty and throws on next
        val itEmpty = p.iterator(n)
        assertFalse(itEmpty.hasNext())
        assertFailsWith<NoSuchElementException> { itEmpty.nextInt() }
    }

    @Test
    fun deterministicForSameSeed() {
        val n = 20
        val factory = { UIntPermutation(n, Random(9)) }
        CommonAsserts.assertDeterministic(factory)
    }

    @Test
    fun roundsAndConstConstraintsAndVariation() {
        assertFailsWith<IllegalArgumentException> {
            UIntPermutation(32, Random(1), const = 4u)
        }
        val p1 = UIntPermutation(128, Random(5), rounds = 1)
        val p2 = UIntPermutation(128, Random(5), rounds = 5)
        CommonAsserts.assertDifferentMapping(p1, p2)
    }

    @Test
    fun rejectsOutOfRange() {
        val n = 10
        val p = UIntPermutation(n, Random(1))
        CommonAsserts.assertRangeChecks(p, n)
    }
}
