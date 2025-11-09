package com.eigenity.kpermute

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.*
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.random.nextULong
import kotlin.test.assertEquals

class FuzzyingTest {

    private val outerIterations = 100
    private val innerIterations = 1000
    private val rng = Random(42)

    // -------- Int permutations --------

    private fun choseIntPerm(): Triple<IntPermutation, Long, Int> {
        var size: Int
        do {
            size = when (rng.nextInt(4)) {
                0 -> rng.nextInt(17)        // ArrayIntPermutation
                1 -> rng.nextInt()          // HalfIntPermutation
                2 -> rng.nextUInt().toInt() // UIntPermutation
                else -> -1                  // FullIntPermutation
            }
        } while (size == 0)
        val rounds = rng.nextInt(2, 8)
        val seed = rng.nextLong()
        return Triple(intPermutation(size, Random(seed), rounds), seed, rounds)
    }

    // Normal CDF approximation (Abramowitz–Stegun 7.1.26)
    private fun normalCdf(x: Double): Double {
        val t = 1.0 / (1.0 + 0.2316419 * abs(x))
        val d = 0.3989423 * exp(-x * x / 2.0)
        val prob = d * t * (
                0.3193815 +
                        t * (-0.3565638 +
                        t * (1.781478 +
                        t * (-1.821256 +
                        t * 1.330274)))
                )
        return if (x > 0.0) 1.0 - prob else prob
    }

    // Two-sided p-value for t-test on mean under H0: mean = expectedMean
    private fun meanTPValue(
        mean: Double,
        expectedMean: Double,
        variance: Double,
        n: Int
    ): Double {
        if (variance <= 0.0) return 1.0
        val s = sqrt(variance)
        val t = (mean - expectedMean) / (s / sqrt(n.toDouble()))
        val z = abs(t)
        val p = 2.0 * (1.0 - normalCdf(z))
        return p.coerceIn(0.0, 1.0)
    }

    @Test
    fun roundTripWithStats() {
        // Stats output should approach:
        // - 0.53125 for ArrayIntPermutation (skewed by size=1 and size=2)
        // - 0.5 for all others

        val pValuesByClass = mapOf<String, MutableList<Double>>(
            ArrayIntPermutation::class.simpleName!! to ArrayList(),
            HalfIntPermutation::class.simpleName!! to ArrayList(),
            UIntPermutation::class.simpleName!! to ArrayList(),
            FullIntPermutation::class.simpleName!! to ArrayList(),
        )

        repeat(outerIterations) { outerIdx ->

            val (perm, seed, rounds) = choseIntPerm()
            val size = perm.size
            val usize = size.toUInt()
            val className = perm::class.simpleName!!

            // Welford’s algorithm state
            var mean = 0.0
            var m2 = 0.0
            var count = 0

            repeat(innerIterations) {
                val x = if (size > 0) rng.nextInt(size)
                else rng.nextUInt(0u, usize).toInt()

                val y = perm.encode(x)
                val z = perm.decode(y)
                assertEquals(
                    x, z, "round-trip failed: " +
                            "size=$size rounds=$rounds seed=$seed x=$x y=$y z=$z"
                )

                val yu = y.toUInt()
                val yf = yu.toDouble()
                count++
                val delta = yf - mean
                mean += delta / count
                m2 += delta * (yf - mean)

                assertTrue(yu in 0u..<usize) {
                    "support anomaly: perm=$className size=$size " +
                            "rounds=$rounds seed=$seed"
                }
            }

            val variance = if (count > 1) m2 / (count - 1) else 0.0
            val expectedMean = (usize.toDouble() - 1.0) / 2.0

            val pValue = meanTPValue(mean, expectedMean, variance, count)

            pValuesByClass[className]!!.add(pValue)

            if (outerIdx % 1_000 == 0 && outerIdx > 0) {
                val s = pValuesByClass.map {
                    "${it.key} = ${String.format("%.3f", it.value.average())}"
                }
                println(s)
            }
        }
    }

    // -------- Long permutations --------

    private fun choseLongPerm(): Triple<LongPermutation, Long, Int> {
        val rounds = rng.nextInt(2, 8)
        val seed = rng.nextLong()
        val localRng = Random(seed)

        val perm: LongPermutation = when (rng.nextInt(3)) {
            // HalfLongPermutation over a bounded positive domain
            0 -> {
                val size = rng.nextLong(1L, 1L shl 20) // up to ~1M
                HalfLongPermutation(size, localRng, rounds)
            }
            // ULongPermutation over a bounded positive domain
            1 -> {
                val size = rng.nextLong(1L, 1L shl 20)
                ULongPermutation(size, localRng, rounds)
            }
            // FullLongPermutation over entire signed 64-bit space
            else -> FullLongPermutation(localRng, rounds)
        }

        return Triple(perm, seed, rounds)
    }

    @Test
    fun roundTripWithStatsLong() {
        // Same idea as Int, but testing HalfLongPermutation, ULongPermutation, FullLongPermutation.

        val pValuesByClass = mapOf<String, MutableList<Double>>(
            HalfLongPermutation::class.simpleName!! to ArrayList(),
            ULongPermutation::class.simpleName!! to ArrayList(),
            FullLongPermutation::class.simpleName!! to ArrayList(),
        )

        repeat(outerIterations) { outerIdx ->

            val (perm, seed, rounds) = choseLongPerm()
            val size = perm.size     // >0 for half/ulong, -1L sentinel for full
            val usize = size.toULong()
            val className = perm::class.simpleName!!

            // Welford
            var mean = 0.0
            var m2 = 0.0
            var count = 0

            repeat(innerIterations) {
                val x: Long =
                    if (size > 0L) {
                        rng.nextLong(size)          // 0 .. size-1
                    } else {
                        // FullLongPermutation: sample whole 64-bit space via ULong
                        rng.nextULong(0uL, usize).toLong()
                    }

                val y = perm.encode(x)
                val z = perm.decode(y)

                assertEquals(
                    x, z, "round-trip failed (Long): " +
                            "size=$size rounds=$rounds seed=$seed x=$x y=$y z=$z perm=$className"
                )

                val yu = y.toULong()
                val yf = yu.toDouble()

                count++
                val delta = yf - mean
                mean += delta / count
                m2 += delta * (yf - mean)

                // For finite domains, ensure encode() stays inside [0, size).
                // For full domain (size=-1L, usize==ULong.MAX_VALUE), this is always true.
                assertTrue(yu in 0uL..<usize) {
                    "support anomaly (Long): perm=$className size=$size " +
                            "rounds=$rounds seed=$seed"
                }
            }

            val variance = if (count > 1) m2 / (count - 1) else 0.0
            val expectedMean = (usize.toDouble() - 1.0) / 2.0

            val pValue = meanTPValue(mean, expectedMean, variance, count)

            pValuesByClass[className]!!.add(pValue)

            if (outerIdx % 1_000 == 0 && outerIdx > 0) {
                val s = pValuesByClass.map {
                    "${it.key} = ${String.format("%.3f", it.value.average())}"
                }
                println(s)
            }
        }
    }
}
