package com.eigenity.kpermute

import kotlin.random.Random

/**
 * The `IntPermutation` class provides a fast, repeatable integer permutation
 * for non-cryptographic ID obfuscation using a cycle-walking hash algorithm.
 * It ensures that given the same parameters, the same permutation is produced
 * every time, making it suitable for repeatable shuffles and data masking.
 *
 * Input Variables:
 * [size] The size of the integer domain to permute.
 * [rng] A random number generator used to initialize keys and parameters.
 * [rounds] The number of rounds in the permutation algorithm.
 */
fun intPermutation(
    size: UInt = UInt.MAX_VALUE,
    rng: Random = Random.Default,
    rounds: Int = 0
): IntPermutation {
    require(rounds >= 0)
    return when {
        size == UInt.MAX_VALUE ->
            FullIntPermutation(
                rng,
                if (rounds == 0) 1 else rounds
            )

        size.toInt() < 0 ->
            UIntPermutation(
                size,
                rng,
                if (rounds == 0) 3 else rounds
            )

        else ->
            HalfIntPermutation(
                size.toInt(),
                rng,
                if (rounds == 0) 3 else rounds
            )
    }
}

interface IntPermutation : Iterable<Int> {
    /** Encode an integer in [0, size) into its permuted value. */
    fun encode(value: Int): Int

    /** Decode a previously encoded integer back to its original value. */
    fun decode(encoded: Int): Int

    /** Iterator that yields encoded values for [0, size). */
    override fun iterator(): IntIterator = iterator(0)

    /** Iterator that yields encoded values for [offset, size-offset). */
    fun iterator(offset: Int): IntIterator

}

/**
 * This implementation is the most commonly used when for input sizes
 * up to half int range 2^31
 */
class HalfIntPermutation(
    val size: Int = Int.MAX_VALUE,
    rng: Random = Random.Default,
    val rounds: Int = 3,
    val const: Int = 0x7F4A7C15,
    val offset: Int = 0
) : IntPermutation {

    private val mask: Int           // 2^k - 1 with 2^k >= size
    private val rshift: Int         // right-shift count
    private val keys = IntArray(rounds) { rng.nextInt() }
    private val invConst: Int       // multiplicative inverse of const mod 2^k

    init {
        require(size > 0) { "size must be > 0" }
        require(const % 2 == 1) { "const must be odd" }
        require(rounds > 0) { "rounds must be > 0" }
        require(offset >= 0) { "offset must be >= 0" }

        // Find the smallest power of two >= size
        var pow = 8
        var bits = 3
        while (bits < 31 && pow < size) {
            pow = pow shl 1
            bits++
        }
        mask = pow - 1
        rshift = bits * 3 / 7
        invConst = multiplicativeInverse32(const, mask)
    }

    override fun encode(value: Int): Int {
        require(value in 0 until size)
        var x = value
        do {
            repeat(rounds) { r ->
                x = (x * const + keys[r]) and mask
                x = x xor (x ushr rshift)
            }
        } while (x >= size)
        return x + offset
    }

    override fun decode(encoded: Int): Int {
        require(encoded in 0 until size)
        var x = encoded - offset
        do {
            // reverse order of rounds
            for (r in rounds - 1 downTo 0) {
                x = invertXorShift(x)
                x = ((x - keys[r]) and mask) * invConst and mask
            }
        } while (x >= size)
        return x
    }

    override fun iterator(offset: Int): IntIterator {
        var i = offset
        return object : IntIterator() {
            override fun hasNext() = i < size
            override fun nextInt() = encode(i++)
        }
    }

    /** Inverse of x ^= x >>> rshift for k-bit domain. */
    private fun invertXorShift(y0: Int): Int {
        var x = y0
        var shift = rshift
        while (shift < bitWidth()) {
            x = x xor (x ushr shift)
            shift = shift shl 1
        }
        return x and mask
    }

    /** Compute multiplicative inverse of an odd integer modulo 2^k. */
    private fun multiplicativeInverse32(a: Int, mask: Int): Int {
        var inv = a
        // Newton iteration mod 2^n
        repeat(5) { inv *= (2 - a * inv) }
        return inv and mask
    }

    private fun bitWidth(): Int = 32 - Integer.numberOfLeadingZeros(mask)
}


/**
 * Fast full-range (2^32) permutation on Int using an
 * ARX bijection + Even–Mansour wrap.
 */
