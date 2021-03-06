package sarong.discouraged;

import sarong.SkippingRandomness;
import sarong.StatefulRandomness;
import sarong.ThrustAltRNG;
import sarong.util.StringKit;

import java.io.Serializable;

/**
 * Currently a work in progress; do not use for important tasks. This generator is very, very fast, however. It is known
 * to pass at least 4TB of PractRand testing; an earlier version couldn't pass 32GB. The generation is based on a unary
 * hash (this is reversible) called on a counter, which can be optimized to an astounding extent when it is used in a
 * tight loop. Speed is comparable to {@link ThrustAltRNG}, but this is reversible and equidistributed, while
 * ThrustAltRNG certainly is not.
 * <br>
 * Thanks to Ashiren, for advice on this in #libgdx on Freenode, and to Pierre L'Ecuyer and Donald Knuth for finding the
 * constants used (originally for linear congruential generators).
 * Created by Tommy Ettinger on 8/3/2017.
 */
public final class ThrustRNG implements StatefulRandomness, SkippingRandomness, Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * Can be any long value.
     */
    public long state;

    /**
     * Creates a new generator seeded using Math.random.
     */
    public ThrustRNG() {
        this((long) ((Math.random() - 0.5) * 0x10000000000000L)
                ^ (long) (((Math.random() - 0.5) * 2.0) * 0x8000000000000000L));
    }

    public ThrustRNG(final long seed) {
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
     * @param state a 64-bit long. You may want to avoid passing 0 for compatibility, though this implementation can handle that.
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
    public final int next(int bits) {
        final long s = (state += 0xC6BC279692B5C323L);
        final long z = (s ^ s >>> 31 ^ s >>> 15) * 0xE7037ED1A0B428DBL;
        return (int) ((z ^ z >>> 26) >>> 64 - bits);

        //return (int)(((state = state * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL) + (state >> 28)) >>> (64 - bits));
//        long z = (state += 0x9E3779B97F4A7C15L);
//        z = (z ^ z >>> 26) * 0x2545F4914F6CDD1DL;
//        return (int)(z ^ z >>> 28) >>> (32 - bits);
                //(state = state * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL) + (state >> 28)

                //(state *= 0x2545F4914F6CDD1DL) + (state >> 28)
                //((state += 0x2545F4914F6CDD1DL) ^ (state >>> 30 & state >> 27) * 0xBF58476D1CE4E5B9L)
                //(state ^ (state += 0x2545F4914F6CDD1DL)) * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL
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
        final long s = (state += 0xC6BC279692B5C323L);
        final long z = (s ^ s >>> 31 ^ s >>> 15) * 0xE7037ED1A0B428DBL;
        return z ^ z >>> 26;

//        long z = (state += 0x9E3779B97F4A7C15L);
//        z = (z ^ z >>> 26) * 0x2545F4914F6CDD1DL;
//        return z ^ z >>> 28;
        // the first multiplier that worked fairly well was 0x5851F42D4C957F2DL ; its source is unclear so I'm trying
        // other numbers with better evidence for their strength
        // the multiplier 0x6A5D39EAE116586DL did not work very well (L'Ecuyer, best found MCG constant for modulus
        // 2 to the 64 when generating 16 bits, but this left 16 bits of each long lower-quality)
        // the multiplier 0x2545F4914F6CDD1DL is also from L'Ecuyer and seems much better
        // * 0x27BB2EE687B0B0FDL; // ???
        //return ((state = state * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL) + (state >> 28));

        //return (state = state * 0x59A2B8F555F5828FL % 0x7FFFFFFFFFFFFFE7L) ^ state << 2;
        //return (state = state * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL);
        //return (state ^ (state += 0x2545F4914F6CDD1DL)) * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL;
        //return (state * 0x5851F42D4C957F2DL) + ((state += 0x14057B7EF767814FL) >> 28);
        //return (((state += 0x14057B7EF767814FL) >>> 28) * 0x5851F42D4C957F2DL + (state >>> 1));
    }

    /**
     * Advances or rolls back the ThrustRNG's state without actually generating each number. Skips forward
     * or backward a number of steps specified by advance, where a step is equal to one call to nextLong(),
     * and returns the random number produced at that step (you can get the state with {@link #getState()}).
     *
     * @param advance Number of future generations to skip over; can be negative to backtrack, 0 gets the most-recently-generated number
     * @return the random long generated after skipping forward or backwards by {@code advance} numbers
     */
    public final long skip(long advance) {
        final long s = (state += 0xC6BC279692B5C323L * advance);
        final long z = (s ^ s >>> 31 ^ s >>> 15) * 0xE7037ED1A0B428DBL;
        return z ^ z >>> 26;

//        long z = (state += 0x9E3779B97F4A7C15L * advance);
//        z = (z ^ z >>> 26) * 0x2545F4914F6CDD1DL;
//        return z ^ z >>> 28;
    }


    /**
     * Produces a copy of this RandomnessSource that, if next() and/or nextLong() are called on this object and the
     * copy, both will generate the same sequence of random numbers from the point copy() was called. This just need to
     * copy the state so it isn't shared, usually, and produce a new value with the same exact state.
     *
     * @return a copy of this RandomnessSource
     */
    @Override
    public ThrustRNG copy() {
        return new ThrustRNG(state);
    }
    @Override
    public String toString() {
        return "ThrustRNG with state 0x" + StringKit.hex(state) + 'L';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ThrustRNG thrustRNG = (ThrustRNG) o;

        return state == thrustRNG.state;
    }

    @Override
    public int hashCode() {
        return (int) (state ^ (state >>> 32));
    }

    /**
     * Returns a random permutation of state; if state is the same on two calls to this, this will return the same
     * number. This is expected to be called with some changing variable, e.g. {@code determine(++state)}, where
     * the increment for state should be odd but otherwise doesn't really matter. This multiplies state by
     * {@code 0xC6BC279692B5C323L} within this method, so using a small increment won't be much different from using a
     * very large one, as long as it is odd.
     * @param state a variable that should be different every time you want a different random result;
     *              using {@code determine(++state)} is recommended to go forwards or {@code determine(--state)} to
     *              generate numbers in reverse order
     * @return a pseudo-random permutation of state
     */
    public static long determine(long state) {
        return (state = ((state *= 0xC6BC279692B5C323L) ^ state >>> 31 ^ state >>> 15) * 0xE7037ED1A0B428DBL) ^ state >>> 26;
    }
//        state = ((state *= 0x9E3779B97F4A7C15L) ^ state >>> 26) * 0x2545F4914F6CDD1DL;
//        return state ^ state >>> 28;

    /**
     * Given a state that should usually change each time this is called, and a bound that limits the result to some
     * (typically fairly small) int, produces a pseudo-random int between 0 and bound (exclusive). The bound can be
     * negative, which will cause this to produce 0 or a negative int; otherwise this produces 0 or a positive int.
     * The state should change each time this is called, generally by incrementing by an odd number (not an even number,
     * especially not 0). It's fine to use {@code determineBounded(++state, bound)} to get a different int each time.
     * @param state a variable that should be different every time you want a different random result;
     *              using {@code determineBounded(++state, bound)} is recommended to go forwards or
     *              {@code determineBounded(--state, bound)} to generate numbers in reverse order
     * @param bound the outer exclusive bound for the int this produces; can be negative or positive
     * @return a pseudo-random int between 0 (inclusive) and bound (exclusive)
     */
    public static int determineBounded(long state, final int bound)
    { 
        return (int)((bound * ((state = ((state *= 0xC6BC279692B5C323L) ^ state >>> 31 ^ state >>> 15) * 0xE7037ED1A0B428DBL) ^ state >>> 26 & 0xFFFFFFFFL)) >> 32);
//        state = ((state *= 0x9E3779B97F4A7C15L) ^ state >>> 26) * 0x2545F4914F6CDD1DL;
//        return (int)((bound * ((state ^ state >>> 28) & 0x7FFFFFFFL)) >> 31);
    }

}
