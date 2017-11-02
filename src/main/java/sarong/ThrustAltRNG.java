package sarong;

import sarong.util.StringKit;

import java.io.Serializable;

/**
 * A variant on {@link ThrustRNG} that gives up a small amount of speed to attain better quality. ThrustAltRNG is
 * expected to pass BigCrush, which is a difficult statistical quality test that is part of TestU01, because it does so
 * well on other statistical tests. On <a href="http://gjrand.sourceforge.net/">gjrand</a>'s "testunif" checks, this
 * does very well on 100GB of tested data, with the "Overall summary one sided P-value P = 0.79", where 1 is perfect
 * and 0.1 or less is a failure. On <a href="http://pracrand.sourceforge.net/">PractRand</a>, this runs past 1TB of
 * generated numbers without finding any failures, and this version avoids issues with Gap-16 tests that cause ThrustRNG
 * to fail at 32GB and can cause slight variations on the code here to fail at 256GB. Like ThrustRNG and LightRNG, this
 * changes its state with a steady fixed increment, and does cipher-like adjustments to the current state to randomize
 * it. The period on ThrustAltRNG is 2 to the 64. ThrustAltRNG is a bit slower than ThrustRNG (but seems to have better
 * quality), while it is faster than LightRNG, XoRoRNG, and most other very-high-quality generators (not counting
 * ThrustRNG). Similarly to other cipher-like PRNGs, ThrustAltRNG has a {@link #determine(long)} method that takes a
 * state as a long and returns a deterministic random number (each input has one output). Unlike some generators (like
 * PermutedRNG), changing the seed even slightly generally produces completely different results, which applies
 * primarily to determine() but also the first number generated in a series of nextLong() calls.
 * <br>
 * As an aside, this generator has probably been adjusted more by me than any other generator in the library, and if
 * quality is only somewhat more important than speed, this should be ideal, though it isn't the fastest and doesn't
 * have the highest quality. For some purposes, ThrustRNG will be better because it is faster. For others, XoRoRNG or
 * IsaacRNG may be better because they may have more-provably-good quality. Note than XoRoRNG fails binary rank tests,
 * which may be important for some usage, while LightRNG and ThrustAltRNG don't. ThrustAltRNG actually does better than
 * LightRNG on gjrand's tests despite LightRNG using significantly more operations (LightRNG has "P = 0.305" on gjrand
 * with 100GB tested and fails 2 of 13 tests with grade 1 failures, while ThrustAltRNG fails none). IsaacRNG is also
 * substantially slower than most other generators, though it offers better promise of security.
 * <br>
 * This generator has changed since its introduction; the initial version used both the current and subsequent states
 * during each calculation, while this version only uses the current state (which it updates as it reads it). This makes
 * the {@link #skip(long)} method much simpler and (because it requires less operations in general) probably faster as
 * well, while it seems to have no performance impact on the normal {@link #nextLong()} and {@link #next(int)} methods.
 * Quality is still very high, though not as stellar as in the first version according to gjrand (the earlier one had a
 * P-value of 0.904, while this has 0.79), but there's more potential for this to go even further on PractRand tests
 * than the previous one, which was unlikely to do well beyond 2TB. Since the current algorithm is a 64-bit variant on
 * Jab63RNG's algorithm (even using very similar constants), it seems probable that this will share Jab32RNG's high
 * PractRand results (that generator passes 4TB and seems able to keep going).
 * <br>
 * Created by Tommy Ettinger on 10/18/2017.
 */
public final class ThrustAltRNG implements StatefulRandomness, Serializable {
    private static final long serialVersionUID = 3L;
    /**
     * Can be any long value.
     */
    public long state;

    /**
     * Creates a new generator seeded using Math.random.
     */
    public ThrustAltRNG() {
        this((long) ((Math.random() - 0.5) * 0x10000000000000L)
                ^ (long) (((Math.random() - 0.5) * 2.0) * 0x8000000000000000L));
    }

    public ThrustAltRNG(final long seed) {
        state = seed;
    }

    /**
     * Get the current internal state of the StatefulRandomness as a long.
     *
     * @return the current internal state of this object.
     */
    @Override
    public long getState() {
        return state;
    }

    /**
     * Set the current internal state of this StatefulRandomness with a long.
     *
     * @param state a 64-bit long
     */
    @Override
    public void setState(long state) {
        this.state = state;
    }

    /**
     * Using this method, any algorithm that might use the built-in Java Random
     * can interface with this randomness source.
     *
     * @param bits the number of bits to be returned
     * @return the integer containing the appropriate number of bits
     */
    @Override
    public final int next(final int bits) {
        final long s = (state += 0x6A5D39EAE12657A9L);
        final long z = (s ^ (s >>> 26)) * (s | 3L);
        return (int)(z ^ (z >>> 22)) >>> (32 - bits);
    }
    /**
     * Using this method, any algorithm that needs to efficiently generate more
     * than 32 bits of random data can interface with this randomness source.
     * <p>
     * Get a random long between Long.MIN_VALUE and Long.MAX_VALUE (both inclusive).
     *
     * @return a random long between Long.MIN_VALUE and Long.MAX_VALUE (both inclusive)
     */
    @Override
    public final long nextLong() {
        final long s = (state += 0x6A5D39EAE12657A9L);
        final long z = (s ^ (s >>> 26)) * (s | 3L);
        return z ^ (z >>> 22);
    }

