package sarong;

import sarong.util.CrossHash;
import sarong.util.StringKit;

import java.util.Arrays;

/**
 * A mix of fast 32-bit-friendly RNGs like FlapRNG with the larger state size of LongPeriodRNG, in the hopes of
 * improving Flap's period without seriously reducing speed. It sorta works, since {@link #nextInt()} is faster than
 * {@link LongPeriodRNG#nextLong()} by a fair amount, but only "sorta" because methods like {@link #next(int)} slow
 * down when they are called by classes like RNG. There's some behavior of the JVM at play here, and it may be different
 * across machines and installations. This has 512 bits of {@link #state} in an int array with 16 elements, plus 32
 * bits of semi-state in the {@link #choice} field (used to decide which of the 16 ints in state to update and query).
 * The period is known to be not-terrible, and must be at least (2 to the 48) but is almost certainly much higher, since
 * testing a variant of this with significantly fewer bits of state (using 4 shorts instead of 16 ints, with the same
 * int for choice) still had a period greater than 2 to the 38, implying the period here may be greater than (2 to the
 * 256), and potentially as high as (2 to the 512), though this last possibility is very unlikely.
 * Created by Tommy Ettinger on 6/5/2017.
 */
public class HerdRNG implements RandomnessSource {
    public final int[] state = new int[16];
    public int choice = 0;
    public HerdRNG() {
        this((int)((Math.random() * 2.0 - 1.0) * 0x80000000));
    }

    public HerdRNG(final int seed) {
        setState(seed);
    }

    public HerdRNG(final int a, final int b, final int c, final int d,
                    final int e, final int f, final int g, final int h,
                    final int i, final int j, final int k, final int l,
                    final int m, final int n, final int o, final int p)
    {
        state[0]  = a;
        state[1]  = b;
        state[2]  = c;
        state[3]  = d;
        state[4]  = e;
        state[5]  = f;
        state[6]  = g;
        state[7]  = h;
        state[8]  = i;
        state[9]  = j;
        state[10] = k;
        state[11] = l;
        state[12] = m;
        state[13] = n;
        state[14] = o;
        state[15] = p;
        choice = a ^ ~p;
    }

    public HerdRNG(final int[] seed) {
        int len;
        if (seed == null || (len = seed.length) == 0) {
            for (int i = 0; i < 16; i++) {
                state[i] = PintRNG.determine(0x632D978F + i * 0x9E3779B9);
            }
            choice = state[0];
        } else if (len < 16) {
            for (int i = 0, s = 0; i < 16; i++, s++) {
                if(s == len) s = 0;
                state[i] ^= seed[s];
            }
            choice = state[0] * len;
        } else {
            for (int i = 0, s = 0; s < len; s++, i = (i + 1) & 15) {
                state[i] ^= seed[s];
            }
            choice = state[0] * len;
        }
    }

    public void setState(final int seed) {
        for (int i = 0; i < 16; i++) {
            state[i] = PintRNG.determine(seed + i * 0x9E3779B9);
        }
        choice = state[0] ^ seed;
    }

    public final long nextLong() {
        final int c = (choice += 0x9CBC278D);
        return (state[c & 15] += (state[c >>> 28] >>> 1) + 0x8E3779B9)
                * 0xC6AC279692B5CC53L ^ state[c >>> 26 & 15];
        // 0x632AE59B69B3C209L

        //        + high ^ (0x9E3779B97F4A7C15L * ((high += low & (low += 0xAB79B96DCD7FE75EL)) >> 20))); // thunder
        //        + ((low = (low >>> 1 ^ (-(low & 1L) & 0x6000000000000000L)))) // LFSR, 63-bit
        ///        ^ (high = high >>> 1 ^ (-(high & 1L) & 0xD800000000000000L))); // LFSR, 64-bit;
    }

    public final int nextInt() {
        final int c = (choice += 0x9CBC278D);
        return (state[c & 15] += (state[c >>> 28] >>> 1) + 0x8E3779B9);
        //0xBE377BB97F4A7C17L
        /*
        return (int) ((state1 += (state0 += 0x632AE59B69B3C209L) * 0x9E3779B97F4A7C15L)
                + (low = (low >>> 1 ^ (-(low & 1L) & 0x6000000000000000L))) // LFSR, 63-bit
                ^ (high = high >>> 1 ^ (-(high & 1L) & 0xD800000000000000L))); // LFSR, 64-bit;
        */
    }

    public final int next(final int bits) {
        final int c = (choice += 0x9CBC278D);
        return ((state[c & 15] += (state[c >>> 28] >>> 1) + 0x8E3779B9) >>> (32 - bits)); //0x9E3779B9
    }

    /**
     * Produces a copy of this RandomnessSource that, if next() and/or nextLong() are called on this object and the
     * copy, both will generate the same sequence of random numbers from the point copy() was called. This just needs to
     * copy the state so it isn't shared, usually, and produce a new value with the same exact state.
     *
     * @return a copy of this RandomnessSource
     */
    @Override
    public RandomnessSource copy() {
        HerdRNG hr = new HerdRNG(state);
        hr.choice = choice;
        return hr;
    }

    @Override
    public String toString() {
        return "HerdRNG{" +
                "state=" + StringKit.hex(state) +
                ", choice=" + choice +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HerdRNG herdRNG = (HerdRNG) o;

        return choice == herdRNG.choice && Arrays.equals(state, herdRNG.state);
    }

    @Override
    public int hashCode() {
        return 31 * choice + CrossHash.hash(state);
    }
}
