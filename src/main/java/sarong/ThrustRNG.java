package sarong;

import sarong.util.StringKit;

/**
 * A hybrid of the type of algorithm LightRNG uses with some of the specific steps of a linear congruential generator.
 * This RandomnessSource has no failures or even anomalies when tested with PractRand (even LightRNG has anomalies),
 * allows all longs as states (including 0), implements StatefulRandomness, and is measurably faster than LightRNG at
 * generating both ints and longs. This is very similar in capabilities to LightRNG because the algorithm is similar,
 * with both able to skip forward and backward about as quickly as they can generate numbers normally. ThrustRNG should
 * be a good general-purpose substitute for or complement to LightRNG. The period for a ThrustRNG should be 2 to the 64,
 * because it is based on the same concept LightRNG uses, where it increments its state by an odd number and uses a very
 * different permutation of the state as its returned random result. It only repeats a cycle of numbers after the state
 * has wrapped around the modulus for long addition enough times to come back to the original starting state, which
 * should take exactly 2 to the 64 generated numbers. The main weakness ThrustRNG has compared to LightRNG is that it is
 * sensitive to the increment used to change the state, so ThrustRNG uses a fixed size for the changes it makes to the
 * state (adding {@code 0x9E3779B97F4A7C15L} if going forward with the normal RandomnessSource methods, or adding or
 * subtracting a multiple of that if using {@link #skip(long)} to go forwards or backwards by some amount of steps).
 * Because the SplitMix64 algorithm that LightRNG uses goes through more steps to randomize the state, it can use any
 * odd increment, but this also makes it somewhat slower, and LightRNG doesn't use other increments anyway (but, Java 8
 * provides the random number generator SplittableRandom, which uses SplitMix64 and does use different increments).
 * <br>
 * The speed of this generator is fairly good, and it is the fastest generator to pass PractRand with no anomalies, and
 * remains faster than all generators without failures in PractRand. LapRNG, FlapRNG (when FlapRNG produces ints), and
 * (narrowly) ThunderRNG are faster, but all have significant amounts of PractRand testing failures, indicating flaws in
 * quality. The performance of this RandomnessSource has been surprisingly reasonable to improve beyond the baseline of
 * SplitMix64; where LightRNG takes 1.385 seconds to generate a billion pseudo-random long values, this takes just under
 * a second (0.958 seconds, to be exact) to generate the same quantity. This speed will vary depending on hardware, and
 * was benchmarked using JMH on a relatively-recent laptop (with a i7-6700HQ processor and DDR4 RAM, using a Zulu build
 * of OpenJDK 8); you can expect better performance on most desktops or dedicated "gaming PCs," or potentially much
 * slower speeds on Android or especially GWT (still, while GWT's emulation of the long data type is not fast, this
 * generator should yield the same results on GWT as on desktop or Android if the seed given is the same).
 * <br>
 * Thanks to Ashiren, for advice on this in #libgdx on Freenode, and to Pierre L'Ecuyer and Donald Knuth for finding the
 * constants used (originally for linear congruential generators).
 * Created by Tommy Ettinger on 8/3/2017.
 */
public class ThrustRNG implements StatefulRandomness {
    /**
     * Can be any long value.
     */
    public long state;

    /**
     * Creates a new generator seeded using Math.random.
     */
    public ThrustRNG() {
        this((long) ((Math.random() * 2.0 - 1.0) * 0x8000000000000L)
                ^ (long) ((Math.random() * 2.0 - 1.0) * 0x8000000000000000L));
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
        //return (int)(((state = state * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL) + (state >> 28)) >>> (64 - bits));
        long z = (state += 0x9E3779B97F4A7C15L);
        z = (z ^ z >>> 30) * 0x5851F42D4C957F2DL;
        return (int)(z ^ z >>> 28) >>> (32 - bits);
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
        long z = (state += 0x9E3779B97F4A7C15L);
        z = (z ^ z >>> 30) * 0x5851F42D4C957F2DL;// + 0x632BE59BD9B4E019L;
        return z ^ z >>> 28;
        // * 0x27BB2EE687B0B0FDL;
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
        long z = (state += 0x9E3779B97F4A7C15L * advance);
        z = (z ^ z >>> 30) * 0x5851F42D4C957F2DL;
        return z ^ z >>> 28;
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
     * {@code 0x9E3779B97F4A7C15L} within this method, so using a small increment won't be much different from using a
     * very large one, as long as it is odd.
     * @param state a variable that should be different every time you want a different random result;
     *              using {@code determine(++state)} is recommended to go forwards or {@code determine(--state)} to
     *              generate numbers in reverse order
     * @return a pseudo-random permutation of state
     */
    public static long determine(long state)
    {
        state = ((state *= 0x9E3779B97F4A7C15L) ^ state >>> 30) * 0x5851F42D4C957F2DL;
        return state ^ state >>> 28;
    }

    /**
     * Given a state that should usually change each time this is called, and a bound that limits the result to some
     * (typically fairly small) int, produces a pseudo-random int between 0 and bound (exclusive). The bound can be
     * negative, which will cause this to produce 0 or a negative int; otherwise this produces 0 or a positive int.
     * The state should change each time this is called, generally by incrementing by an odd number (not an even number,
     * especially not 0). It's fine to use {@code determineBounded(++state, bound)} to get a different int each time.
     * @param state a variable that should be different every time you want a different random result;
     *              using {@code randomize(++state)} is recommended to go forwards or {@code randomize(--state)} to
     *              generate numbers in reverse order
     * @param bound the outer exclusive bound for the int this produces; can be negative or positive
     * @return a pseudo-random int between 0 (inclusive) and bound (exclusive)
     */
    public static int determineBounded(long state, final int bound)
    {
        state = ((state *= 0x9E3779B97F4A7C15L) ^ state >>> 30) * 0x5851F42D4C957F2DL;
        return (int)((bound * ((state ^ state >>> 28) & 0x7FFFFFFFL)) >> 31);
    }

}