    /**
     * Advances or rolls back the ThrustAltRNG's state without actually generating each number. Skips forward
     * or backward a number of steps specified by advance, where a step is equal to one call to nextLong(),
     * and returns the random number produced at that step (you can get the state with {@link #getState()}).
     *
     * @param advance Number of future generations to skip over; can be negative to backtrack, 0 gets the most-recently-generated number
     * @return the random long generated after skipping forward or backwards by {@code advance} numbers
     */
    public final long skip(long advance) {
        final long s = (state += 0x6A5D39EAE12657A9L * advance);
        final long z = (s ^ (s >>> 26)) * (s | 3L);
        return z ^ (z >>> 22);
    }


    /**
     * Produces a copy of this RandomnessSource that, if next() and/or nextLong() are called on this object and the
     * copy, both will generate the same sequence of random numbers from the point copy() was called. This just need to
     * copy the state so it isn't shared, usually, and produce a new value with the same exact state.
     *
     * @return a copy of this RandomnessSource
     */
    @Override
    public RandomnessSource copy() {
        return new ThrustAltRNG(state);
    }
    @Override
    public String toString() {
        return "ThrustAltRNG with state 0x" + StringKit.hex(state) + 'L';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ThrustAltRNG thrustAltRNG = (ThrustAltRNG) o;

        return state == thrustAltRNG.state;
    }

    @Override
    public int hashCode() {
        return (int) (state ^ (state >>> 32));
    }

    /**
     * Returns a random permutation of state; if state is the same on two calls to this, this will return the same
     * number. This is expected to be called with some changing variable, e.g. {@code determine(++state)}, where
     * the increment for state should be odd but otherwise doesn't really matter. This multiplies state by
     * {@code 0x6A5D39EAE12657A9L} within this method, so using a small increment won't be much different from using a
     * very large one, as long as it is odd. The period is 2 to the 64.
     * @param state a variable that should be different every time you want a different random result;
     *              using {@code determine(++state)} is recommended to go forwards or {@code determine(--state)} to
     *              generate numbers in reverse order
     * @return a pseudo-random permutation of state
     */
    public static long determine(long state) {
        state = ((state *= 0x6A5D39EAE12657A9L) ^ (state >>> 26)) * (state | 3L);
        return state ^ (state >>> 22);
    }
    //for quick one-line pastes of how the algo can be used with "randomize(++state)"
    //public static long randomize(long state) { state = ((state *= 0x6A5D39EAE12657A9L) ^ (state >>> 26)) * (state | 3L); return z ^ (z >>> 22); }

    /**
     * Returns a random permutation of state; if state is the same on two calls to this, this will return the same
     * number. This is expected to be called with a changing variable using a specific pattern, namely
     * {@code determine(state += 0x6A5D39EAE12657A9L)}, which was specifically matched up to the rest of the generator.
     * You can add the given number to go forwards or subtract it to go backwards in the sequence. The period
     * is 2 to the 64. This method may rarely be preferable over {@link #determine(long)} if your code controls how
     * state is updated, and can make sure it updates by the given amount or a similar value; this version saves a small
     * amount of time by not needing to multiply the local state.
     * @param state a variable that should be different every time you want a different random result;
     *              using {@code determine(state += 0x6A5D39EAE12657A9L)} is recommended to go forwards or
     *              {@code determine(state -= 0x6A5D39EAE12657A9L)} to generate numbers in reverse order
     * @return a pseudo-random permutation of state
     */
    public static long determineBare(long state)
    {
        state = (state ^ (state >>> 26)) * (state | 3L);
        return state ^ (state >>> 22);
    }

    /**
     * Given a state that should usually change each time this is called, and a bound that limits the result to some
     * (typically fairly small) int, produces a pseudo-random int between 0 and bound (exclusive). The bound can be
     * negative, which will cause this to produce 0 or a negative int; otherwise this produces 0 or a positive int.
     * The state should change each time this is called, generally by incrementing by an odd number (not an even number,
     * especially not 0). It's fine to use {@code determineBounded(++state, bound)} to get a different int each time.
     * The period is usually 2 to the 64, but some bounds may reduce the period (in the extreme case, a bound of 1 would
     * force only 0 to be generated, so that would make the period 1).
     * @param state a variable that should be different every time you want a different random result;
     *              using {@code determineBounded(++state, bound)} is recommended to go forwards or
     *              {@code determineBounded(--state, bound)} to generate numbers in reverse order
     * @param bound the outer exclusive bound for the int this produces; can be negative or positive
     * @return a pseudo-random int between 0 (inclusive) and bound (exclusive)
     */
    public static int determineBounded(long state, final int bound)
    {
        state = ((state *= 0x6A5D39EAE12657A9L) ^ (state >>> 26)) * (state | 3L);
        return (int)((bound * ((state ^ (state >>> 22)) & 0xFFFFFFFFL)) >> 32);
    }
}
