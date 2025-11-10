@file:JvmName("Permutations")

package com.eigenity.kpermute

import kotlin.random.Random

/**
 * Creates an [IntPermutation] for a contiguous integer domain.
 *
 * The concrete implementation depends on [size]:
 * - `size == -1` uses [FullIntPermutation] over the full signed 32-bit range.
 * - `size < 0` (excluding `-1`) uses [UIntPermutation] over an unsigned-style domain.
 * - `0 <= size <= 16` uses [ArrayIntPermutation].
 * - `size > 16` uses [HalfIntPermutation] with cycle-walking over a 2^k block.
 *
 * When [rounds] is `0`, a size-dependent default is chosen to balance speed and
 * dispersion. The permutation is reproducible for a given [rng] state.
 *
 * @param size domain size. For non-negative values the domain is `[0, size)`.
 *             Special values: `-1` selects a full 32-bit permutation, other
 *             negative values select an unsigned-style domain.
 * @param rng random source used to derive internal keys and parameters.
 * @param rounds number of mixing rounds; `0` selects a reasonable default per
 *               implementation.
 * @return an [IntPermutation] with domain `[0, size)` or the full 32-bit space
 *         when `size == -1`.
 */
@JvmOverloads
fun intPermutation(
    size: Int = Int.MAX_VALUE, rng: Random = Random.Default, rounds: Int = 0
): IntPermutation {
    require(rounds >= 0) { "rounds must be >= 0" }

    // Determine default rounds when not provided.
    fun defaultRoundsForHalf(n: Int): Int = when {
        n <= 1 shl 10 -> 3          // up to 1 K
        n <= 1 shl 20 -> 4          // up to 1 M
        else -> 6                   // larger domains
    }

    fun defaultRoundsForUInt(n: Int): Int = when {
        n <= 1 shl 16 -> 3
        n <= 1 shl 24 -> 4
        else -> 5
    }

    return when {
        size == -1 -> FullIntPermutation(
            rng, if (rounds == 0) 2 else rounds // FullInt needs few rounds
        )

        size < 0 -> UIntPermutation(
            size, rng, if (rounds == 0) defaultRoundsForUInt(size) else rounds
        )

        size <= 16 -> ArrayIntPermutation(size, rng)

        else -> HalfIntPermutation(
            size, rng, if (rounds == 0) defaultRoundsForHalf(size) else rounds
        )
    }
}

/**
 * Creates an [IntPermutation] using a seed-based [Random] instance.
 *
 * This overload behaves like [intPermutation] with an explicit rng, but
 * derives the random source from [seed]. For a fixed combination of
 * `[size, seed, rounds]` the resulting permutation is deterministic.
 *
 * @param size domain size; see [intPermutation] for semantics and special values.
 * @param seed seed used to construct the underlying [Random].
 * @param rounds number of mixing rounds; `0` selects a reasonable default per
 *               implementation.
 * @return an [IntPermutation] with domain `[0, size)` or the full 32-bit space
 *         when `size == -1`.
 * @see intPermutation
 */
@JvmOverloads
fun intPermutation(
    size: Int = Int.MAX_VALUE, seed: Long, rounds: Int = 0
): IntPermutation = intPermutation(size, Random(seed), rounds)

/**
 * Creates an [IntPermutation] for values within the given inclusive [range].
 *
 * Internally this constructs a permutation over a domain of length
 * `range.last - range.first + 1`, then wraps it so that [IntPermutation.encode]
 * and [IntPermutation.decode] operate directly on values in [range].
 *
 * The same implementation selection rules and default [rounds] logic as
 * [intPermutation] are used based on the range length.
 *
 * @param range inclusive range of values to permute.
 * @param rng random source used to derive internal keys and parameters.
 * @param rounds number of mixing rounds; `0` selects a reasonable default per
 *               implementation.
 * @return an [IntPermutation] whose domain is exactly [range].
 * @throws IllegalArgumentException if [range] is empty or its length exceeds [Int.MAX_VALUE].
 */
@JvmOverloads
fun intPermutation(
    range: IntRange, rng: Random = Random.Default, rounds: Int = 0
): IntPermutation {
    val nLong = range.last.toLong() - range.first.toLong() + 1L
    require(nLong > 0L) {
        "range must be non-empty and increasing: $range"
    }
    require(nLong <= Int.MAX_VALUE.toLong()) {
        "range size $nLong exceeds Int.MAX_VALUE"
    }
    return intPermutation(nLong.toInt(), rng, rounds).range(range)
}

/**
 * Creates an [IntPermutation] for the given inclusive [range] using a
 * seed-based [Random] instance.
 *
 * This overload behaves like [intPermutation] with an explicit rng, but
 * derives the random source from [seed].
 *
 * @param range inclusive range of values to permute.
 * @param seed seed used to construct the underlying [Random].
 * @param rounds number of mixing rounds; `0` selects a reasonable default per
 *               implementation.
 * @return an [IntPermutation] whose domain is exactly [range].
 * @see intPermutation
 */
