package com.eigenity.kpermute

import kotlin.random.Random

class FullIntPermutation(
    rng: Random = Random.Default,
    private val rounds: Int = 2,
    private val c1: Int = 0x7F4A7C15,
    private val c2: Int = 0x27D4EB2D
) : IntPermutation {

    override val size: Int get() = -1

    private val k1: IntArray = IntArray(rounds) { rng.nextInt() }
    private val k2: IntArray = IntArray(rounds) { rng.nextInt() }
    private val c1Inv: Int = PermMathInt.invOdd32(c1, -1)
    private val c2Inv: Int = PermMathInt.invOdd32(c2, -1)

    override fun encode(value: Int): Int {
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

    override fun decode(encoded: Int): Int {
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
                return encode((i++))
            }
        }
    }
}
