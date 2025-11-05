package com.eigenity.kpermute

import kotlin.random.Random

@OptIn(ExperimentalUnsignedTypes::class)
class UIntPermutation(
    override val size: Int,
    rng: Random = Random.Default,
    override val rounds: Int = 3,
    val const: UInt = 0x7F4A7C15u            // must be odd
) : IntPermutation {

    private val usize = size.toUInt()
    private val mask: UInt
    private val kBits: Int
    private val rshift: Int
    private val keys: UIntArray = UIntArray(rounds) { rng.nextInt().toUInt() }
    private val invConst: UInt

    init {
        require(rounds > 0) { "rounds must be > 0" }
        require(const % 2u == 1u) { "const must be odd" }

        val (m, k, r) = PermMathUInt.block(size.toUInt())
        mask = m; kBits = k; rshift = r
        invConst = PermMathUInt.invOdd32(const, mask)
    }

    override fun encode(value: Int): Int {
        val u = value.toUInt()
        require(u < usize) { "value out of range [0, $size)" }
        if (size == 1) return 0
        var x = u and mask
        do {
            repeat(rounds) { r ->
                x = (x * const + keys[r]) and mask
                x = x xor (x shr rshift)
            }
        } while (x >= usize)
        return x.toInt()
    }

    override fun decode(encoded: Int): Int {
        val u = encoded.toUInt()
        require(u < usize) { "value out of range [0, $size)" }
        if (size == 1) return 0
        var x = u and mask
        do {
            for (r in rounds - 1 downTo 0) {
                x = PermMathUInt.invXorShift(x, rshift, kBits, mask)
                x = ((x - keys[r]) and mask) * invConst and mask
            }
        } while (x >= usize)
        return x.toInt()
    }

    override fun iterator(offset: Int): IntIterator {
        var i = offset.toUInt()
        return object : IntIterator() {
            override fun hasNext() = i < usize
            override fun nextInt(): Int {
                if (!hasNext()) throw NoSuchElementException()
                return encode((i++).toInt())
            }
        }
    }
}
