package com.eigenity.kpermute

import kotlin.random.Random

/**
 * Using format-preserving encryption with cycling.
 */
class IntPermutation(val size: Int = Int.MAX_VALUE, rng: Random, val prime:Int = 0x7F4A7C15) : Iterable<Int> {

    private val mask: Int
    private val rish: Int
    private val rk1 = rng.nextInt()
    private val rk2 = rng.nextInt()
    private val rk3 = rng.nextInt()

    private val invConst: Int  // multiplicative inverse of const modulo 2^k (k = bits(mask+1))

    init {
        require(size > 0)
        var i = 8
        var j = 3
        while (j < 31 && i < size) {
            i += i
            j++
        }
        this.mask = i - 1
        this.rish = j * 3 / 7
        this.invConst = mulInverseOdd32(prime, mask)
    }

    fun encode(value: Int): Int {
        require(value in 0 until size)
        var x = value
        do {
            x = roundF(x, rk1)
            x = unlinearF(x)
            x = roundF(x, rk2)
            x = unlinearF(x)
            x = roundF(x, rk3)
            x = unlinearF(x)
        } while (x >= this.size)
        return x
    }

    /** Inverse of encode. */
    fun decode(value: Int): Int {
        require(value in 0 until size)
        var x = value
        do {
            // apply inverse rounds in reverse order
            x = invUnlinearF(x)
            x = invRoundF(x, rk3)
            x = invUnlinearF(x)
            x = invRoundF(x, rk2)
            x = invUnlinearF(x)
            x = invRoundF(x, rk1)
        } while (x >= this.size)
        return x
    }

    // ---- helpers ----

    // forward: x = (x * const + rk) mod 2^k
    private fun roundF(x0: Int, rk: Int): Int =
        ((x0 * prime) + rk) and mask

    // forward: x ^= x >>> rish (already in k-bit space)
    private fun unlinearF(x0: Int): Int = x0 xor (x0 ushr rish)

    // inverse of roundF: x = (x - rk) * const^{-1} mod 2^k
    private fun invRoundF(y0: Int, rk: Int): Int {
        val y = (y0 - rk) and mask
        // multiply modulo 2^k
        return (y * invConst) and mask
    }

    // inverse of x ^= x >>> s for k-bit words
    private fun invUnlinearF(y0: Int): Int {
        var x = y0
        var shift = rish
        while (shift < bitWidth()) {
            x = x xor (x ushr shift)
            shift = shift shl 1
        }
        return x and mask
    }

    // multiplicative inverse of an odd 32-bit constant modulo 2^k where mask = 2^k - 1
    private fun mulInverseOdd32(a: Int, mask: Int): Int {
        // Newton–Raphson on Z/2^kZ: inv_{n+1} = inv_n * (2 - a*inv_n)  (mod 2^k)
        var inv = a // correct mod 2
        // Lift to full k bits; 5 iterations suffice for up to 32 bits
        repeat(5) {
            inv *= (2 - a * inv)
        }
        return inv and mask
    }

    private fun bitWidth(): Int {
        // mask = 2^k-1 => k = number of 1-bits in mask+1's highest power-of-two
        // since we built mask by doubling, we can count bits via Integer.SIZE - numberOfLeadingZeros(mask)
        return 32 - Integer.numberOfLeadingZeros(mask)
    }

    override fun iterator(): IntIterator {
        var i = 0
        return object : IntIterator() {
            override fun hasNext() = i < size
            override fun nextInt() = encode(i++)
        }
    }
}

fun main() {

    println("This example shows how to encode a Long ID to e.g. obfuscate how many users you have in your app")
    val perm = IntPermutation(size = 100, Random(1248192), prime = 3)
    val encoded = perm.encode(42)
    println("encoded: $encoded (always 85 for this fixed seed)")
    val decoded = perm.decode(encoded)
    println("decoded: $decoded (should be 42)")
    // perm.encode(1204) // too big to encode since the size was set to 100
    println()

    // This example shows how to shuffle a very large list
    // pretend this list is read from a disk with random access, for example, with a parquet file with billions of rows
    val list = List(100) { it }

    for (i in list.indices) {
        // this will print all elements shuffled without loading the whole list in memory
        print("" + list[perm.encode(i)] + ", ")
    }
    println()

}
