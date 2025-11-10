package com.eigenity.kpermute

import kotlin.random.Random

/**
 * Full 32-bit integer permutation over the entire signed `Int` domain.
 *
 * Uses an invertible hash built from XOR, shifts, and odd multipliers with
 * per-round keys. No tables are stored; encode and decode apply forward and
 * inverse rounds respectively.
 *
 * @param [rng] Source of randomness for generating round keys.
 * @param [rounds] Number of mixing rounds; more rounds increase dispersion.
 * @param [c1] First odd multiplicative constant used in the mixing steps.
 * @param [c2] Second odd multiplicative constant used in the mixing steps.
 */
class FullIntPermutation(
    rng: Random = Random.Default,
    private val rounds: Int = 2,
    private val c1: Int = -2048144789,
    private val c2: Int = -1028477387
) : IntPermutation {

    override val size: Int get() = -1

    private val k1: IntArray = IntArray(rounds) { rng.nextInt() }
    private val k2: IntArray = IntArray(rounds) { rng.nextInt() }
    private val c1Inv: Int = PermMathInt.invOdd32(c1, -1)
    private val c2Inv: Int = PermMathInt.invOdd32(c2, -1)

    override fun encodeUnchecked(value: Int): Int {
        var x = value
        repeat(rounds) { r ->
            x = x xor k1[r]
            x = x xor (x ushr 16); x *= c1
            x = x xor (x ushr 15); x *= c2
            x = x xor (x ushr 16)
            x = x xor k2[r]
        }
        return x
    }

    override fun decodeUnchecked(encoded: Int): Int {
        var y = encoded
        for (r in rounds - 1 downTo 0) {
            y = y xor k2[r]
            y = PermMathInt.invXorShift(y, 16, 32, -1); y *= c2Inv
            y = PermMathInt.invXorShift(y, 15, 32, -1); y *= c1Inv
            y = PermMathInt.invXorShift(y, 16, 32, -1)
            y = y xor k1[r]
        }
        return y
    }

    override fun iterator(offset: Int): IntIterator {
        var i = offset
        return object : IntIterator() {
            override fun hasNext() = i != -1
            override fun nextInt(): Int {
                if (!hasNext()) throw NoSuchElementException()
                return encodeUnchecked((i++))
            }
        }
    }

    override fun toString(): String = "FullIntPermutation(size=$size)"
}
