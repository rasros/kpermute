package com.eigenity.kpermute

import kotlin.random.Random

/**
 * Finite permutation over an unsigned 32-bit-style domain encoded as `Int`.
 *
 * Interprets values as `UInt` and permutes `[0, size)` using reversible affine
 * steps and XOR-shift mixing within a 2^k block. Cycle walking ensures that
 * all outputs stay inside the unsigned domain.
 *
 * @param [size] Size of the unsigned domain encoded as an `Int`.
 * @param [rng] Random generator used to derive per-round keys.
 * @param [rounds] Number of mixing rounds; higher values increase dispersion.
 * @param [const] Odd multiplicative constant used in each affine step.
 */
@OptIn(ExperimentalUnsignedTypes::class)
class UIntPermutation(
    override val size: Int,
    rng: Random = Random.Default,
    private val rounds: Int = 3,
    private val const: UInt = 0x9E3779B1u
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

    override fun encodeUnchecked(value: Int): Int {
        val u = value.toUInt()
        var x = u and mask
        do {
            repeat(rounds) { r ->
                x = (x * const + keys[r]) and mask
                x = x xor (x shr rshift)
            }
        } while (x >= usize)
        return x.toInt()
    }

    override fun decodeUnchecked(encoded: Int): Int {
        val u = encoded.toUInt()
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
                return encodeUnchecked((i++).toInt())
            }
        }
    }

    override fun toString(): String = "UIntPermutation(size=$size)"

}
