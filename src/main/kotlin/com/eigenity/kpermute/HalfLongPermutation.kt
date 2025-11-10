package com.eigenity.kpermute

import kotlin.math.absoluteValue
import kotlin.random.Random

/**
 * Finite long permutation using cycle-walking over a 2^k-sized block.
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
class HalfLongPermutation(
    override val size: Long = Long.MAX_VALUE,
    rng: Random = Random.Default,
    private val rounds: Int = 3,
    private val const: Long = 0x52A531B54E4EC5CBL
) : LongPermutation {

    private val mask: Long
    private val kBits: Int
    private val rshift: Int
    private val keys = LongArray(rounds) { rng.nextLong() }
    private val invConst: Long

    init {
        require(size > 0L) { "size must be > 0" }
        require(rounds > 0) { "rounds must be > 0" }
        require((const % 2L).absoluteValue == 1L) { "const must be odd" }

        val (m, k, r) = PermMathLong.block(size)
        mask = m; kBits = k; rshift = r
        invConst = PermMathLong.invOdd64(const, mask)
    }

    override fun encodeUnchecked(value: Long): Long {
        var x = value
        do {
            repeat(rounds) { r ->
                x = (x * const + keys[r]) and mask
                x = x xor (x ushr rshift)
            }
        } while (x >= size)
        return x
    }

    override fun decodeUnchecked(encoded: Long): Long {
        var x = encoded
        do {
            for (r in rounds - 1 downTo 0) {
                x = PermMathLong.invXorShift(x, rshift, kBits, mask)
                x = ((x - keys[r]) and mask) * invConst and mask
            }
        } while (x >= size)
        return x
    }

    override fun iterator(offset: Long): LongIterator {
        var i = offset
        return object : LongIterator() {
            override fun hasNext() = i < size
            override fun nextLong(): Long {
                if (!hasNext()) throw NoSuchElementException()
                return encodeUnchecked(i++)
            }
        }
    }

    override fun toString(): String = "HalfLongPermutation(size=$size)"
}