fun intPermutation(
    range: IntRange, seed: Long, rounds: Int = 0
): IntPermutation = intPermutation(range, Random(seed), rounds)

/**
 * Creates a [LongPermutation] for a contiguous long domain.
 *
 * The concrete implementation depends on [size]:
 * - `size == -1L` uses [FullLongPermutation] over the full signed 64-bit range.
 * - `size < 0L` (excluding `-1L`) uses [ULongPermutation] over an unsigned-style domain.
 * - `0 <= size <= 16L` uses [ArrayLongPermutation].
 * - `size > 16L` uses [HalfLongPermutation] with cycle-walking over a 2^k block.
 *
 * When [rounds] is `0`, a size-dependent default is chosen to balance speed and
 * dispersion. The permutation is reproducible for a given [rng] state.
 *
 * @param size domain size. For non-negative values the domain is `[0, size)`.
 *             Special values: `-1L` selects a full 64-bit permutation, other
 *             negative values select an unsigned-style domain.
 * @param rng random source used to derive internal keys and parameters.
 * @param rounds number of mixing rounds; `0` selects a reasonable default per
 *               implementation.
 * @return a [LongPermutation] with domain `[0, size)` or the full 64-bit space
 *         when `size == -1L`.
 */
@JvmOverloads
fun longPermutation(
    size: Long = Long.MAX_VALUE, rng: Random = Random.Default, rounds: Int = 0
): LongPermutation {
    require(rounds >= 0) { "rounds must be >= 0" }

    fun defaultRoundsForHalf(n: Long): Int = when {
        n <= 1L shl 10 -> 3          // up to 1 K
        n <= 1L shl 20 -> 4          // up to 1 M
        else -> 6                    // larger domains
    }

    fun defaultRoundsForULong(n: Long): Int = when {
        n <= 1L shl 16 -> 3
        n <= 1L shl 24 -> 4
        else -> 5
    }

    return when {
        size == -1L -> FullLongPermutation(
            rng, if (rounds == 0) 2 else rounds
        )

        size < 0L -> ULongPermutation(
            size, rng, if (rounds == 0) defaultRoundsForULong(-size) else rounds
        )

        size <= 16L -> ArrayLongPermutation(size, rng)

        else -> HalfLongPermutation(
            size, rng, if (rounds == 0) defaultRoundsForHalf(size) else rounds
        )
    }
}

/**
 * Creates a [LongPermutation] using a seed-based [Random] instance.
 *
 * This overload behaves like [longPermutation] with an explicit rng, but
 * derives the random source from [seed]. For a fixed combination of
 * `[size, seed, rounds]` the resulting permutation is deterministic.
 *
 * @param size domain size; see [longPermutation] for semantics and special values.
 * @param seed seed used to construct the underlying [Random].
 * @param rounds number of mixing rounds; `0` selects a reasonable default per
 *               implementation.
 * @return a [LongPermutation] with domain `[0, size)` or the full 64-bit space
 *         when `size == -1L`.
 * @see longPermutation
 */
fun longPermutation(
    size: Long = Long.MAX_VALUE, seed: Long, rounds: Int = 0
): LongPermutation = longPermutation(size, Random(seed), rounds)

/**
 * Creates a [LongPermutation] for values within the given inclusive [range].
 *
 * Internally this constructs a permutation over a domain of length
 * `range.last - range.first + 1`, then wraps it so that [IntPermutation.encode]
 * and [IntPermutation.decode] operate directly on values in [range].
 *
 * The same implementation selection rules and default [rounds] logic as
 * [longPermutation] are used based on the range length.
 *
 * @param range inclusive range of values to permute.
 * @param rng random source used to derive internal keys and parameters.
 * @param rounds number of mixing rounds; `0` selects a reasonable default per
 *               implementation.
 * @return a [LongPermutation] whose domain is exactly [range].
 * @throws IllegalArgumentException if [range] is empty or its length exceeds [Long.MAX_VALUE].
 */
@JvmOverloads
fun longPermutation(
    range: LongRange, rng: Random = Random.Default, rounds: Int = 0
): LongPermutation {
    val nULong = range.last.toULong() - range.first.toULong() + 1uL
    require(nULong > 0uL) {
        "range must be non-empty and increasing: $range"
    }
    require(nULong <= Long.MAX_VALUE.toULong()) {
        "range size $nULong exceeds Long.MAX_VALUE"
    }
    return longPermutation(nULong.toLong(), rng, rounds).range(range)
}

/**
 * Creates a [LongPermutation] for the given inclusive [range] using a
 * seed-based [Random] instance.
 *
 * This overload behaves like [longPermutation] with an explicit rng, but
 * derives the random source from [seed].
 *
 * @param range inclusive range of values to permute.
 * @param seed seed used to construct the underlying [Random].
 * @param rounds number of mixing rounds; `0` selects a reasonable default per
 *               implementation.
 * @return a [LongPermutation] whose domain is exactly [range].
 * @see longPermutation
 */
@JvmOverloads
fun longPermutation(
    range: LongRange, seed: Long, rounds: Int = 0
): LongPermutation = longPermutation(range, Random(seed), rounds)
