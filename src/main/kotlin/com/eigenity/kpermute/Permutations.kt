@file:JvmName("IntPermutations")

package com.eigenity.kpermute

import kotlin.random.Random

/**
 * Provides an `IntPermutation` instance, a fast repeatable integer permutation
 * for shuffling lists and data masking using a cycle-walking hash algorithm.
 *
 * Input Variables:
 *
 * [size] The size of the integer domain to permute. This also decides which
 * implementation is used, normally [HalfIntPermutation] but for negative values
 * [UIntPermutation] and for -1 [FullIntPermutation].
 *
 * [rng] A random number generator used to initialize keys and parameters.
 *
 * [rounds] The number of rounds in the permutation algorithm. Use 8 for high
 * dispersion requirements and minimum 3 for low requirements.
 */
@JvmOverloads
fun intPermutation(
    size: Int = UInt.MAX_VALUE.toInt(),
    rng: Random = Random.Default,
    rounds: Int = 0
) = intPermutation(size.toUInt(), rng, rounds)

/**
 * Provides an `IntPermutation` instance, a fast repeatable integer permutation
 * for shuffling lists and data masking using a cycle-walking hash algorithm.
 *
 * Input Variables:
 *
 * [size] The size of the integer domain to permute. This also decides which
 * implementation is used, normally [HalfIntPermutation] but for negative values
 * [UIntPermutation] and for -1 [FullIntPermutation].
 *
 * [seed] Used as seed to [Random] to initialize keys and parameters.
 *
 * [rounds] The number of rounds in the permutation algorithm. Use 8 for high
 * dispersion requirements and minimum 3 for low requirements.
 */
@JvmOverloads
fun intPermutation(
    size: Int = UInt.MAX_VALUE.toInt(),
    seed: Long,
    rounds: Int = 0
) = intPermutation(size.toUInt(), Random(seed), rounds)

/**
 * Provides an `IntPermutation` instance, a fast repeatable integer permutation
 * for shuffling lists and data masking using a cycle-walking hash algorithm.
 *
 * Input Variables:
 *
 * [size] The size of the integer domain to permute. This also decides which
 * implementation is used, normally [HalfIntPermutation] but for negative values
 * [UIntPermutation] and for [UInt.MAX_VALUE] [FullIntPermutation].
 *
 * [rng] A random number generator used to initialize keys and parameters.
 *
 * [rounds] The number of rounds in the permutation algorithm. Use 8 for high
 * dispersion requirements and minimum 3 for low requirements.
 */
fun intPermutation(
    size: UInt = UInt.MAX_VALUE,
    rng: Random = Random.Default,
    rounds: Int = 0
): IntPermutation {
    require(rounds >= 0)
    return when {
        size <= 16u -> ArrayIntPermutation(size.toInt(), rng)
        size == UInt.MAX_VALUE ->
            FullIntPermutation(
                rng,
                if (rounds == 0) 1 else rounds
            )

        size.toInt() < 0 ->
            UIntPermutation(
                size.toInt(),
                rng,
                if (rounds == 0) 3 else rounds
            )

        else -> {
            HalfIntPermutation(
                size.toInt(),
                rng,
                if (rounds == 0) 3 else rounds
            )
        }
    }
}

/**
 * Provides an `IntPermutation` instance for values within the given [range].
 * The implementation and parameters follow the same rules as the size-based
 * factories, normally using [HalfIntPermutation] for most domains.
 *
 * Input Variables:
 *
 * [range] The inclusive integer range to permute. Its length determines the
 * domain size used internally.
 *
 * [rng]  A random number generator used to initialize keys and parameters.
 *
 * [rounds] The number of permutation rounds. Use 8 for high dispersion and at
 * least 3 for low requirements.
 *
 * The resulting permutation encodes and decodes values directly in [range].
 */
@JvmOverloads
fun intPermutation(
    range: IntRange,
    rng: Random = Random.Default,
    rounds: Int = 0
): IntPermutation =
    intPermutation(range.count(), rng, rounds).range(range)

/**
 * Provides an `IntPermutation` instance for values within the given [range].
 * The implementation and parameters follow the same rules as the size-based
 * factories, normally using [HalfIntPermutation] for most domains.
 *
 * Input Variables:
 *
 * [range] The inclusive integer range to permute. Its length determines the
 * domain size used internally.
 *
 * [rng]  A random number generator used to initialize keys and parameters.
 *
 * [rounds] The number of permutation rounds. Use 8 for high dispersion and at
 * least 3 for low requirements.
 *
 * The resulting permutation encodes and decodes values directly in [range].
 */
@JvmOverloads
fun intPermutation(
    range: IntRange,
    seed: Long,
    rounds: Int = 0
): IntPermutation =
    intPermutation(range.count(), seed, rounds).range(range)
