package sarong;

import sarong.util.StringKit;

import java.io.Serializable;

/**
 * A variant on {@link ThrustRNG} that gives up a small amount of speed to attain better quality. ThrustRNG can pass
 * BigCrush, which is a difficult statistical quality test that is part of TestU01, but fails a different statistical
 * quality test, PractRand, when more data is requested than had been used to test Thrust originally (it tends to fail
 * at 32 GB of data, almost always on a test called "Gap-16" that isn't part of BigCrush). This generator passes
 * PractRand and should pass BigCrush, though that hasn't yet been checked for certain. Unlike ThrustRNG, this requires
 * its state variable to be an odd number, which reduces its period to 2 to the 63 (instead of 2 to the 64).
 * Interestingly, the C version of this generator is faster than ThrustRNG's version and has better quality as well. It
 * may be possible to adjust some qualities of this generator to get more of the optimizations used by C compilers.
 * <br>
 * Created by Tommy Ettinger on 10/18/2017.
 */
public final class ThrustAltRNG implements StatefulRandomness, Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * Can be any odd long value (least significant bit must be 1; set that bit with {@code state |= 1L}).
     */
    private long state;

    /**
     * Creates a new generator seeded using Math.random.
     */
    public ThrustAltRNG() {
        this((long) ((Math.random() * 2.0 - 1.0) * 0x8000000000000L)
                ^ (long) ((Math.random() * 2.0 - 1.0) * 0x8000000000000000L));
    }

    public ThrustAltRNG(final long seed) {
        state = seed | 1L;
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
     * Set the current internal state of this StatefulRandomness with a long. Only uses the most significant 63 bits of
     * state, and sets the least significant bit to 1, always.
     *
     * @param state a 64-bit long; the least significant bit will always be set to 1 (making it an odd number)
     */
    @Override
    public void setState(long state) {
        this.state = state | 1L;
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
        //final long z = (state ^ state >> 26) * ((state += 0x6A5D39EAE116586AL));
        final long s = state;
        final long z = (s ^ s >> 26) * (state += 0x6A5D39EAE116586AL);
        return (int)(z ^ (z >> 28)) >> (32 - bits);
    }
/*
    public final int next(int bits) {
        //return (int)(((state = state * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL) + (state >> 28)) >>> (64 - bits));

        final long s = (state += 0xC6BC279692B5CC83L);
        final long z = (s ^ (s >> 26)) * 0x2545F4914F6CDD1DL;
        return (int)((s | 0x5851F42D4C957F2DL) * (z >> 28) + z) >>> (32 - bits);
        //return (int)(((z >> 28) ^ z) * ((state += 0x9E3779B97F4A7C15L) | 1L)) >>> (32 - bits);

        //long z = (state += 0x9E3779B97F4A7C15L);
        //z = (z ^ z >>> 26) * 0x2545F4914F6CDD1DL;
        //return (int)(z ^ z >>> 28) >>> (32 - bits);

        //(state = state * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL) + (state >> 28)

        //(state *= 0x2545F4914F6CDD1DL) + (state >> 28)
        //((state += 0x2545F4914F6CDD1DL) ^ (state >>> 30 & state >> 27) * 0xBF58476D1CE4E5B9L)
        //(state ^ (state += 0x2545F4914F6CDD1DL)) * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL
    }
    */
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
        //final long z = (state ^ state >> 26) * ((state += 0x6A5D39EAE116586AL));
        final long s = state;
        final long z = (s ^ s >> 26) * (state += 0x6A5D39EAE116586AL);
        return z ^ (z >> 28);
    }
    /*
    public final long nextLong() {

        final long s = (state += 0xC6BC279692B5CC83L);
        final long z = (s ^ (s >> 26)) * 0x2545F4914F6CDD1DL;
        return (s | 0x5851F42D4C957F2DL) * (z >> 28) + z;
        //final long z = (s ^ s >> 26) * (s | 1L);
        //return z ^ ((z >> 28) + (state += 0xC6BC279692B5CC83L));

        //final long z = (s ^ (s >> 26)) * 0x2545F4914F6CDD1DL;
        //return ((z >> 28) ^ z) * (s | 1L);
        //long z = (state += 0x9E3779B97F4A7C15L);
        //z = (z ^ z >>> 26) * 0x2545F4914F6CDD1DL;
        //return z ^ z >>> 28;

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
    */
    /*
    public final long nextLong2() {
        final long s = (state += 0xC6BC279692B5CC83L);
        final long z = (s ^ s >> 26) * (s | 1L);
        return z ^ ((z >> 28) + s);
    }
    public final int next2(final int bits) {
        final long s = (state += 0xC6BC279692B5CC83L);
        final long z = (s ^ s >> 26) * (s | 1L);
        return (int)(z ^ ((z >> 28) + s)) >> (32 - bits);
    }
    public final long nextLong3() {
        final long s = (state += 0xC6BC279692B5CC83L);
        final long z = (s ^ (s >> 26)) * 0x2545F4914F6CDD1DL;
        return (s | 0x5851F42D4C957F2DL) * (z >> 28) + z;
    }
    public final int next3(final int bits) {
        final long s = (state += 0xC6BC279692B5CC83L);
        final long z = (s ^ (s >> 26)) * 0x2545F4914F6CDD1DL;
        return (int)((s | 0x5851F42D4C957F2DL) * (z >> 28) + z) >> (32 - bits);
    }
    public final long nextLong4() {
        final long s = state;
        final long z = (s ^ s >> 26) * (state += 0x6A5D39EAE116586AL);
        return z ^ (z >> 28);
    }
    public final int next4(final int bits) {
        final long s = state;
        final long z = (s ^ s >> 26) * (state += 0x6A5D39EAE116586AL);
        final long z = (s ^ s >> 26) * (state += 0x6A5D39EAE116586AL);
        return (int)(z ^ (z >> 28)) >> (32 - bits);
    }
    */
    /**
     * Advances or rolls back the ThrustRNG's state without actually generating each number. Skips forward
     * or backward a number of steps specified by advance, where a step is equal to one call to nextLong(),
     * and returns the random number produced at that step (you can get the state with {@link #getState()}).
     *
     * @param advance Number of future generations to skip over; can be negative to backtrack, 0 gets the most-recently-generated number
     * @return the random long generated after skipping forward or backwards by {@code advance} numbers
     */
    public final long skip(long advance) {
        final long s = state + 0x6A5D39EAE116586AL * (advance - 1L);
        final long z = (s ^ s >> 26) * (state += 0x6A5D39EAE116586AL * advance);
        return z ^ (z >> 28);
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
        return (int) (state >>> 1 ^ (state >>> 32));
    }

    /**
     * Returns a random permutation of state; if state is the same on two calls to this, this will return the same
     * number. This is expected to be called with some changing variable, e.g. {@code determine(++state)}, where
     * the increment for state should be odd but otherwise doesn't really matter. This multiplies state by
     * {@code 0x6A5D39EAE116586BL} within this method, so using a small increment won't be much different from using a
     * very large one, as long as it is odd. Notably, this allows both even and odd numbers for state, which the
     * normal parts of the generator do not, and the period is 2 to the 64.
     * @param state a variable that should be different every time you want a different random result;
     *              using {@code determine(++state)} is recommended to go forwards or {@code determine(--state)} to
     *              generate numbers in reverse order
     * @return a pseudo-random permutation of state
     */
    public static long determine(long state)
    {
        final long z = ((state *= 0x6A5D39EAE116586BL) | 0x6A5D39EAE1165865L) * (state ^ state >> 26);
        return z ^ (z >> 28);
    }

    /**
     * Returns a random permutation of state; if state is the same on two calls to this, this will return the same
     * number. This is expected to be called with a changing variable using a specific pattern, namely
     * {@code determine(state += 0x6A5D39EAE116586BL)}, but other odd-number increments may work (they may work better,
     * actually). You can add the given number to go forwards or subtract it to go backwards in the sequence. Notably,
     * this allows both even and odd numbers for state, which the normal parts of the generator do not, and the period
     * is 2 to the 64. This method may rarely be preferable over {@link #determine(long)} if your code controls how
     * state is updated, and can make sure it updates by the given amount or a similar value; this version saves a small
     * amount of time by not needing to multiply the local state.
     * @param state a variable that should be different every time you want a different random result;
     *              using {@code determine(state += 0x6A5D39EAE116586BL)} is recommended to go forwards or
     *              {@code determine(state -= 0x6A5D39EAE116586BL)} to generate numbers in reverse order
     * @return a pseudo-random permutation of state
     */
    public static long determineBare(long state)
    {
        final long z = (state | 0x6A5D39EAE1165865L) * (state ^ state >> 26);
        return z ^ (z >> 28);
    }

    /**
     * Given a state that should usually change each time this is called, and a bound that limits the result to some
     * (typically fairly small) int, produces a pseudo-random int between 0 and bound (exclusive). The bound can be
     * negative, which will cause this to produce 0 or a negative int; otherwise this produces 0 or a positive int.
     * The state should change each time this is called, generally by incrementing by an odd number (not an even number,
     * especially not 0). It's fine to use {@code determineBounded(++state, bound)} to get a different int each time.
     * Notably, this allows both even and odd numbers for state, which the normal parts of the generator do not, but the
     * period is still 2 to the 63 (or less, depending on bound).
     * @param state a variable that should be different every time you want a different random result;
     *              using {@code determineBounded(++state, bound)} is recommended to go forwards or
     *              {@code determineBounded(--state, bound)} to generate numbers in reverse order
     * @param bound the outer exclusive bound for the int this produces; can be negative or positive
     * @return a pseudo-random int between 0 (inclusive) and bound (exclusive)
     */
    public static int determineBounded(long state, final int bound)
    {
        final long z = ((state *= 0x6A5D39EAE116586BL) | 0x6A5D39EAE1165865L) * (state ^ state >> 26);
        return (int)((bound * ((z ^ (z >> 28)) & 0x7FFFFFFFL)) >> 31);
    }

}
