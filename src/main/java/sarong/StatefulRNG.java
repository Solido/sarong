package sarong;

import sarong.util.CrossHash;

import java.io.Serializable;

/**
 * A slight variant on RNG that always uses a stateful RandomessSource and so can have its state
 * set or retrieved using setState() or getState().
 * Created by Tommy Ettinger on 9/15/2015.
 * @author Tommy Ettinger
 */
public class StatefulRNG extends RNG implements Serializable, IStatefulRNG {

    private static final long serialVersionUID = -2456306898212937163L;

    public StatefulRNG() {
        super();
    }

    public StatefulRNG(RandomnessSource random) {
        super((random instanceof StatefulRandomness) ? random : new DiverRNG(random.nextLong()));
    }

    /**
     * Seeded constructor uses DiverRNG, which is of high quality, but low period (which rarely matters for games),
     * and has good speed and tiny state size.
     * <br>
     * Note: This constructor changed behavior on April 20, 2019, as part of the move to DiverRNG instead of
     * ThrustAltRNG.
     */
    public StatefulRNG(long seed) {
        this(new DiverRNG(seed));
    }
    /**
     * String-seeded constructor uses the hash of the String as a seed for DiverRNG, which is of high quality, but
     * low period (which rarely matters for games), and has good speed and tiny state size.
     * <br>
     * Note: This constructor changed behavior on April 20, 2019, as part of the move to DiverRNG instead of
     * ThrustAltRNG.
     */
    public StatefulRNG(CharSequence seedString) {
        this(new DiverRNG(CrossHash.hash64(seedString)));
    }

    @Override
    public void setRandomness(RandomnessSource random) {
        super.setRandomness(random == null ? new DiverRNG() :
                (random instanceof StatefulRandomness) ? random : new DiverRNG(random.nextLong()));
    }

    /**
     * Creates a copy of this StatefulRNG; it will generate the same random numbers, given the same calls in order, as
     * this StatefulRNG at the point copy() is called. The copy will not share references with this StatefulRNG.
     *
     * @return a copy of this StatefulRNG
     */
    @Override
    public StatefulRNG copy() {
        return new StatefulRNG(random.copy());
    }

    /**
     * Get a long that can be used to reproduce the sequence of random numbers this object will generate starting now.
     * @return a long that can be used as state.
     */
    public long getState()
    {
        return ((StatefulRandomness)random).getState();
    }

    /**
     * Sets the state of the random number generator to a given long, which will alter future random numbers this
     * produces based on the state.
     * @param state a long, which typically should not be 0 (some implementations may tolerate a state of 0, however).
     */
    public void setState(long state)
    {
        ((StatefulRandomness)random).setState(state);
    }

    @Override
    public String toString() {
        return "StatefulRNG{" + Long.toHexString(((StatefulRandomness)random).getState()) + "}";
    }
    /**
     * Returns this StatefulRNG in a way that can be deserialized even if only {@link IRNG}'s methods can be called.
     * @return a {@link Serializable} view of this StatefulRNG; always {@code this}
     */
    @Override
    public Serializable toSerializable() {
        return this;
    }

}
