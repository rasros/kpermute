package com.eigenity.kpermute

import kotlin.random.Random

class HalfIntPermutation(
    override val size: Int = Int.MAX_VALUE,
    rng: Random = Random.Default,
    override val rounds: Int = 3,
    val const: Int = 0x7F4A7C15
) : IntPermutation {

    private val mask: Int
    private val kBits: Int
    private val rshift: Int
    private val keys = IntArray(rounds) { rng.nextInt() }
    private val invConst: Int

    init {
        require(size > 0) { "size must be > 0" }
        require(rounds > 0) { "rounds must be > 0" }
        require(const % 2 == 1) { "const must be odd" }

        val (m, k, r) = PermMathInt.block(size)
        mask = m; kBits = k; rshift = r
        invConst = PermMathInt.invOdd32(const, mask)
    }

    override fun encode(value: Int): Int {
        require(value in 0 until size)
        if (size == 1) return 0
        var x = value
        do {
            repeat(rounds) { r ->
                x = (x * const + keys[r]) and mask
                x = x xor (x ushr rshift)
            }
        } while (x >= size)
        return x
    }

    override fun decode(encoded: Int): Int {
        require(encoded in 0 until size)
        if (size == 1) return 0
        var x = encoded
        do {
            for (r in rounds - 1 downTo 0) {
                x = PermMathInt.invXorShift(x, rshift, kBits, mask)
                x = ((x - keys[r]) and mask) * invConst and mask
            }
        } while (x >= size)
        return x
    }

    override fun iterator(offset: Int): IntIterator {
        var i = offset
        return object : IntIterator() {
            override fun hasNext() = i < size
            override fun nextInt(): Int {
                if (!hasNext()) throw NoSuchElementException()
                return encode((i++))
            }
        }
    }
}
