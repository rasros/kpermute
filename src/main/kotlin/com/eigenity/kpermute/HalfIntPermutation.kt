package com.eigenity.kpermute

import kotlin.math.absoluteValue
import kotlin.random.Random

/**
 * Finite integer permutation using cycle-walking over a 2^k-sized block.
 *
 * Embeds the domain `[0, size)` in a power-of-two block and applies reversible
 * affine steps plus XOR-shift mixing. Outputs outside `[0, size)` are
 * rejected and walked until they fall back inside the domain.
 *
 * @param [size] Size of the permutation domain; valid inputs are `[0, size)`.
 * @param [rng] Random generator used to derive per-round keys.
 * @param [rounds] Number of mixing rounds; higher values increase dispersion.
 * @param [const] Odd multiplicative constant used in each affine step.
 */
class HalfIntPermutation(
    override val size: Int = Int.MAX_VALUE,
    rng: Random = Random.Default,
    private val rounds: Int = 3,
    private val const: Int = -1640531535
) : IntPermutation {

    private val mask: Int
    private val kBits: Int
    private val rshift: Int
    private val keys = IntArray(rounds) { rng.nextInt() }
    private val invConst: Int

    init {
        require(size > 0) { "size must be > 0" }
        require(rounds > 0) { "rounds must be > 0" }
        require((const % 2).absoluteValue == 1) { "const must be odd" }

        val (m, k, r) = PermMathInt.block(size)
        mask = m; kBits = k; rshift = r
        invConst = PermMathInt.invOdd32(const, mask)
    }

    override fun encodeUnchecked(value: Int): Int {
        var x = value
        do {
            repeat(rounds) { r ->
                x = (x * const + keys[r]) and mask
                x = x xor (x ushr rshift)
            }
        } while (x >= size)
        return x
    }

    override fun decodeUnchecked(encoded: Int): Int {
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
                return encodeUnchecked((i++))
            }
        }
    }

    override fun toString(): String = "HalfIntPermutation(size=$size)"
}