class FullIntPermutation(
    rng: Random,
    private val rounds: Int,              // 1 is usually enough; use 2 for stronger diffusion
    private val c1: Int = 0x7F4A7C15, // odd -> has modular inverse mod 2^32
    private val c2: Int = 0x27D4EB2D  // odd -> has modular inverse mod 2^32
) : IntPermutation {
    private val k1: IntArray = IntArray(rounds) { rng.nextInt() }
    private val k2: IntArray = IntArray(rounds) { rng.nextInt() }
    private val c1Inv: Int = invOdd32(c1)
    private val c2Inv: Int = invOdd32(c2)

    override fun encode(value: Int): Int {
        var x = value
        repeat(rounds) { r ->
            // Even–Mansour pre-whitening
            x = x xor k1[r]
            x = run {
                var x = x
                x = x xor (x ushr 16)
                x *= c1
                x = x xor (x ushr 15)
                x *= c2
                x = x xor (x ushr 16)
                x
            }

            x = x xor k2[r]   // post-whitening
        }
        return x
    }

    override fun decode(encoded: Int): Int {
        var y = encoded
        for (r in rounds - 1 downTo 0) {
            y = y xor k2[r]
            y = run {
                var y = y
                y = invXorShift(y, 16)
                y *= c2Inv
                y = invXorShift(y, 15)
                y *= c1Inv
                y = invXorShift(y, 16)
                y
            }
            y = y xor k1[r]
        }
        return y
    }

    override fun iterator(offset: Int): IntIterator {
        var i = offset
        return object : IntIterator() {
            override fun hasNext() = i < Int.MAX_VALUE
            override fun nextInt() = encode(i++)
        }
    }

    /* ---------- Helpers (all Int; wraparound is mod 2^32) ---------- */

    // Inverse of: v ^= v >>> s (do XOR/shift with doubling window)
    private fun invXorShift(v0: Int, s0: Int): Int {
        var v = v0
        var s = s0
        while (s < 32) {
            v = v xor (v ushr s)
            s = s shl 1
        }
        return v
    }

    // Multiplicative inverse of an odd 32-bit Int modulo 2^32 (Newton iteration).
    private fun invOdd32(a: Int): Int {
        var inv = 1  // correct modulo 2 when 'a' is odd
        repeat(6) { inv *= (2 - a * inv) } // doubles correct bits each time; 6 >= log2(32)
        return inv
    }
}

/**
 * Cycle-walking hash-based permutation over the inclusive domain [0..size].
 */
class UIntPermutation(
    val size: UInt,
    rng: Random = Random.Default,
    val rounds: Int = 3,
    val const: UInt = 0x7F4A7C15u
) : IntPermutation {

    private val kBits: Int                   // minimal k with 2^k >= (last + 1)
    private val blockMask: UInt              // (1u << kBits) - 1, or 0xFFFF_FFFFu for k=32
    private val fullRange: Boolean           // true when last == 0xFFFF_FFFFu (2^32 states)
    private val keys: IntArray = IntArray(rounds) { rng.nextInt() }
    private val invConst: UInt               // multiplicative inverse of const mod 2^k

    init {
        require(rounds > 0) { "rounds must be > 0" }
        require(const % 2u == 1u) { "const must be odd" }

        // k = ceil(log2(last + 1)).
        // For last == 0xFFFF_FFFFu => k = 32.
        kBits = when (size) {
            0u -> 1 // size=1 (domain {0}) — trivial; k=1 keeps logic simple
            0xFFFF_FFFFu -> 32
            else -> 32 - Integer.numberOfLeadingZeros(size.toInt()) // floor(log2(last)) + 1
        }

        blockMask = if (kBits == 32) 0xFFFF_FFFFu else ((1u shl kBits) - 1u)
        fullRange = (size == 0xFFFF_FFFFu)
        invConst = multiplicativeInversePow2(const, kBits, blockMask)
    }

    override fun encode(value: Int): Int {
        val uvalue = value.toUInt()
        require(uvalue <= size) { "value out of range [0, $size]" }
        var x = uvalue and blockMask

        if (fullRange) {
            // Whole 32-bit space: no cycle-walking, just rounds.
            repeat(rounds) { r ->
                x = (x * const + keys[r].toUInt()) and blockMask
                x = x xor (x shr rshift())
            }
            return x.toInt()
        } else {
            // Retry until result ≤ last (i.e., < size = last+1)
            do {
                repeat(rounds) { r ->
                    x = (x * const + keys[r].toUInt()) and blockMask
                    x = x xor (x shr rshift())
                }
            } while (x > size)
            return x.toInt()
        }
    }

    override fun decode(encoded: Int): Int {
        val uvalue = encoded.toUInt()
        require(uvalue <= size) { "value out of range [0, $size]" }
        var x = uvalue and blockMask

        if (fullRange) {
            repeat(rounds) { r ->
                x = invertXorShift(x)
                // (x - key) * invConst  mod 2^k
                val key = keys[rounds - 1 - r]
                x = ((x - key.toUInt()) and blockMask) * invConst and blockMask
            }
            return x.toInt()
        } else {
            do {
                repeat(rounds) { r ->
                    x = invertXorShift(x)
                    val key = keys[rounds - 1 - r]
                    x =
                        ((x - key.toUInt()) and blockMask) * invConst and blockMask
                }
            } while (x > size)
            return x.toInt()
        }
    }

    override fun iterator(offset: Int): IntIterator {
        var i: UInt = offset.toUInt()
        return object : IntIterator() {
            override fun hasNext() = i <= size
            override fun nextInt(): Int {
                if (!hasNext()) throw NoSuchElementException()
                return encode(i.toInt()).also { i++ }
            }
        }
    }

    /* ---------------- Helpers ---------------- */

    // Right-shift count heuristic like your original: floor((k * 3) / 7)
    private fun rshift(): Int = (kBits * 3) / 7

    // Inverse of x ^= x >>> s on k-bit words
    private fun invertXorShift(y0: UInt): UInt {
        var x = y0 and blockMask
        var s = rshift()
        while (s < kBits) {
            x = x xor (x shr s)
            s = s shl 1
        }
        return x and blockMask
    }

    // Multiplicative inverse of an odd constant modulo 2^k via Newton iteration
    private fun multiplicativeInversePow2(a: UInt, k: Int, mask: UInt): UInt {
        var inv = 1u // a is odd -> inverse is 1 modulo 2
        // Each iteration doubles correct bits; 6 is enough for up to 32 bits
        repeat(6) {
            inv = (inv * (2u - (a * inv))) and mask
        }
        return inv and mask
    }
}
