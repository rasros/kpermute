package com.eigenity.kpermute

import kotlin.random.Random

/**
 * Finite permutation over an unsigned 64-bit-style domain encoded as `Long`.
 *
 * Interprets values as `ULong` and permutes `[0, size)` using reversible affine
 * steps and XOR-shift mixing within a 2^k block. Cycle walking ensures that
 * all outputs stay inside the unsigned domain.
 *
 * @param [size] Size of the unsigned domain encoded as a `Long`.
 * @param [rng] Random generator used to derive per-round keys.
 * @param [rounds] Number of mixing rounds; higher values increase dispersion.
 * @param [const] Odd multiplicative constant used in each affine step.
 */
@OptIn(ExperimentalUnsignedTypes::class)
class ULongPermutation(
    override val size: Long,
    rng: Random = Random.Default,
    private val rounds: Int = 3,
    private val const: ULong = 0x9E3779B97F4A7C15U
) : LongPermutation {

    private val usize: ULong = size.toULong()
    private val mask: ULong
    private val kBits: Int
    private val rshift: Int
    private val keys: ULongArray =
        ULongArray(rounds) { rng.nextLong().toULong() }
    private val invConst: ULong

    init {
        require(rounds > 0) { "rounds must be > 0" }
        require(const % 2uL == 1uL) { "const must be odd" }

        val (m, k, r) = PermMathULong.block(usize)
        mask = m; kBits = k; rshift = r
        invConst = PermMathULong.invOdd64(const, mask)
    }

    override fun encodeUnchecked(value: Long): Long {
        val u = value.toULong()
        var x = u and mask
        do {
            repeat(rounds) { r ->
                x = (x * const + keys[r]) and mask
                x = x xor (x shr rshift)
            }
        } while (x >= usize)
        return x.toLong()
    }

    override fun decodeUnchecked(encoded: Long): Long {
        val u = encoded.toULong()
        var x = u and mask
        do {
            for (r in rounds - 1 downTo 0) {
                x = PermMathULong.invXorShift(x, rshift, kBits, mask)
                x = ((x - keys[r]) and mask) * invConst and mask
            }
        } while (x >= usize)
        return x.toLong()
    }

    override fun iterator(offset: Long): LongIterator {
        var i = offset.toULong()
        return object : LongIterator() {
            override fun hasNext() = i < usize
            override fun nextLong(): Long {
                if (!hasNext()) throw NoSuchElementException()
                return encodeUnchecked((i++).toLong())
            }
        }
    }

    override fun toString(): String = "ULongPermutation(size=$size)"
}
