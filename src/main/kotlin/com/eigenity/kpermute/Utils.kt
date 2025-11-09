package com.eigenity.kpermute

internal object PermMathInt {
    /** k = min s.t. 2^k >= size (size in 1..2^31), mask = (2^k - 1), rshift ~= k*3/7 */
    fun block(sizeExclusive: Int): Triple<Int /*mask*/, Int /*kBits*/, Int /*rshift*/> {
        val k =
            if (sizeExclusive <= 1) 1 else 32 - Integer.numberOfLeadingZeros(
                sizeExclusive - 1
            )
        val mask = -1 ushr (32 - k)            // avoids 1 shl 31 overflow
        val rshift = (k * 3) / 7
        return Triple(mask, k, rshift)
    }

    /** Inverse of v ^= v >>> s on k-bit words. */
    fun invXorShift(v0: Int, s0: Int, kBits: Int, mask: Int): Int {
        var v = v0 and mask
        var s = s0
        while (s < kBits) {
            v = v xor (v ushr s)
            s = s shl 1
        }
        return v and mask
    }

    /** Multiplicative inverse of an odd 32-bit Int modulo 2^k (mask = 2^k-1). */
    fun invOdd32(a: Int, mask: Int): Int {
        var inv = 1
        repeat(6) { inv *= (2 - a * inv) }     // Newton iteration
        return inv and mask
    }
}

internal object PermMathUInt {
    /** k = min s.t. 2^k >= size, mask = (2^k - 1) (k in 1..32). */
    fun block(sizeExclusive: UInt): Triple<UInt /*mask*/, Int /*kBits*/, Int /*rshift*/> {
        val k = if (sizeExclusive <= 1u) 1
        else 32 - Integer.numberOfLeadingZeros((sizeExclusive - 1u).toInt())
        val mask = if (k == 32) 0xFFFF_FFFFu else (1u shl k) - 1u
        val rshift = (k * 3) / 7
        return Triple(mask, k, rshift)
    }

    /** Inverse of v ^= v >>> s on k-bit words. */
    fun invXorShift(v0: UInt, s0: Int, kBits: Int, mask: UInt): UInt {
        var v = v0 and mask
        var s = s0
        while (s < kBits) {
            v = v xor (v shr s)
            s = s shl 1
        }
        return v and mask
    }

    /** Multiplicative inverse of an odd UInt modulo 2^k (mask = 2^k-1). */
    fun invOdd32(a: UInt, mask: UInt): UInt {
        var inv = 1u
        repeat(6) { inv = (inv * (2u - a * inv)) and mask }
        return inv and mask
    }
}


internal object PermMathLong {
    /** k = min s.t. 2^k >= size (size in 1..2^63), mask = (2^k - 1), rshift ~= k*3/7 */
    fun block(sizeExclusive: Long): Triple<Long /*mask*/, Int /*kBits*/, Int /*rshift*/> {
        val k = if (sizeExclusive <= 1L) 1
        else 64 - java.lang.Long.numberOfLeadingZeros(sizeExclusive - 1L)
        val mask = if (k == 64) -1L else -1L ushr (64 - k)
        val rshift = (k * 3) / 7
        return Triple(mask, k, rshift)
    }

    /** Inverse of v ^= v >>> s on k-bit words. */
    fun invXorShift(v0: Long, s0: Int, kBits: Int, mask: Long): Long {
        var v = v0 and mask
        var s = s0
        while (s < kBits) {
            v = v xor (v ushr s)
            s = s shl 1
        }
        return v and mask
    }

    /** Multiplicative inverse of an odd 64-bit Long modulo 2^k (mask = 2^k-1). */
    fun invOdd64(a: Long, mask: Long): Long {
        var inv = 1L
        // 6 iterations are enough for 64 bits (1 → 2 → 4 → 8 → 16 → 32 → 64)
        repeat(6) { inv *= (2L - a * inv) }
        return inv and mask
    }
}

internal object PermMathULong {
    /** k = min s.t. 2^k >= size, mask = (2^k - 1) (k in 1..64). */
    fun block(sizeExclusive: ULong): Triple<ULong /*mask*/, Int /*kBits*/, Int /*rshift*/> {
        val k = if (sizeExclusive <= 1uL) 1
        else 64 - java.lang.Long.numberOfLeadingZeros((sizeExclusive - 1uL).toLong())
        val mask = if (k == 64) 0xFFFF_FFFF_FFFF_FFFFuL else (1uL shl k) - 1uL
        val rshift = (k * 3) / 7
        return Triple(mask, k, rshift)
    }

    /** Inverse of v ^= v >>> s on k-bit words. */
    fun invXorShift(v0: ULong, s0: Int, kBits: Int, mask: ULong): ULong {
        var v = v0 and mask
        var s = s0
        while (s < kBits) {
            v = v xor (v shr s)
            s = s shl 1
        }
        return v and mask
    }

    /** Multiplicative inverse of an odd ULong modulo 2^k (mask = 2^k-1). */
    fun invOdd64(a: ULong, mask: ULong): ULong {
        var inv = 1uL
        repeat(6) { inv = (inv * (2uL - a * inv)) and mask }
        return inv and mask
    }
}
