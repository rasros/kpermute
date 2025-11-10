package com.eigenity.kpermute

import kotlin.random.Random

/**
 * Full 64-bit integer permutation over the entire signed `Long` domain.
 *
 * Uses an invertible 64-bit hash built from XOR, shifts, and odd multipliers
 * with per-round keys. No tables are stored; encode and decode apply forward
 * and inverse rounds respectively.
 *
 * @param [rng] Source of randomness for generating round keys.
 * @param [rounds] Number of mixing rounds; more rounds increase dispersion.
 * @param [c1] First odd multiplicative constant used in the mixing steps.
 * @param [c2] Second odd multiplicative constant used in the mixing steps.
 */
class FullLongPermutation(
    rng: Random = Random.Default,
    private val rounds: Int = 2,
    private val c1: Long = -4658895280553007687,
    private val c2: Long = -7723592293110705685
) : LongPermutation {

    override val size: Long get() = -1L

    private val k1: LongArray = LongArray(rounds) { rng.nextLong() }
    private val k2: LongArray = LongArray(rounds) { rng.nextLong() }
    private val c1Inv: Long = PermMathLong.invOdd64(c1, -1L)
    private val c2Inv: Long = PermMathLong.invOdd64(c2, -1L)

    override fun encodeUnchecked(value: Long): Long {
        var x = value
        repeat(rounds) { r ->
            x = x xor k1[r]
            x = x xor (x ushr 30); x *= c1
            x = x xor (x ushr 27); x *= c2
            x = x xor (x ushr 31)
            x = x xor k2[r]
        }
        return x
    }

    override fun decodeUnchecked(encoded: Long): Long {
        var y = encoded
        for (r in rounds - 1 downTo 0) {
            y = y xor k2[r]
            y = PermMathLong.invXorShift(y, 31, 64, -1L); y *= c2Inv
            y = PermMathLong.invXorShift(y, 27, 64, -1L); y *= c1Inv
            y = PermMathLong.invXorShift(y, 30, 64, -1L)
            y = y xor k1[r]
        }
        return y
    }

    override fun iterator(offset: Long): LongIterator {
        var i = offset
        return object : LongIterator() {
            override fun hasNext() = i != -1L
            override fun nextLong(): Long {
                if (!hasNext()) throw NoSuchElementException()
                return encodeUnchecked(i++)
            }
        }
    }

    override fun toString(): String = "FullLongPermutation(size=$size)"
}
