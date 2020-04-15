package sarong;

/**
 * Various numeric functions that are important to performance but need alternate implementations on GWT to obtain it.
 * Super-sourced on GWT, but most things here are direct calls to JDK methods when on desktop or Android.
 * Some of this code makes use of "creative" bit manipulation of floats and doubles, which can sometimes allow uncommon
 * input-to-output patterns (as in {@link #bounce(float)}), or even can yield a performance boost (compare
 * {@link #zigzag(float)} to using modulus to accomplish the same results). The bit manipulation has good performance on
 * GWT thanks to JS typed arrays, which are well-supported now across all recent browsers and have fallbacks in GWT in
 * the unlikely event of a browser not supporting them.
 */
public final class NumberTools {
    /**
     * Identical to {@link Double#doubleToLongBits(double)} on desktop; optimized on GWT. When compiling to JS via GWT,
     * there is no way to distinguish NaN values with different bits but that are still NaN, so this doesn't try to
     * somehow permit that. Uses JS typed arrays on GWT, which are well-supported now across all recent browsers and
     * have fallbacks in GWT in the unlikely event of a browser not supporting them. JS typed arrays support double, but
     * not long, so this needs to compose a long from two ints, which means the double-to/from-long conversions aren't
     * as fast as float-to/from-int conversions.
     * @param value a {@code double} floating-point number.
     * @return the bits that represent the floating-point number.
     */
    public static long doubleToLongBits(final double value)
    {
        return Double.doubleToLongBits(value);
    }
    /**
     * Identical to {@link Double#doubleToLongBits(double)} on desktop (note, not
     * {@link Double#doubleToRawLongBits(double)}); optimized on GWT. When compiling to JS via GWT, there is no way to
     * distinguish NaN values with different bits but that are still NaN, so this doesn't try to somehow permit that.
     * Uses JS typed arrays on GWT, which are well-supported now across all recent browsers and have fallbacks in GWT in
     * the unlikely event of a browser not supporting them. JS typed arrays support double, but not long, so this needs
     * to compose a long from two ints, which means the double-to/from-long conversions aren't as fast as
     * float-to/from-int conversions.
     * @param value a {@code double} floating-point number.
     * @return the bits that represent the floating-point number.
     */
    public static long doubleToRawLongBits(final double value)
    {
        return Double.doubleToLongBits(value);
    }

    /**
     * Identical to {@link Double#longBitsToDouble(long)} on desktop; optimized on GWT. Uses JS typed arrays on GWT,
     * which are well-supported now across all recent browsers and have fallbacks in GWT in the unlikely event of a
     * browser not supporting them. JS typed arrays support double, but not long, so this needs to compose a long from
     * two ints, which means the double-to/from-long conversions aren't as fast as float-to/from-int conversions.
     * @param bits a long.
     * @return the {@code double} floating-point value with the same bit pattern.
     */
    public static double longBitsToDouble(final long bits)
    {
        return Double.longBitsToDouble(bits);
    }
    /**
     * Converts {@code value} to a long and gets the lower 32 bits of that long, as an int.
     * @param value a {@code double} precision floating-point number.
     * @return the lower half of the bits that represent the floating-point number, as an int.
     */
    public static int doubleToLowIntBits(final double value)
    {
        return (int)(Double.doubleToLongBits(value) & 0xffffffffL);
    }

    /**
     * Converts {@code value} to a long and gets the upper 32 bits of that long, as an int.
     * @param value a {@code double} precision floating-point number.
     * @return the upper half of the bits that represent the floating-point number, as an int.
     */
    public static int doubleToHighIntBits(final double value)
    {
        return (int)(Double.doubleToLongBits(value) >>> 32);
    }

    /**
     * Converts {@code value} to a long and gets the XOR of its upper and lower 32-bit sections. Useful for numerical
     * code where a 64-bit double needs to be reduced to a 32-bit value with some hope of keeping different doubles
     * giving different ints.
     * @param value a {@code double} precision floating-point number.
     * @return the XOR of the lower and upper halves of the bits that represent the floating-point number.
     */
    public static int doubleToMixedIntBits(final double value)
    {
        final long l = Double.doubleToLongBits(value);
        return (int)(l & 0xFFFFFFFFL) ^ (int)(l >>> 32);
    }

    /**
     * Makes a modified version of value that uses the specified bits (up to 12) for its exponent and sign.
     * Meant for some specific cases, like adjusting the exponent on an unknown double to the 1.0 to 2.0 range (which
     * would pass 0x3ff for exponentBits). If you have a double from 1.0 to 2.0, you can subtract 1.0 from it to get the
     * often-desirable 0.0-1.0 range. Other common cases are 0x400, which adjusts to between 2.0 and 4.0 (subtracting
     * 3.0 from this gives the -1.0 to 1.0 range, useful for noise), and 0xBFF, which adjusts to between -2.0 and -1.0.
     * For the last case, you might think that -0x3ff would work, but sadly it doesn't. You can use
     * {@code exponentBits |= 0x800} to set the sign bit to negative, or {@code exponentBits &= 0x7ff} for positive.
     * @param value a double that will have its sign and exponent set to the specified bits
     * @param exponentBits the bits to use for the sign and exponent section of the returned modification of value
     * @return the double produced by keeping the significand of value but changing its exponent and sign as given
     */
    public static double setExponent(final double value, final int exponentBits)
    {
        return Double.longBitsToDouble((Double.doubleToLongBits(value) & 0xfffffffffffffL) | ((long) exponentBits << 52));
    }

    /**
     * Gets an 8-bit section of the given double {@code value}, using {@code whichByte} to select whether this should
     * return byte 0 (least significant), 1, 2, and so on up to 7 (most significant).
     * @param value a float
     * @param whichByte an int that will be used to select the byte to take from value (any int is allowed, only the bottom 3 bits are used to select)
     * @return the selected byte from the given float
     */
    public static byte getSelectedByte(final double value, final int whichByte)
    {
        return (byte)(Double.doubleToLongBits(value) >>> ((whichByte & 7) << 3));
    }

    /**
     * Like {@link #getSelectedByte(double, int)}, this sets the byte at a selected position in the int representation of
     * a double, then returns the double produced by the bit change. Uses {@code whichByte} to select whether this should
     * set byte 0 (least significant), 1, 2, and so on up to 7 (most significant). {@code newValue} is a byte.
     * @param value a double
     * @param whichByte an int that will be used to select the byte to take from value (any int is allowed, only the bottom 3 bits are used to select)
     * @param newValue a byte that will be placed into the returned double's bits at the selected position
     * @return a double that results from changing the bits at the selected position to match newValue
     */
    public static double setSelectedByte(final double value, final int whichByte, final byte newValue)
    {
        return Double.longBitsToDouble((Double.doubleToLongBits(value) & ~(255 << ((whichByte & 7) << 3)))
                | ((newValue & 255) << ((whichByte & 7) << 3)));
    }

    /**
     * Very limited-use; takes any double and produces a double in the -1.0 to 1.0 range, with similar inputs producing
     * close to a consistent rate of up and down through the range. This is meant for noise, where it may be useful to
     * limit the amount of change between nearby points' noise values and prevent sudden "jumps" in noise value.
     * @param value any double
     * @return a double from -1.0 (inclusive) to 1.0 (exclusive)
     */
    public static double bounce(final double value)
    {
        final long s = Double.doubleToLongBits(value);
        return Double.longBitsToDouble(((s ^ -((s & 0x8000000000000L)>>51)) & 0xfffffffffffffL)
                | 0x4010000000000000L) - 5.0;
    }

    /**
     * Very limited-use; takes any double and produces a double in the -1.0 to 1.0 range, with similar inputs producing
     * close to a consistent rate of up and down through the range. This is meant for noise, where it may be useful to
     * limit the amount of change between nearby points' noise values and prevent sudden "jumps" in noise value.
     * @param value any double
     * @return a double from -1.0 (inclusive) to 1.0 (exclusive)
     */
    public static float bounce(final float value)
    {
        final int s = Float.floatToIntBits(value);
        return Float.intBitsToFloat(((s ^ -((s & 0x00400000)>>22)) & 0x007fffff)
                | 0x40800000) - 5f;
    }
    /**
     * Very limited-use; takes the significand bits of a double, represented as a long of which this uses 52 bits, and
     * produces a double in the -1.0 to 1.0 range, with similar inputs producing close to a consistent rate of up and
     * down through the range. This is meant for noise, where it may be useful to limit the amount of change between
     * nearby points' noise values and prevent sudden "jumps" in noise value.
     * @param value any long; only the lower 52 bits will be used
     * @return a double from -1.0 (inclusive) to 1.0 (exclusive)
     */
    public static double bounce(final long value)
    {
        return Double.longBitsToDouble(((value ^ -((value & 0x8000000000000L)>>51)) & 0xfffffffffffffL)
                | 0x4010000000000000L) - 5.0;
    }
    /**
     * Very limited-use; takes the significand bits of a double, represented as a pair of ints {@code valueLow} and
     * {@code valueHigh}, using all bits in valueLow and the least-significant 20 bits of valueHigh, and
     * produces a double in the -1.0 to 1.0 range, with similar inputs producing close to a consistent rate of up and
     * down through the range. This is meant for noise, where it may be useful to limit the amount of change between
     * nearby points' noise values and prevent sudden "jumps" in noise value.
     * @param valueLow any int; all bits will be used as the less-significant bits of the significand
     * @param valueHigh any int; only the bottom 20 bits will be used as the more-significant bits of the significand
     * @return a double from -1.0 (inclusive) to 1.0 (exclusive)
     */

    public static double bounce(final int valueLow, final int valueHigh)
    {
        final long s = (((long) valueHigh) << 32 | valueLow);
        return Double.longBitsToDouble(((s ^ -((s & 0x8000000000000L)>>51)) & 0xfffffffffffffL)
                | 0x4010000000000000L) - 5.0;
    }

    /**
     * Limited-use; takes any double and produces a double in the -1.0 to 1.0 range, with similar inputs producing
     * close to a consistent rate of up and down through the range. This is meant for noise, where it may be useful to
     * limit the amount of change between nearby points' noise values and prevent sudden "jumps" in noise value. It is
     * very similar to {@link #bounce(double)}, but unlike bounce() this will maintain a continuous rate regardless of
     * the magnitude of its input. An input of any even number should produce something very close to -1.0, any odd
     * number should produce something very close to 1.0, and any number halfway between two incremental integers (like
     * 8.5 or -10.5) should produce 0.0 or a very small fraction.
     * @param value any double
     * @return a double from -1.0 (inclusive) to 1.0 (inclusive)
     */
    public static double zigzag(final double value)
    {
        final long s = Double.doubleToLongBits(value + (value < 0f ? -2.0 : 2.0)), m = (s >>> 52 & 0x7FFL) - 0x400, sm = s << m;
        return (Double.longBitsToDouble(((sm ^ -((sm & 0x8000000000000L)>>51)) & 0xfffffffffffffL)
                | 0x4010000000000000L) - 5.0);
    }

    /**
     * Limited-use; takes any float and produces a float in the -1f to 1f range, with similar inputs producing
     * close to a consistent rate of up and down through the range. This is meant for noise, where it may be useful to
     * limit the amount of change between nearby points' noise values and prevent sudden "jumps" in noise value. It is
     * very similar to {@link #bounce(float)}, but unlike bounce() this will maintain a continuous rate regardless of
     * the magnitude of its input. An input of any even number should produce something very close to -1f, any odd
     * number should produce something very close to 1f, and any number halfway between two incremental integers (like
     * 8.5f or -10.5f) should produce 0f or a very small fraction.
     * @param value any float
     * @return a float from -1f (inclusive) to 1f (inclusive)
     */
    public static float zigzag(final float value)
    {
        final int s = Float.floatToIntBits(value + (value < 0f ? -2f : 2f)), m = (s >>> 23 & 0xFF) - 0x80, sm = s << m;
        return (Float.intBitsToFloat(((sm ^ -((sm & 0x00400000)>>22)) & 0x007fffff)
                | 0x40800000) - 5f);
    }

    /**
     * Limited-use; takes any double and produces a double in the -1 to 1 range, with a graph of input to output that
     * looks much like a sine wave, curving to have a flat slope when given an integer input and a steep slope when the
     * input is halfway between two integers, smoothly curving at any points between those extremes. This is meant for
     * noise, where it may be useful to limit the amount of change between nearby points' noise values and prevent both
     * sudden "jumps" in noise value and "cracks" where a line takes a sudden jagged movement at an angle. It is very
     * similar to {@link #bounce(double)} and {@link #zigzag(double)}, but unlike bounce() this will maintain its
     * frequency of returning max or min values, regardless of the magnitude of its input (as long as there is enough
     * floating-point precision to represent changes smaller than 1.0), and unlike zigzag() this will smooth its path.
     * An input of any even number should produce something very close to -1.0, any odd number should produce something
     * very close to 1.0, and any number halfway between two incremental integers (like 8.5 or -10.5) should produce 0.0
     * or a very small fraction. In the (unlikely) event that this is given a double that is too large to represent
     * many or any non-integer values, this will simply return -1.0 or 1.0.
     * @param value any double other than NaN or infinite values; extremely large values can't work properly
     * @return a double from -1.0 (inclusive) to 1.0 (inclusive)
     */
    public static double sway(final double value)
    {
        final long s = Double.doubleToLongBits(value + (value < 0.0 ? -2.0 : 2.0)), m = (s >>> 52 & 0x7FFL) - 0x400, sm = s << m;
        final double a = (Double.longBitsToDouble(((sm ^ -((sm & 0x8000000000000L)>>51)) & 0xfffffffffffffL)
                | 0x4000000000000000L) - 2.0);
        return a * a * a * (a * (a * 6.0 - 15.0) + 10.0) * 2.0 - 1.0;
    }

    /**
     * Limited-use; takes any float and produces a float in the -1f to 1f range, with a graph of input to output that
     * looks much like a sine wave, curving to have a flat slope when given an integer input and a steep slope when the
     * input is halfway between two integers, smoothly curving at any points between those extremes. This is meant for
     * noise, where it may be useful to limit the amount of change between nearby points' noise values and prevent both
     * sudden "jumps" in noise value and "cracks" where a line takes a sudden jagged movement at an angle. It is very
     * similar to {@link #bounce(float)} and {@link #zigzag(float)}, but unlike bounce() this will maintain its
     * frequency of returning max or min values, regardless of the magnitude of its input (as long as there is enough
     * floating-point precision to represent changes smaller than 1f), and unlike zigzag() this will smooth its path.
     * An input of any even number should produce something very close to -1f, any odd number should produce something
     * very close to 1f, and any number halfway between two incremental integers (like 8.5f or -10.5f) should produce 0f
     * or a very small fraction. In the (unlikely) event that this is given a float that is too large to represent
     * many or any non-integer values, this will simply return -1f or 1f.
     * @param value any float other than NaN or infinite values; extremely large values can't work properly
     * @return a float from -1f (inclusive) to 1f (inclusive)
     */
    public static float sway(final float value)
    {
        final int s = Float.floatToIntBits(value + (value < 0f ? -2f : 2f)), m = (s >>> 23 & 0xFF) - 0x80, sm = s << m;
        final float a = (Float.intBitsToFloat(((sm ^ -((sm & 0x00400000)>>22)) & 0x007fffff) | 0x40000000) - 2f);
        return a * a * a * (a * (a * 6f - 15f) + 10f) * 2f - 1f;
    }

    /**
     * Limited-use; takes any float and produces a float in the 0f to 1f range, with a graph of input to output that
     * looks much like a sine wave, curving to have a flat slope when given an integer input and a steep slope when the
     * input is halfway between two integers, smoothly curving at any points between those extremes. This is meant for
     * noise, where it may be useful to limit the amount of change between nearby points' noise values and prevent both
     * sudden "jumps" in noise value and "cracks" where a line takes a sudden jagged movement at an angle. It is very
     * similar to {@link #bounce(float)} and {@link #zigzag(float)}, but unlike bounce() this will not change its
     * frequency of returning max or min values, regardless of the magnitude of its input (as long as there is enough
     * floating-point precision to represent changes smaller than 1f), and unlike zigzag() this will smooth its path.
     * An input of any even number should produce something very close to 0f, any odd number should produce something
     * very close to 1f, and any number halfway between two incremental integers (like 8.5f or -10.5f) should produce
     * 0.5f. In the (unlikely) event that this is given a float that is too large to represent many or any non-integer
     * values, this will simply return 0f or 1f. This version is called "Tight" because its range is tighter than
     * {@link #sway(float)}.
     * @param value any float other than NaN or infinite values; extremely large values can't work properly
     * @return a float from 0f (inclusive) to 1f (inclusive)
     */
    public static float swayTight(final float value)
    {
        final int s = Float.floatToIntBits(value + (value < 0f ? -2f : 2f)), m = (s >>> 23 & 0xFF) - 0x80, sm = s << m;
        final float a = (Float.intBitsToFloat(((sm ^ -((sm & 0x00400000)>>22)) & 0x007fffff) | 0x40000000) - 2f);
        return a * a * a * (a * (a * 6f - 15f) + 10f);
    }
    /**
     * Limited-use; takes any double and produces a double in the 0.0 to 1.0 range, with a graph of input to output that
     * looks much like a sine wave, curving to have a flat slope when given an integer input and a steep slope when the
     * input is halfway between two integers, smoothly curving at any points between those extremes. This is meant for
     * noise, where it may be useful to limit the amount of change between nearby points' noise values and prevent both
     * sudden "jumps" in noise value and "cracks" where a line takes a sudden jagged movement at an angle. It is very
     * similar to {@link #bounce(double)} and {@link #zigzag(double)}, but unlike bounce() this will not change its
     * frequency of returning max or min values, regardless of the magnitude of its input (as long as there is enough
     * floating-point precision to represent changes smaller than 1.0), and unlike zigzag() this will smooth its path.
     * An input of any even number should produce something very close to 0.0, any odd number should produce something
     * very close to 1.0, and any number halfway between two incremental integers (like 8.5 or -10.5) should produce
     * 0.5f. In the (unlikely) event that this is given a double that is too large to represent many or any non-integer
     * values, this will simply return 0.0 or 1.0. This version is called "Tight" because its range is tighter than
     * {@link #sway(double)}.
     * @param value any double other than NaN or infinite values; extremely large values can't work properly
     * @return a double from 0.0 (inclusive) to 1.0 (inclusive)
     */
    public static double swayTight(final double value)
    {
        final long s = Double.doubleToLongBits(value + (value < 0.0 ? -2.0 : 2.0)), m = (s >>> 52 & 0x7FFL) - 0x400, sm = s << m;
        final double a = (Double.longBitsToDouble(((sm ^ -((sm & 0x8000000000000L)>>51)) & 0xfffffffffffffL)
                | 0x4000000000000000L) - 2.0);
        return a * a * a * (a * (a * 6.0 - 15.0) + 10.0);
    }

    /**
     * Identical to {@link Float#floatToIntBits(float)} on desktop; optimized on GWT. Uses JS typed arrays on GWT, which
     * are well-supported now across all recent browsers and have fallbacks in GWT in the unlikely event of a browser
     * not supporting them.
     * @param value a floating-point number.
     * @return the bits that represent the floating-point number.
     */
    public static int floatToIntBits(final float value)
    {
        return Float.floatToIntBits(value);
    }
    /**
     * Identical to {@link Float#floatToIntBits(float)} on desktop (note, not {@link Float#floatToRawIntBits(float)});
     * optimized on GWT. When compiling to JS via GWT, there is no way to distinguish NaN values with different bits but
     * that are still NaN, so this doesn't try to somehow permit that. Uses JS typed arrays on GWT, which are
     * well-supported now across all recent browsers and have fallbacks in GWT in the unlikely event of a browser not
     * supporting them.
     * @param value a floating-point number.
     * @return the bits that represent the floating-point number.
     */
    public static int floatToRawIntBits(final float value)
    {
        return Float.floatToIntBits(value);
    }

    /**
     * Identical to {@link Float#intBitsToFloat(int)} on desktop; optimized on GWT. Uses JS typed arrays on GWT, which
     * are well-supported now across all recent browsers and have fallbacks in GWT in the unlikely event of a browser
     * not supporting them.
     * @param bits an integer.
     * @return the {@code float} floating-point value with the same bit pattern.
     */
    public static float intBitsToFloat(final int bits)
    {
        return Float.intBitsToFloat(bits);
    }

    /**
     * Gets an 8-bit section of the given float {@code value}, using {@code whichByte} to select whether this should
     * return byte 0 (least significant), 1, 2, or 3 (most significant).
     * @param value a float
     * @param whichByte an int that will be used to select the byte to take from value (any int is allowed, only the bottom 2 bits are used to select)
     * @return the selected byte from the given float
     */
    public static byte getSelectedByte(final float value, final int whichByte)
    {
        return (byte)(Float.floatToIntBits(value) >>> ((whichByte & 3) << 3));
    }

    /**
     * Like {@link #getSelectedByte(float, int)}, this sets the byte at a selected position in the int representation of
     * a float, then returns the float produced by the bit change. Uses {@code whichByte} to select whether this should
     * set byte 0 (least significant), 1, 2, or 3 (most significant). {@code newValue} is a byte.
     * @param value a float
     * @param whichByte an int that will be used to select the byte to take from value (any int is allowed, only the bottom 2 bits are used to select)
     * @param newValue a byte that will be placed into the returned float's bits at the selected position
     * @return a float that results from changing the bits at the selected position to match newValue
     */
    public static float setSelectedByte(final float value, final int whichByte, final byte newValue)
    {
        return Float.intBitsToFloat((Float.floatToIntBits(value) & ~(255 << ((whichByte & 3) << 3)))
                | ((newValue & 255) << ((whichByte & 3) << 3)));
    }

    /**
     * Get a pseudo-random long from this with {@code splitMix64(z += 0x9E3779B97F4A7C15L)}, where z is a long to use
     * as state. 0x9E3779B97F4A7C15L can be changed for any odd long if the same number is used across calls.
     * @param state long, must be changed with each call; {@code splitMix64(z += 0x9E3779B97F4A7C15L)} is recommended
     * @return a pseudo-random long
     */
    public static long splitMix64(long state) {
        state = ((state >>> 30) ^ state) * 0xBF58476D1CE4E5B9L;
        state = (state ^ (state >>> 27)) * 0x94D049BB133111EBL;
        return state ^ (state >>> 31);
    }

    /**
     * Generates a pseudo-random double between 0.0 (inclusive) and 1.0 (exclusive) using the given long seed, passing
     * it once through the (high-quality and very fast) {@link ThrustAltRNG} algorithm.
     * <br>
     * Consider calling this with {@code NumberTools.randomDouble(++seed)} for an optimal period of 2 to the 64 when
     * repeatedly called, but {@code NumberTools.randomDouble(seed += ODD_LONG)} will also work just fine if ODD_LONG is
     * any odd-number long, positive or negative.
     * @param seed any long to be used as a seed
     * @return a pseudo-random double from 0.0 (inclusive) to 1.0 (exclusive)
     */
    public static double randomDouble(long seed)
    {
        return (((seed = ((seed *= 0x6C8E9CF570932BD5L) ^ (seed >>> 25)) * (seed | 0xA529L)) ^ (seed >>> 23)) & 0x1FFFFFFFFFFFFFL) * 0x1p-53;
    }
    /**
     * Generates a pseudo-random float between 0f (inclusive) and 1f (exclusive) using the given long seed, passing it
     * once through the (high-quality and very fast) {@link ThrustAltRNG} algorithm.
     * <br>
     * Consider calling this with {@code NumberTools.randomFloat(++seed)} for an optimal period of 2 to the 64 when
     * repeatedly called, but {@code NumberTools.randomFloat(seed += ODD_LONG)} will also work just fine if ODD_LONG is
     * any odd-number long, positive or negative.
     * @param seed any long to be used as a seed
     * @return a pseudo-random float from -1.0f (exclusive) to 1.0f (exclusive)
     */
    public static float randomFloat(long seed)
    {
        return (((seed = ((seed *= 0x6C8E9CF570932BD5L) ^ (seed >>> 25)) * (seed | 0xA529L)) ^ (seed >>> 23)) & 0xFFFFFF) * 0x1p-24f;
    }
    /**
     * Generates a pseudo-random float between -1f (inclusive) and 1f (exclusive) using the given long seed, passing
     * it once through the (high-quality and very fast) {@link ThrustAltRNG} algorithm. This can be useful as a
     * multiplier that has approximately equal likelihood of changing or leaving the sign of its multiplicand, and won't
     * make the result larger (more significant) but will usually make it closer to 0.
     * <br>
     * Consider calling this with {@code NumberTools.randomDouble(++seed)} for an optimal period of 2 to the 64 when
     * repeatedly called, but {@code NumberTools.randomDouble(seed += ODD_LONG)} will also work just fine if ODD_LONG is
     * any odd-number long, positive or negative.
     * @param seed any long to be used as a seed
     * @return a pseudo-random float from -1.0f (exclusive) to 1.0f (exclusive)
     */
    public static float randomSignedFloat(long seed)
    {
        return (((seed = ((seed *= 0x6C8E9CF570932BD5L) ^ (seed >>> 25)) * (seed | 0xA529L)) ^ (seed >>> 23)) >> 39) * 0x1p-24f;
    }

    /**
     * Generates a pseudo-random double between -1.0 (exclusive) and 1.0 (exclusive) with a distribution that has a
     * strong central bias (around 0.0). Uses the given long seed, passing it once through the (high-quality and very
     * fast) {@link ThrustAltRNG} algorithm. This produces a pseudo-random long, which this simply passes to
     * {@link #formCurvedFloat(long)}, since it is already well-suited to generating a curved distribution.
     * <br>
     * Consider calling this with {@code NumberTools.randomFloatCurved(++seed)} for an optimal period of 2 to the 64
     * when repeatedly called, but {@code NumberTools.randomFloatCurved(seed += ODD_LONG)} will also work just fine if
     * ODD_LONG is any odd-number long, positive or negative.
     * @param seed any int to be used as a seed
     * @return a pseudo-random double from -1.0 (exclusive) to 1.0 (exclusive), distributed on a curve centered on 0.0
     */
    public static float randomFloatCurved(long seed)
    {
        return formCurvedFloat(((seed = ((seed *= 0x6C8E9CF570932BD5L) ^ (seed >>> 25)) * (seed | 0xA529L)) ^ (seed >>> 23)));
    }

    /**
     * Given an int as a seed, this uses its least-significant 23 bits to produce a float between 0f (inclusive) and 1f
     * (exclusive). This does not randomize the seed at all, and the upper 9 bits of the seed are ignored.
     * @param seed an int; only the bottom 23 bits will be used
     * @return a float between 0f (inclusive) and 1f (exclusive)
     */
    public static float formFloat(final int seed)
    {
        return Float.intBitsToFloat((seed & 0x7FFFFF) | 0x3f800000) - 1f;
    }
    /**
     * Given an int as a seed, this uses its least-significant 23 bits to produce a float between -1f (inclusive) and 1f
     * (exclusive). This does not randomize the seed at all, and the upper 9 bits of the seed are ignored.
     * @param seed an int; only the bottom 23 bits will be used
     * @return a float between -1f (inclusive) and 1f (exclusive)
     */
    public static float formSignedFloat(final int seed)
    {
        return Float.intBitsToFloat((seed & 0x7FFFFF) | 0x40000000) - 3f;
    }

    /**
     * Given a long as a seed, this uses its least-significant 52 bits to produce a double between 0 (inclusive) and 1
     * (exclusive). This does not randomize the seed at all, and the upper 12 bits of the seed are ignored.
     * @param seed a long; only the bottom 52 bits will be used
     * @return a double between 0 (inclusive) and 1 (exclusive)
     */
    public static double formDouble(final long seed)
    {
        return Double.longBitsToDouble((seed & 0xfffffffffffffL) | 0x3ff0000000000000L) - 1f;
    }
    /**
     * Given a long as a seed, this uses its least-significant 52 bits to produce a double between -1 (inclusive) and 1
     * (exclusive). This does not randomize the seed at all, and the upper 12 bits of the seed are ignored.
     * @param seed a long; only the bottom 52 bits will be used
     * @return a double between -1 (inclusive) and 1 (exclusive)
     */
    public static double formSignedDouble(final long seed)
    {
        return Double.longBitsToDouble((seed & 0xfffffffffffffL) | 0x4000000000000000L) - 3f;
    }

    /**
     * A different kind of determine-like method that expects to be given a random long and produces a random double
     * with a curved distribution that centers on 0 (where it has a bias) and can (rarely) approach -1f and 1f.
     * The distribution for the values is similar to Irwin-Hall, and is frequently near 0 but not too-rarely near -1.0
     * or 1.0. It cannot produce 1.0, -1.0, or any values further from 0 than those bounds.
     * @param start a long, usually random, such as one produced by any RandomnessSource; all bits will be used
     * @return a deterministic double between -1.0 (exclusive) and 1.0 (exclusive); very likely to be close to 0.0
     */
    public static double formCurvedDouble(long start) {
        return    longBitsToDouble((start >>> 12) | 0x3fe0000000000000L)
                + longBitsToDouble(((start *= 0x2545F4914F6CDD1DL) >>> 12) | 0x3fe0000000000000L)
                - longBitsToDouble(((start *= 0x2545F4914F6CDD1DL) >>> 12) | 0x3fe0000000000000L)
                - longBitsToDouble(((start *  0x2545F4914F6CDD1DL) >>> 12) | 0x3fe0000000000000L)
                ;
    }
    /**
     * A different kind of determine-like method that expects to be given a random long and produces a random double
     * with a curved distribution that centers on 0 (where it has a bias) and can (rarely) approach 0.0 and 1.0.
     * The distribution for the values is similar to Irwin-Hall, and is frequently near 0 but not too-rarely near 0.0 or
     * 1.0. It cannot produce 0.0, 1.0, or any values further from 0.5 than those bounds.
     * @param start a long, usually random, such as one produced by any RandomnessSource; all bits will be used
     * @return a deterministic double between 0.0 (exclusive) and 1.0 (exclusive); very likely to be close to 0.5
     */
    public static double formCurvedDoubleTight(long start) {
        return  0.5
                + longBitsToDouble((start >>> 12) | 0x3fd0000000000000L)
                + longBitsToDouble(((start *= 0x2545F4914F6CDD1DL) >>> 12) | 0x3fd0000000000000L)
                - longBitsToDouble(((start *= 0x2545F4914F6CDD1DL) >>> 12) | 0x3fd0000000000000L)
                - longBitsToDouble(((start *  0x2545F4914F6CDD1DL) >>> 12) | 0x3fd0000000000000L);
    }

    /**
     * A different kind of determine-like method that expects to be given a random long and produces a random float with
     * a curved distribution that centers on 0 (where it has a bias) and can (rarely) approach -1f and 1f.
     * The distribution for the values is similar to Irwin-Hall, and is frequently near 0 but not too-rarely near -1f or
     * 1f. It cannot produce 1f, -1f, or any values further from 0 than those bounds.
     * @param start a long, usually random, such as one produced by any RandomnessSource
     * @return a deterministic float between -1f (exclusive) and 1f (exclusive), that is very likely to be close to 0f
     */
    public static float formCurvedFloat(final long start) {
        return    intBitsToFloat((int)start >>> 9 | 0x3F000000)
                + intBitsToFloat((int) (start >>> 41) | 0x3F000000)
                - intBitsToFloat(((int)(start ^ ~start >>> 20) & 0x007FFFFF) | 0x3F000000)
                - intBitsToFloat(((int) (~start ^ start >>> 30) & 0x007FFFFF) | 0x3F000000)
                ;
    }

    /**
     * A different kind of determine-like method that expects to be given random ints and produces a random float with
     * a curved distribution that centers on 0 (where it has a bias) and can (rarely) approach -1f and 1f.
     * The distribution for the values is similar to Irwin-Hall, and is frequently near 0 but not too-rarely near -1f or
     * 1f. It cannot produce 1f, -1f, or any values further from 0 than those bounds.
     * @param start1 an int usually random, such as one produced by any RandomnessSource
     * @param start2 an int usually random, such as one produced by any RandomnessSource
     * @return a deterministic float between -1f (exclusive) and 1f (exclusive), that is very likely to be close to 0f
     */
    public static float formCurvedFloat(final int start1, final int start2) {
        return    intBitsToFloat(start1 >>> 9 | 0x3F000000)
                + intBitsToFloat((~start1 & 0x007FFFFF) | 0x3F000000)
                - intBitsToFloat(start2 >>> 9 | 0x3F000000)
                - intBitsToFloat((~start2 & 0x007FFFFF) | 0x3F000000)
                ;
    }
    /**
     * A different kind of determine-like method that expects to be given a random int and produces a random float with
     * a curved distribution that centers on 0 (where it has a bias) and can (rarely) approach -1f and 1f.
     * The distribution for the values is similar to Irwin-Hall, and is frequently near 0 but not too-rarely near -1f or
     * 1f. It cannot produce 1f, -1f, or any values further from 0 than those bounds.
     * @param start an int, usually random, such as one produced by any RandomnessSource
     * @return a deterministic float between -1f (exclusive) and 1f (exclusive), that is very likely to be close to 0f
     */
    public static float formCurvedFloat(final int start) {
        return    intBitsToFloat(start >>> 9 | 0x3F000000)
                + intBitsToFloat((start & 0x007FFFFF) | 0x3F000000)
                - intBitsToFloat(((start << 18 & 0x007FFFFF) ^ ~start >>> 14) | 0x3F000000)
                - intBitsToFloat(((start << 13 & 0x007FFFFF) ^ ~start >>> 19) | 0x3F000000)
                ;
    }

    /**
     * Returns an int value with at most a single one-bit, in the position of the lowest-order ("rightmost") one-bit in
     * the specified int value. Returns zero if the specified value has no one-bits in its two's complement binary
     * representation, that is, if it is equal to zero.
     * <br>
     * Identical to {@link Integer#lowestOneBit(int)}, but super-sourced to act correctly on GWT. If you have GWT as a
     * target and do bit manipulation work, double-check everything! An int can be higher than {@link Integer#MAX_VALUE}
     * or lower than {@link Integer#MIN_VALUE} on GWT, without actually being a long (internally it's a double). This
     * is especially relevant for the overload of this method that takes and returns a long;
     * {@link Long#lowestOneBit(long)} does not provide correct results for certain inputs on GWT, such as
     * -17592186044416L, which it mysteriously returns 0L on, so you should use {@link #lowestOneBit(long)}.
     * @param num the value whose lowest one bit is to be computed
     * @return an int value with a single one-bit, in the position of the lowest-order one-bit in the specified value,
     *         or zero if the specified value is itself equal to zero.
     */
    public static int lowestOneBit(int num)
    {
        return num & -num;
    }
    /**
     * Returns an long value with at most a single one-bit, in the position of the lowest-order ("rightmost") one-bit in
     * the specified long value. Returns zero if the specified value has no one-bits in its two's complement binary
     * representation, that is, if it is equal to zero.
     * <br>
     * Identical to {@link Long#lowestOneBit(long)}, but super-sourced to act correctly on GWT. If you have GWT as a
     * target and do bit manipulation work, double-check everything! An int can be higher than {@link Integer#MAX_VALUE}
     * or lower than {@link Integer#MIN_VALUE} on GWT, without actually being a long (internally it's a double). This
     * is especially relevant for this overload (for longs more so than for ints); {@link Long#lowestOneBit(long)} does
     * not provide correct results for certain inputs on GWT, such as -17592186044416L, which it mysteriously returns 0L
     * on, so you should use this method.
     * @param num the value whose lowest one bit is to be computed
     * @return a long value with a single one-bit, in the position of the lowest-order one-bit in the specified value,
     *         or zero if the specified value is itself equal to zero.
     */
    public static long lowestOneBit(long num)
    {
        return num & -num;
    }

//    /**
//     * A fairly-close approximation of {@link Math#sin(double)} that can be significantly faster (between 4x and 40x
//     * faster sin() calls in benchmarking, depending on whether HotSpot deoptimizes Math.sin() for its own inscrutable
//     * reasons), and both takes and returns doubles. Takes the same arguments Math.sin() does, so one angle in radians,
//     * which may technically be any double (but this will lose precision on fairly large doubles, such as those that
//     * are larger than about 65536.0). This is closely related to {@link #sway(float)}, but the shape of the output when
//     * graphed is almost identical to sin().  The difference between the result of this method and
//     * {@link Math#sin(double)} should be under 0.001 at all points between -pi and pi, with an average difference of
//     * about 0.0005; not all points have been checked for potentially higher errors, though. Coercion between float and
//     * double takes about as long as this method normally takes to run, so if you have floats you should usually use
//     * methods that take floats (or return floats, if assigning the result to a float), and likewise for doubles.
//     * <br>
//     * If you call this frequently, consider giving it either all positive numbers, i.e. 0 to PI * 2 instead of -PI to
//     * PI; this can help the performance of this particular approximation by making its one branch easier to predict.
//     * <br>
//     * The technique for sine approximation is mostly from
//     * <a href="https://web.archive.org/web/20080228213915/http://devmaster.net/forums/showthread.php?t=5784">this archived DevMaster thread</a>,
//     * with credit to "Nick". Changes have been made to accelerate wrapping from any double to the valid input range,
//     * using code extremely similar to {@link #zigzag(double)}.
//     * @param radians an angle in radians as a double, often from 0 to pi * 2, though not required to be.
//     * @return the sine of the given angle, as a double between -1.0 and 1.0 (probably exclusive on -1.0, but not 1.0)
//     */
//    public static double sin(final double radians)
//    {
//        long sign, s;
//        if(radians < 0.0) {
//            s = Double.doubleToLongBits(radians * 0.3183098861837907 - 2.0);
//            sign = 1L;
//        }
//        else {
//            s = Double.doubleToLongBits(radians * 0.3183098861837907 + 2.0);
//            sign = -1L;
//        }
//        final long m = (s >>> 52 & 0x7FFL) - 0x400L, sm = s << m, sn = -((sm & 0x8000000000000L) >> 51);
//        double n = (Double.longBitsToDouble(((sm ^ sn) & 0xfffffffffffffL) | 0x4010000000000000L) - 4.0);
//        n *= 2.0 - n;
//        return n * (-0.775 - 0.225 * n) * ((sn ^ sign) | 1L);
//    }
//
//    /**
//     * A fairly-close approximation of {@link Math#sin(double)} that can be significantly faster (between 4x and 40x
//     * faster sin() calls in benchmarking, depending on whether HotSpot deoptimizes Math.sin() for its own inscrutable
//     * reasons), and both takes and returns floats. Takes the same arguments Math.sin() does, so one angle in radians,
//     * which may technically be any float (but this will lose precision on fairly large floats, such as those that are
//     * larger than about 4096f). This is closely related to {@link #sway(float)}, but the shape of the output when
//     * graphed is almost identical to sin(). The difference between the result of this method and
//     * {@link Math#sin(double)} should be under 0.001 at all points between -pi and pi, with an average difference of
//     * about 0.0005; not all points have been checked for potentially higher errors, though. The error for this float
//     * version is extremely close to the double version, {@link #sin(double)}, so you should choose based on what type
//     * you have as input and/or want to return rather than on quality concerns. Coercion between float and double takes
//     * about as long as this method normally takes to run, so if you have floats you should usually use methods that
//     * take floats (or return floats, if assigning the result to a float), and likewise for doubles.
//     * <br>
//     * If you call this frequently, consider giving it either all positive numbers, i.e. 0 to PI * 2 instead of -PI to
//     * PI; this can help the performance of this particular approximation by making its one branch easier to predict.
//     * <br>
//     * The technique for sine approximation is mostly from
//     * <a href="https://web.archive.org/web/20080228213915/http://devmaster.net/forums/showthread.php?t=5784">this archived DevMaster thread</a>,
//     * with credit to "Nick". Changes have been made to accelerate wrapping from any double to the valid input range,
//     * using code extremely similar to {@link #zigzag(float)}.
//     * @param radians an angle in radians as a float, often from 0 to pi * 2, though not required to be.
//     * @return the sine of the given angle, as a float between -1f and 1f (probably exclusive on -1f, but not 1f)
//     */
//    public static float sin(final float radians)
//    {
//        int sign, s;
//        if(radians < 0.0f) {
//            s = Float.floatToIntBits(radians * 0.3183098861837907f - 2f);
//            sign = 1;
//        }
//        else {
//            s = Float.floatToIntBits(radians * 0.3183098861837907f + 2f);
//            sign = -1;
//        }
//        final int m = (s >>> 23 & 0xFF) - 0x80, sm = s << m, sn = -((sm & 0x00400000) >> 22);
//        float n = (Float.intBitsToFloat(((sm ^ sn) & 0x007fffff) | 0x40800000) - 4f);
//        n *= 2f - n;
//        return n * (-0.775f - 0.225f * n) * ((sn ^ sign) | 1);
//    }
//
//    /**
//     * A fairly-close approximation of {@link Math#cos(double)} that can be significantly faster (between 4x and 40x
//     * faster cos() calls in benchmarking, depending on whether HotSpot deoptimizes Math.cos() for its own inscrutable
//     * reasons), and both takes and returns doubles. Takes the same arguments Math.cos() does, so one angle in radians,
//     * which may technically be any double (but this will lose precision on fairly large doubles, such as those that
//     * are larger than about 65536.0). This is closely related to {@link #sway(float)}, but the shape of the output when
//     * graphed is almost identical to cos(). The difference between the result of this method and
//     * {@link Math#cos(double)} should be under 0.001 at all points between -pi and pi, with an average difference of
//     * about 0.0005; not all points have been checked for potentially higher errors, though.Coercion between float and
//     * double takes about as long as this method normally takes to run, so if you have floats you should usually use
//     * methods that take floats (or return floats, if assigning the result to a float), and likewise for doubles.
//     * <br>
//     * If you call this frequently, consider giving it either all positive numbers, i.e. 0 to PI * 2 instead of -PI to
//     * PI; this can help the performance of this particular approximation by making its one branch easier to predict.
//     * <br>
//     * The technique for cosine approximation is mostly from
//     * <a href="https://web.archive.org/web/20080228213915/http://devmaster.net/forums/showthread.php?t=5784">this archived DevMaster thread</a>,
//     * with credit to "Nick". Changes have been made to accelerate wrapping from any double to the valid input range,
//     * using code extremely similar to {@link #zigzag(double)}.
//     * @param radians an angle in radians as a double, often from 0 to pi * 2, though not required to be.
//     * @return the cosine of the given angle, as a double between -1.0 and 1.0 (probably exclusive on 1.0, but not -1.0)
//     */
//    public static double cos(final double radians)
//    {
//        long sign, s;
//        if(radians < -1.5707963267948966) {
//            s = Double.doubleToLongBits(radians * 0.3183098861837907 - 1.5);
//            sign = 1L;
//        }
//        else {
//            s = Double.doubleToLongBits(radians * 0.3183098861837907 + 2.5);
//            sign = -1L;
//        }
//        final long m = (s >>> 52 & 0x7FFL) - 0x400L, sm = s << m, sn = -((sm & 0x8000000000000L) >> 51);
//        double n = (Double.longBitsToDouble(((sm ^ sn) & 0xfffffffffffffL) | 0x4010000000000000L) - 4.0);
//        n *= 2.0 - n;
//        return n * (-0.775 - 0.225 * n) * ((sn ^ sign) | 1L);
//    }
//
//    /**
//     * A fairly-close approximation of {@link Math#cos(double)} that can be significantly faster (between 4x and 40x
//     * faster cos() calls in benchmarking, depending on whether HotSpot deoptimizes Math.cos() for its own inscrutable
//     * reasons), and both takes and returns floats. Takes the same arguments Math.cos() does, so one angle in radians,
//     * which may technically be any float (but this will lose precision on fairly large floats, such as those that are
//     * larger than about 4096f). This is closely related to {@link #sway(float)}, but the shape of the output when
//     * graphed is almost identical to cos(). The difference between the result of this method and
//     * {@link Math#cos(double)} should be under 0.001 at all points between -pi and pi, with an average difference of
//     * about 0.0005; not all points have been checked for potentially higher errors, though. The error for this float
//     * version is extremely close to the double version, {@link #cos(double)}, so you should choose based on what type
//     * you have as input and/or want to return rather than on quality concerns. Coercion between float and double takes
//     * about as long as this method normally takes to run, so if you have floats you should usually use methods that
//     * take floats (or return floats, if assigning the result to a float), and likewise for doubles.
//     * <br>
//     * If you call this frequently, consider giving it either all positive numbers, i.e. 0 to PI * 2 instead of -PI to
//     * PI; this can help the performance of this particular approximation by making its one branch easier to predict.
//     * <br>
//     * The technique for cosine approximation is mostly from
//     * <a href="https://web.archive.org/web/20080228213915/http://devmaster.net/forums/showthread.php?t=5784">this archived DevMaster thread</a>,
//     * with credit to "Nick". Changes have been made to accelerate wrapping from any double to the valid input range,
//     * using code extremely similar to {@link #zigzag(float)}.
//     * @param radians an angle in radians as a float, often from 0 to pi * 2, though not required to be.
//     * @return the cosine of the given angle, as a float between -1f and 1f (probably exclusive on 1f, but not -1f)
//     */
//    public static float cos(final float radians)
//    {
//        int sign, s;
//        if(radians < -1.5707963267948966f) {
//            s = Float.floatToIntBits(radians * 0.3183098861837907f - 1.5f);
//            sign = 1;
//        }
//        else {
//            s = Float.floatToIntBits(radians * 0.3183098861837907f + 2.5f);
//            sign = -1;
//        }
//        final int m = (s >>> 23 & 0xFF) - 0x80, sm = s << m, sn = -((sm & 0x00400000) >> 22);
//        float n = (Float.intBitsToFloat(((sm ^ sn) & 0x007fffff) | 0x40800000) - 4f);
//        n *= 2f - n;
//        return n * (-0.775f - 0.225f * n) * ((sn ^ sign) | 1);
//    }
    /**
     * A fairly-close approximation of {@link Math#sin(double)} that can be significantly faster (between 8x and 80x
     * faster sin() calls in benchmarking; if you have access to libGDX you should consider its sometimes-more-precise
     * and sometimes-faster MathUtils.sin() method. Because this method doesn't rely on a
     * lookup table, where libGDX's MathUtils does, applications that have a bottleneck on memory may perform better
     * with this method than with MathUtils. Takes the same arguments Math.sin() does, so one angle in radians,
     * which may technically be any double (but this will lose precision on fairly large doubles, such as those that are
     * larger than {@link Long#MAX_VALUE}, because those doubles themselves will lose precision at that scale). This
     * is closely related to {@link #sway(double)}, but the shape of the output when graphed is almost identical to
     * sin(). The difference between the result of this method and {@link Math#sin(double)} should be under 0.0011 at
     * all points between -pi and pi, with an average difference of about 0.0005; not all points have been checked for
     * potentially higher errors, though.
     * <br>
     * The error for this double version is extremely close to the float version, {@link #sin(float)}, so you should
     * choose based on what type you have as input and/or want to return rather than on quality concerns. Coercion
     * between float and double takes about as long as this method normally takes to run (or longer), so if you have
     * floats you should usually use methods that take floats (or return floats, if assigning the result to a float),
     * and likewise for doubles.
     * <br>
     * Unlike in previous versions of this method, the sign of the input doesn't affect performance here, at least not
     * by a measurable amount.
     * <br>
     * The technique for sine approximation is mostly from
     * <a href="https://web.archive.org/web/20080228213915/http://devmaster.net/forums/showthread.php?t=5784">this archived DevMaster thread</a>,
     * with credit to "Nick". Changes have been made to accelerate wrapping from any double to the valid input range.
     * @param radians an angle in radians as a double, often from 0 to pi * 2, though not required to be.
     * @return the sine of the given angle, as a double between -1.0 and 1.0 (both inclusive)
     */

    public static double sin(double radians)
    {
        radians *= 0.6366197723675814;
        final long floor = (radians >= 0.0 ? (long) radians : (long) radians - 1L) & -2L;
        radians -= floor;
        radians *= 2.0 - radians;
        return radians * (-0.775 - 0.225 * radians) * ((floor & 2L) - 1L);
    }

    /**
     * A fairly-close approximation of {@link Math#cos(double)} that can be significantly faster (between 8x and 80x
     * faster cos() calls in benchmarking; if you have access to libGDX you should consider its sometimes-more-precise
     * and sometimes-faster MathUtils.cos() method. Because this method doesn't rely on a
     * lookup table, where libGDX's MathUtils does, applications that have a bottleneck on memory may perform better
     * with this method than with MathUtils. Takes the same arguments Math.cos() does, so one angle in radians,
     * which may technically be any double (but this will lose precision on fairly large doubles, such as those that are
     * larger than {@link Long#MAX_VALUE}, because those doubles themselves will lose precision at that scale). This
     * is closely related to {@link #sway(double)}, but the shape of the output when graphed is almost identical to
     * cos(). The difference between the result of this method and {@link Math#cos(double)} should be under 0.0011 at
     * all points between -pi and pi, with an average difference of about 0.0005; not all points have been checked for
     * potentially higher errors, though.
     * <br>
     * The error for this double version is extremely close to the float version, {@link #cos(float)}, so you should
     * choose based on what type you have as input and/or want to return rather than on quality concerns. Coercion
     * between float and double takes about as long as this method normally takes to run (or longer), so if you have
     * floats you should usually use methods that take floats (or return floats, if assigning the result to a float),
     * and likewise for doubles.
     * <br>
     * Unlike in previous versions of this method, the sign of the input doesn't affect performance here, at least not
     * by a measurable amount.
     * The technique for cosine approximation is mostly from
     * <a href="https://web.archive.org/web/20080228213915/http://devmaster.net/forums/showthread.php?t=5784">this archived DevMaster thread</a>,
     * with credit to "Nick". Changes have been made to accelerate wrapping from any double to the valid input range.
     * @param radians an angle in radians as a double, often from 0 to pi * 2, though not required to be.
     * @return the cosine of the given angle, as a double between -1.0 and 1.0 (both inclusive)
     */
    public static double cos(double radians)
    {
        radians = radians * 0.6366197723675814 + 1.0;
        final long floor = (radians >= 0.0 ? (long) radians : (long) radians - 1L) & -2L;
        radians -= floor;
        radians *= 2.0 - radians;
        return radians * (-0.775 - 0.225 * radians) * ((floor & 2L) - 1L);
    }

    /**
     * A fairly-close approximation of {@link Math#sin(double)} that can be significantly faster (between 8x and 80x
     * faster sin() calls in benchmarking, and both takes and returns floats; if you have access to libGDX you should
     * consider its more-precise and sometimes-faster MathUtils.sin() method. Because this method doesn't rely on a
     * lookup table, where libGDX's MathUtils does, applications that have a bottleneck on memory may perform better
     * with this method than with MathUtils. Takes the same arguments Math.sin() does, so one angle in radians,
     * which may technically be any float (but this will lose precision on fairly large floats, such as those that are
     * larger than {@link Integer#MAX_VALUE}, because those floats themselves will lose precision at that scale). This
     * is closely related to {@link #sway(float)}, but the shape of the output when graphed is almost identical to
     * sin(). The difference between the result of this method and {@link Math#sin(double)} should be under 0.0011 at
     * all points between -pi and pi, with an average difference of about 0.0005; not all points have been checked for
     * potentially higher errors, though.
     * <br>
     * The error for this float version is extremely close to the double version, {@link #sin(double)}, so you should
     * choose based on what type you have as input and/or want to return rather than on quality concerns. Coercion
     * between float and double takes about as long as this method normally takes to run (or longer), so if you have
     * floats you should usually use methods that take floats (or return floats, if assigning the result to a float),
     * and likewise for doubles.
     * <br>
     * Unlike in previous versions of this method, the sign of the input doesn't affect performance here, at least not
     * by a measurable amount.
     * <br>
     * The technique for sine approximation is mostly from
     * <a href="https://web.archive.org/web/20080228213915/http://devmaster.net/forums/showthread.php?t=5784">this archived DevMaster thread</a>,
     * with credit to "Nick". Changes have been made to accelerate wrapping from any float to the valid input range.
     * @param radians an angle in radians as a float, often from 0 to pi * 2, though not required to be.
     * @return the sine of the given angle, as a float between -1f and 1f (both inclusive)
     */
    public static float sin(float radians)
    {
        radians *= 0.6366197723675814f;
        final int floor = (radians >= 0.0 ? (int) radians : (int) radians - 1) & -2;
        radians -= floor;
        radians *= 2f - radians;
        return radians * (-0.775f - 0.225f * radians) * ((floor & 2) - 1);
    }

    /**
     * A fairly-close approximation of {@link Math#cos(double)} that can be significantly faster (between 8x and 80x
     * faster cos() calls in benchmarking, and both takes and returns floats; if you have access to libGDX you should
     * consider its more-precise and sometimes-faster MathUtils.cos() method. Because this method doesn't rely on a
     * lookup table, where libGDX's MathUtils does, applications that have a bottleneck on memory may perform better
     * with this method than with MathUtils. Takes the same arguments Math.cos() does, so one angle in radians,
     * which may technically be any float (but this will lose precision on fairly large floats, such as those that are
     * larger than {@link Integer#MAX_VALUE}, because those floats themselves will lose precision at that scale). This
     * is closely related to {@link #sway(float)}, but the shape of the output when graphed is almost identical to
     * cos(). The difference between the result of this method and {@link Math#cos(double)} should be under 0.0011 at
     * all points between -pi and pi, with an average difference of about 0.0005; not all points have been checked for
     * potentially higher errors, though.
     * <br>
     * The error for this float version is extremely close to the double version, {@link #cos(double)}, so you should
     * choose based on what type you have as input and/or want to return rather than on quality concerns. Coercion
     * between float and double takes about as long as this method normally takes to run (or longer), so if you have
     * floats you should usually use methods that take floats (or return floats, if assigning the result to a float),
     * and likewise for doubles.
     * <br>
     * Unlike in previous versions of this method, the sign of the input doesn't affect performance here, at least not
     * by a measurable amount.
     * <br>
     * The technique for cosine approximation is mostly from
     * <a href="https://web.archive.org/web/20080228213915/http://devmaster.net/forums/showthread.php?t=5784">this archived DevMaster thread</a>,
     * with credit to "Nick". Changes have been made to accelerate wrapping from any float to the valid input range.
     * @param radians an angle in radians as a float, often from 0 to pi * 2, though not required to be.
     * @return the cosine of the given angle, as a float between -1f and 1f (both inclusive)
     */
    public static float cos(float radians)
    {
        radians = radians * 0.6366197723675814f + 1f;
        final int floor = (radians >= 0.0 ? (int) radians : (int) radians - 1) & -2;
        radians -= floor;
        radians *= 2f - radians;
        return radians * (-0.775f - 0.225f * radians) * ((floor & 2) - 1);
    }
    /**
     * A fairly-close approximation of {@link Math#sin(double)} that can be significantly faster (between 8x and 80x
     * faster sin() calls in benchmarking, and both takes and returns floats; if you have access to libGDX, you should
     * consider its more-precise and sometimes-faster MathUtils.sinDeg() method. Because this method doesn't rely on a
     * lookup table, where libGDX's MathUtils does, applications that have a bottleneck on memory may perform better
     * with this method than with MathUtils. Takes one angle in degrees,
     * which may technically be any float (but this will lose precision on fairly large floats, such as those that are
     * larger than {@link Integer#MAX_VALUE}, because those floats themselves will lose precision at that scale). This
     * is closely related to {@link #sway(float)}, but the shape of the output when graphed is almost identical to
     * sin(). The difference between the result of this method and {@link Math#sin(double)} should be under 0.0011 at
     * all points between -360 and 360, with an average difference of about 0.0005; not all points have been checked for
     * potentially higher errors, though.
     * <br>
     * The error for this float version is extremely close to the double version, {@link #sin(double)}, so you should
     * choose based on what type you have as input and/or want to return rather than on quality concerns. Coercion
     * between float and double takes about as long as this method normally takes to run (or longer), so if you have
     * floats you should usually use methods that take floats (or return floats, if assigning the result to a float),
     * and likewise for doubles.
     * <br>
     * Unlike in previous versions of this method, the sign of the input doesn't affect performance here, at least not
     * by a measurable amount.
     * <br>
     * The technique for sine approximation is mostly from
     * <a href="https://web.archive.org/web/20080228213915/http://devmaster.net/forums/showthread.php?t=5784">this archived DevMaster thread</a>,
     * with credit to "Nick". Changes have been made to accelerate wrapping from any float to the valid input range.
     * @param degrees an angle in degrees as a float, often from 0 to 360, though not required to be.
     * @return the sine of the given angle, as a float between -1f and 1f (both inclusive)
     */
    public static float sinDegrees(float degrees)
    {
        degrees = degrees * 0.011111111111111112f;
        final int floor = (degrees >= 0.0 ? (int) degrees : (int) degrees - 1) & -2;
        degrees -= floor;
        degrees *= 2f - degrees;
        return degrees * (-0.775f - 0.225f * degrees) * ((floor & 2) - 1);
    }

    /**
     * A fairly-close approximation of {@link Math#cos(double)} that can be significantly faster (between 8x and 80x
     * faster cos() calls in benchmarking, and both takes and returns floats; if you have access to libGDX, you should
     * consider its more-precise and sometimes-faster MathUtils.cosDeg() method. Because this method doesn't rely on a
     * lookup table, where libGDX's MathUtils does, applications that have a bottleneck on memory may perform better
     * with this method than with MathUtils. Takes one angle in degrees,
     * which may technically be any float (but this will lose precision on fairly large floats, such as those that are
     * larger than {@link Integer#MAX_VALUE}, because those floats themselves will lose precision at that scale). This
     * is closely related to {@link #sway(float)}, but the shape of the output when graphed is almost identical to
     * cos(). The difference between the result of this method and {@link Math#cos(double)} should be under 0.0011 at
     * all points between -360 and 360, with an average difference of about 0.0005; not all points have been checked for
     * potentially higher errors, though.
     * <br>
     * The error for this float version is extremely close to the double version, {@link #cos(double)}, so you should
     * choose based on what type you have as input and/or want to return rather than on quality concerns. Coercion
     * between float and double takes about as long as this method normally takes to run (or longer), so if you have
     * floats you should usually use methods that take floats (or return floats, if assigning the result to a float),
     * and likewise for doubles.
     * <br>
     * Unlike in previous versions of this method, the sign of the input doesn't affect performance here, at least not
     * by a measurable amount.
     * <br>
     * The technique for cosine approximation is mostly from
     * <a href="https://web.archive.org/web/20080228213915/http://devmaster.net/forums/showthread.php?t=5784">this archived DevMaster thread</a>,
     * with credit to "Nick". Changes have been made to accelerate wrapping from any float to the valid input range.
     * @param degrees an angle in degrees as a float, often from 0 to pi * 2, though not required to be.
     * @return the cosine of the given angle, as a float between -1f and 1f (both inclusive)
     */
    public static float cosDegrees(float degrees)
    {
        degrees = degrees * 0.011111111111111112f + 1f;
        final int floor = (degrees >= 0.0 ? (int) degrees : (int) degrees - 1) & -2;
        degrees -= floor;
        degrees *= 2f - degrees;
        return degrees * (-0.775f - 0.225f * degrees) * ((floor & 2) - 1);
    }

    /**
     * A variation on {@link Math#sin(double)} that takes its input as a fraction of a turn instead of in radians; one
     * turn is equal to 360 degrees or two*PI radians. This can be useful as a building block for other measurements;
     * to make a sine method that takes its input in grad (with 400 grad equal to 360 degrees), you would just divide
     * the grad value by 400.0 (or multiply it by 0.0025) and pass it to this method. Similarly for binary degrees, also
     * called brad (with 256 brad equal to 360 degrees), you would divide by 256.0 or multiply by 0.00390625 before
     * passing that value here. The brad case is especially useful because you can use a byte for any brad values, and
     * adding up those brad values will wrap correctly (256 brad goes back to 0) while keeping perfect precision for the
     * results (you still divide by 256.0 when you pass the brad value to this method).
     * <br>
     * The error for this double version is extremely close to the float version, {@link #sin_(float)}, so you should
     * choose based on what type you have as input and/or want to return rather than on quality concerns. Coercion
     * between float and double takes about as long as this method normally takes to run (or longer), so if you have
     * floats you should usually use methods that take floats (or return floats, if assigning the result to a float),
     * and likewise for doubles.
     * <br>
     * The technique for sine approximation is mostly from
     * <a href="https://web.archive.org/web/20080228213915/http://devmaster.net/forums/showthread.php?t=5784">this archived DevMaster thread</a>,
     * with credit to "Nick". Changes have been made to accelerate wrapping from any double to the valid input range.
     * @param turns an angle as a fraction of a turn as a double, with 0.5 here equivalent to PI radians in {@link #cos(double)}
     * @return the sine of the given angle, as a double between -1.0 and 1.0 (both inclusive)
     */
    public static double sin_(double turns)
    {
        turns *= 4.0;
        final long floor = (turns >= 0.0 ? (long) turns : (long) turns - 1L) & -2L;
        turns -= floor;
        turns *= 2.0 - turns;
        return turns * (-0.775 - 0.225 * turns) * ((floor & 2L) - 1L);
    }

    /**
     * A variation on {@link Math#cos(double)} that takes its input as a fraction of a turn instead of in radians; one
     * turn is equal to 360 degrees or two*PI radians. This can be useful as a building block for other measurements;
     * to make a cosine method that takes its input in grad (with 400 grad equal to 360 degrees), you would just divide
     * the grad value by 400.0 (or multiply it by 0.0025) and pass it to this method. Similarly for binary degrees, also
     * called brad (with 256 brad equal to 360 degrees), you would divide by 256.0 or multiply by 0.00390625 before
     * passing that value here. The brad case is especially useful because you can use a byte for any brad values, and
     * adding up those brad values will wrap correctly (256 brad goes back to 0) while keeping perfect precision for the
     * results (you still divide by 256.0 when you pass the brad value to this method).
     * <br>
     * The error for this double version is extremely close to the float version, {@link #cos_(float)}, so you should
     * choose based on what type you have as input and/or want to return rather than on quality concerns. Coercion
     * between float and double takes about as long as this method normally takes to run (or longer), so if you have
     * floats you should usually use methods that take floats (or return floats, if assigning the result to a float),
     * and likewise for doubles.
     * <br>
     * The technique for cosine approximation is mostly from
     * <a href="https://web.archive.org/web/20080228213915/http://devmaster.net/forums/showthread.php?t=5784">this archived DevMaster thread</a>,
     * with credit to "Nick". Changes have been made to accelerate wrapping from any double to the valid input range.
     * @param turns an angle as a fraction of a turn as a double, with 0.5 here equivalent to PI radians in {@link #cos(double)}
     * @return the cosine of the given angle, as a double between -1.0 and 1.0 (both inclusive)
     */
    public static double cos_(double turns)
    {
        turns = turns * 4.0 + 1.0;
        final long floor = (turns >= 0.0 ? (long) turns : (long) turns - 1L) & -2L;
        turns -= floor;
        turns *= 2.0 - turns;
        return turns * (-0.775 - 0.225 * turns) * ((floor & 2L) - 1L);
    }

    /**
     * A variation on {@link Math#sin(double)} that takes its input as a fraction of a turn instead of in radians (it
     * also takes and returns a float); one turn is equal to 360 degrees or two*PI radians. This can be useful as a
     * building block for other measurements; to make a sine method that takes its input in grad (with 400 grad equal to
     * 360 degrees), you would just divide the grad value by 400.0 (or multiply it by 0.0025) and pass it to this
     * method. Similarly for binary degrees, also called brad (with 256 brad equal to 360 degrees), you would divide by
     * 256.0 or multiply by 0.00390625 before passing that value here. The brad case is especially useful because you
     * can use a byte for any brad values, and adding up those brad values will wrap correctly (256 brad goes back to 0)
     * while keeping perfect precision for the results (you still divide by 256.0 when you pass the brad value to this
     * method).
     * <br>
     * The error for this float version is extremely close to the double version, {@link #sin_(double)}, so you should
     * choose based on what type you have as input and/or want to return rather than on quality concerns. Coercion
     * between float and double takes about as long as this method normally takes to run (or longer), so if you have
     * floats you should usually use methods that take floats (or return floats, if assigning the result to a float),
     * and likewise for doubles.
     * <br>
     * The technique for sine approximation is mostly from
     * <a href="https://web.archive.org/web/20080228213915/http://devmaster.net/forums/showthread.php?t=5784">this archived DevMaster thread</a>,
     * with credit to "Nick". Changes have been made to accelerate wrapping from any double to the valid input range.
     * @param turns an angle as a fraction of a turn as a float, with 0.5 here equivalent to PI radians in {@link #cos(double)}
     * @return the sine of the given angle, as a float between -1.0 and 1.0 (both inclusive)
     */
    public static float sin_(float turns)
    {
        turns *= 4f;
        final long floor = (turns >= 0.0 ? (long) turns : (long) turns - 1L) & -2L;
        turns -= floor;
        turns *= 2f - turns;
        return turns * (-0.775f - 0.225f * turns) * ((floor & 2L) - 1L);
    }

    /**
     * A variation on {@link Math#cos(double)} that takes its input as a fraction of a turn instead of in radians (it
     * also takes and returns a float); one turn is equal to 360 degrees or two*PI radians. This can be useful as a
     * building block for other measurements; to make a cosine method that takes its input in grad (with 400 grad equal
     * to 360 degrees), you would just divide the grad value by 400.0 (or multiply it by 0.0025) and pass it to this
     * method. Similarly for binary degrees, also called brad (with 256 brad equal to 360 degrees), you would divide by
     * 256.0 or multiply by 0.00390625 before passing that value here. The brad case is especially useful because you
     * can use a byte for any brad values, and adding up those brad values will wrap correctly (256 brad goes back to 0)
     * while keeping perfect precision for the results (you still divide by 256.0 when you pass the brad value to this
     * method).
     * <br>
     * The error for this float version is extremely close to the float version, {@link #cos_(double)}, so you should
     * choose based on what type you have as input and/or want to return rather than on quality concerns. Coercion
     * between float and double takes about as long as this method normally takes to run (or longer), so if you have
     * floats you should usually use methods that take floats (or return floats, if assigning the result to a float),
     * and likewise for doubles.
     * <br>
     * The technique for cosine approximation is mostly from
     * <a href="https://web.archive.org/web/20080228213915/http://devmaster.net/forums/showthread.php?t=5784">this archived DevMaster thread</a>,
     * with credit to "Nick". Changes have been made to accelerate wrapping from any double to the valid input range.
     * @param turns an angle as a fraction of a turn as a float, with 0.5 here equivalent to PI radians in {@link #cos(double)}
     * @return the cosine of the given angle, as a float between -1.0 and 1.0 (both inclusive)
     */
    public static float cos_(float turns)
    {
        turns = turns * 4f + 1f;
        final long floor = (turns >= 0.0 ? (long) turns : (long) turns - 1L) & -2L;
        turns -= floor;
        turns *= 2f - turns;
        return turns * (-0.775f - 0.225f * turns) * ((floor & 2L) - 1L);
    }

    /**
     * Close approximation of the frequently-used trigonometric method atan2, with higher precision than LibGDX's atan2
     * approximation. Maximum error is below 0.001 radians.
     * Takes y and x (in that unusual order) as doubles, and returns the angle from the origin to that point in radians.
     * It is about 5 times faster than {@link Math#atan2(double, double)} (roughly 17 ns instead of roughly 88 ns for
     * Math, though the computer was under some load during testing). It is almost identical in speed to LibGDX'
     * MathUtils approximation of the same method; MathUtils seems to have worse average error, though.
     * Credit to StackExchange user njuffa, who gave
     * <a href="https://math.stackexchange.com/a/1105038">this useful answer</a>. This method changed from an earlier
     * technique that was twice as fast but had very poor quality, enough to be visually noticeable.
     * @param y y-component of the point to find the angle towards; note the parameter order is unusual by convention
     * @param x x-component of the point to find the angle towards; note the parameter order is unusual by convention
     * @return the angle to the given point, in radians as a double
     */
    public static double atan2(final double y, final double x)
    {
        /*
a := min (|x|, |y|) / max (|x|, |y|)
s := a * a
r := ((-0.0464964749 * s + 0.15931422) * s - 0.327622764) * s * a + a
if |y| > |x| then r := 1.57079637 - r
if x < 0 then r := 3.14159274 - r
if y < 0 then r := -r
         */
        if(y == 0.0 && x >= 0.0) return 0.0;
        final double ax = Math.abs(x), ay = Math.abs(y);
        if(ax < ay)
        {
            final double a = ax / ay, s = a * a,
                    r = 1.57079637 - (((-0.0464964749 * s + 0.15931422) * s - 0.327622764) * s * a + a);
            return (x < 0.0) ? (y < 0.0) ? -3.14159274 + r : 3.14159274 - r : (y < 0.0) ? -r : r;
        }
        else {
            final double a = ay / ax, s = a * a,
                    r = (((-0.0464964749 * s + 0.15931422) * s - 0.327622764) * s * a + a);
            return (x < 0.0) ? (y < 0.0) ? -3.14159274 + r : 3.14159274 - r : (y < 0.0) ? -r : r;
        }
    }

    /**
     * Close approximation of the frequently-used trigonometric method atan2, with higher precision than LibGDX's atan2
     * approximation. Maximum error is below 0.001 radians.
     * Takes y and x (in that unusual order) as floats, and returns the angle from the origin to that point in radians.
     * It is about 5 times faster than {@link Math#atan2(double, double)} (roughly 17 ns instead of roughly 88 ns for
     * Math, though the computer was under some load during testing). It is almost identical in speed to LibGDX'
     * MathUtils approximation of the same method; MathUtils seems to have worse average error, though.
     * Credit to StackExchange user njuffa, who gave
     * <a href="https://math.stackexchange.com/a/1105038">this useful answer</a>. This method changed from an earlier
     * technique that was twice as fast but had very poor quality, enough to be visually noticeable.
     * @param y y-component of the point to find the angle towards; note the parameter order is unusual by convention
     * @param x x-component of the point to find the angle towards; note the parameter order is unusual by convention
     * @return the angle to the given point, in radians as a float
     */
    public static float atan2(final float y, final float x)
    {
        /*
a := min (|x|, |y|) / max (|x|, |y|)
s := a * a
r := ((-0.0464964749 * s + 0.15931422) * s - 0.327622764) * s * a + a
if |y| > |x| then r := 1.57079637 - r
if x < 0 then r := 3.14159274 - r
if y < 0 then r := -r
         */
        if(y == 0f && x >= 0f) return 0f;
        final float ax = Math.abs(x), ay = Math.abs(y);
        if(ax < ay)
        {
            final float a = ax / ay, s = a * a,
                    r = 1.57079637f - (((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * a + a);
            return (x < 0f) ? (y < 0f) ? -3.14159274f + r : 3.14159274f - r : (y < 0f) ? -r : r;
        }
        else {
            final float a = ay / ax, s = a * a,
                    r = (((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * a + a);
            return (x < 0f) ? (y < 0f) ? -3.14159274f + r : 3.14159274f - r : (y < 0f) ? -r : r;
        }
    }
    /**
     * Arc sine approximation with fairly low error while still being faster than {@link NumberTools#sin(double)}.
     * This formula is number 201 in <a href=">http://www.fastcode.dk/fastcodeproject/articles/index.htm">Dennis
     * Kjaer Christensen's unfinished math work on arc sine approximation</a>. This method is about 40 times faster
     * than {@link Math#asin(double)}.
     * @param a an input to the inverse sine function, from -1 to 1 inclusive (error is higher approaching -1 or 1)
     * @return an output from the inverse sine function, from -PI/2 to PI/2 inclusive.
     */
    public static double asinAlt(double a) {
        return (a * (1.0 + (a *= a) * (-0.141514171442891431 + a * -0.719110791477959357))) /
                (1.0 + a * (-0.439110389941411144 + a * -0.471306172023844527));
    }
    /**
     * Arc sine approximation with fairly low error while still being faster than {@link NumberTools#sin(float)}.
     * This formula is number 201 in <a href=">http://www.fastcode.dk/fastcodeproject/articles/index.htm">Dennis
     * Kjaer Christensen's unfinished math work on arc sine approximation</a>. This method is about 40 times faster
     * than {@link Math#asin(double)}, and takes and returns a float.
     * @param a an input to the inverse sine function, from -1 to 1 inclusive (error is higher approaching -1 or 1)
     * @return an output from the inverse sine function, from -PI/2 to PI/2 inclusive.
     */
    public static float asinAlt(float a) {
        return (a * (1f + (a *= a) * (-0.141514171442891431f + a * -0.719110791477959357f))) /
                (1f + a * (-0.439110389941411144f + a * -0.471306172023844527f));
    }
    /**
     * Arc sine approximation with very low error, based on a simplified version of {@link #atan2(double, double)}.
     * This method is usually much faster than {@link Math#asin(double)}, but is somewhat less precise than Math's
     * implementation). It is currently more precise than libGDX's approximation in their MathUtils, but this isn't
     * quite as fast; the difference in precision is hard to spot but can be noticeable in some usage.
     * @param n an input to the inverse sine function, from -1 to 1 inclusive
     * @return an output from the inverse sine function, from PI/-2.0 to PI/2.0 inclusive.
     */
    public static float asin(final float n)
    {
        final float ax = (float) Math.sqrt(1f - n * n), ay = Math.abs(n);
        if(ax < ay)
        {
            final float a = ax / ay, s = a * a,
                    r = 1.57079637f - (((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * a + a);
            return (n < 0f) ? -r : r;
        }
        else {
            final float a = ay / ax, s = a * a,
                    r = (((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * a + a);
            return (n < 0f) ? -r : r;
        }
    }

    /**
     * Arc cosine approximation with fairly low error while still being faster than {@link NumberTools#cos(double)}.
     * This formula is number 201 in <a href=">http://www.fastcode.dk/fastcodeproject/articles/index.htm">Dennis
     * Kjaer Christensen's unfinished math work on arc sine approximation</a>, with a basic change to go from arc sine
     * to arc cosine. This method is faster than {@link Math#acos(double)}.
     * @param a an input to the inverse cosine function, from -1 to 1 inclusive (error is higher approaching -1 or 1)
     * @return an output from the inverse cosine function, from 0 to PI inclusive.
     */
    public static double acosAlt(double a) {
        return 1.5707963267948966 - (a * (1.0 + (a *= a) * (-0.141514171442891431 + a * -0.719110791477959357))) /
                (1.0 + a * (-0.439110389941411144 + a * -0.471306172023844527));
    }

    /**
     * Arc sine approximation with very low error, based on a simplified version of {@link #atan2(float, float)}.
     * This method is usually much faster than {@link Math#asin(double)}, but is somewhat less precise than Math's
     * implementation). It is currently more precise than libGDX's approximation in their MathUtils, but this isn't
     * quite as fast; the difference in precision is hard to spot but can be noticeable in some usage.
     * @param n an input to the inverse sine function, from -1 to 1 inclusive
     * @return an output from the inverse sine function, from PI/-2.0 to PI/2.0 inclusive.
     */
    public static double asin(final double n)
    {
        final double ax = Math.sqrt(1.0 - n * n), ay = Math.abs(n);
        if(ax < ay)
        {
            final double a = ax / ay, s = a * a,
                    r = 1.57079637 - (((-0.0464964749 * s + 0.15931422) * s - 0.327622764) * s * a + a);
            return (n < 0.0) ? -r : r;
        }
        else {
            final double a = ay / ax, s = a * a,
                    r = (((-0.0464964749 * s + 0.15931422) * s - 0.327622764) * s * a + a);
            return (n < 0.0) ? -r : r;
        }
    }
    /**
     * Arc cosine approximation with very low error, based on a simplified version of {@link #atan2(double, double)}.
     * This method is usually much faster than {@link Math#acos(double)}, but is somewhat less precise than Math's
     * implementation). It is currently more precise than libGDX's approximation in their MathUtils, but this isn't
     * quite as fast; the difference in precision is hard to spot but can be noticeable in some usage.
     * @param n an input to the inverse cosine function, from -1 to 1 inclusive
     * @return an output from the inverse cosine function, from 0 to PI inclusive.
     */
    public static double acos(final double n)
    {
        final double ax = Math.abs(n), ay = Math.sqrt(1.0 - n * n);
        if(ax < ay)
        {
            final double a = ax / ay, s = a * a,
                    r = 1.57079637 - (((-0.0464964749 * s + 0.15931422) * s - 0.327622764) * s * a + a);
            return (n < 0.0) ? Math.PI - r : r;
        }
        else {
            final double a = ay / ax, s = a * a,
                    r = (((-0.0464964749 * s + 0.15931422) * s - 0.327622764) * s * a + a);
            return (n < 0.0) ? Math.PI - r : r;
        }
    }

    /**
     * Arc cosine approximation with fairly low error while still being faster than {@link NumberTools#cos(float)}.
     * This formula is number 201 in <a href=">http://www.fastcode.dk/fastcodeproject/articles/index.htm">Dennis
     * Kjaer Christensen's unfinished math work on arc sine approximation</a>, with a basic change to go from arc sine
     * to arc cosine. This method is faster than {@link Math#acos(double)}, and takes and returns a float.
     * @param a an input to the inverse cosine function, from -1 to 1 inclusive (error is higher approaching -1 or 1)
     * @return an output from the inverse cosine function, from 0 to PI inclusive.
     */
    public static float acosAlt(float a) {
        return 1.5707963267948966f - (a * (1f + (a *= a) * (-0.141514171442891431f + a * -0.719110791477959357f))) /
                (1f + a * (-0.439110389941411144f + a * -0.471306172023844527f));
    }

    /**
     * Arc cosine approximation with very low error, based on a simplified version of {@link #atan2(float, float)}.
     * This method is usually much faster than {@link Math#acos(double)}, but is somewhat less precise than Math's
     * implementation). It is currently more precise than libGDX's approximation in their MathUtils, but this isn't
     * quite as fast; the difference in precision is hard to spot but can be noticeable in some usage.
     * @param n an input to the inverse cosine function, from -1 to 1 inclusive
     * @return an output from the inverse cosine function, from 0 to PI inclusive.
     */
    public static float acos(final float n)
    {
        final float ax = Math.abs(n), ay = (float) Math.sqrt(1f - n * n);
        if(ax < ay)
        {
            final float a = ax / ay, s = a * a,
                    r = 1.57079637f - (((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * a + a);
            return (n < 0f) ? 3.14159265358979323846f - r : r;
        }
        else {
            final float a = ay / ax, s = a * a,
                    r = (((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * a + a);
            return (n < 0.0) ? 3.14159265358979323846f - r : r;
        }
    }

    /**
     * Inverse sine function (arcsine) but with output measured in turns instead of radians. Possible results for this
     * range from 0.75 (inclusive) to 1.0 (exclusive), and continuing past that to 0.0 (inclusive) to 0.25 (inclusive).
     * <br>
     * This method is extremely similar to the non-turn approximation.
     * @param n a double from -1.0 to 1.0 (both inclusive), usually the output of sin() or cos()
     * @return one of the values that would produce {@code n} if it were passed to {@link #sin_(double)}
     */
    public static double asin_(final double n)
    {
        if(n == 0.0) return 0.0;
        final double ax = Math.sqrt(1.0 - n * n), ay = Math.abs(n);
        if(ax < ay)
        {
            final double a = ax / ay, s = a * a,
                    r = 0.25 - (((-0.0464964749 * s + 0.15931422) * s - 0.327622764) * s * a + a) * 0.15915494309189535;
            return (n < 0.0) ? 1.0 - r : r;
        }
        else {
            final double a = ay / ax, s = a * a,
                    r = (((-0.0464964749 * s + 0.15931422) * s - 0.327622764) * s * a + a) * 0.15915494309189535;
            return (n < 0.0) ? 1.0 - r : r;
        }
    }
    /**
     * Inverse cosine function (arccos) but with output measured in turns instead of radians. Possible results for this
     * range from 0.0 (inclusive) to 0.5 (inclusive).
     * <br>
     * This method is extremely similar to the non-turn approximation.
     * @param n a double from -1.0 to 1.0 (both inclusive), usually the output of sin() or cos()
     * @return one of the values that would produce {@code n} if it were passed to {@link #cos_(double)}
     */
    public static double acos_(final double n)
    {
        final double ax = Math.abs(n), ay = Math.sqrt(1.0 - n * n);
        if(ax < ay)
        {
            final double a = ax / ay, s = a * a,
                    r = 0.25 - (((-0.0464964749 * s + 0.15931422) * s - 0.327622764) * s * a + a) * 0.15915494309189535;
            return (n < 0.0) ? 0.5 - r : r;
        }
        else {
            final double a = ay / ax, s = a * a,
                    r = (((-0.0464964749 * s + 0.15931422) * s - 0.327622764) * s * a + a) * 0.15915494309189535;
            return (n < 0.0) ? 0.5 - r : r;
        }
    }


    /**
     * Inverse sine function (arcsine) but with output measured in turns instead of radians. Possible results for this
     * range from 0.75f (inclusive) to 1.0f (exclusive), and continuing past that to 0.0f (inclusive) to 0.25f
     * (inclusive).
     * <br>
     * This method is extremely similar to the non-turn approximation.
     * @param n a float from -1.0f to 1.0f (both inclusive), usually the output of sin() or cos()
     * @return one of the values that would produce {@code n} if it were passed to {@link #sin_(float)}
     */
    public static float asin_(final float n)
    {
        if(n == 0.0f) return 0.0f;
        final float ax = (float) Math.sqrt(1f - n * n), ay = Math.abs(n);
        if(ax < ay)
        {
            final float a = ax / ay, s = a * a,
                    r = 0.25f - (((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * a + a) * 0.15915494309189535f;
            return (n < 0.0f) ? 1.0f - r : r;
        }
        else {
            final float a = ay / ax, s = a * a,
                    r = (((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * a + a) * 0.15915494309189535f;
            return (n < 0.0f) ? 1.0f - r : r;
        }
    }
    /**
     * Inverse cosine function (arccos) but with output measured in turns instead of radians. Possible results for this
     * range from 0.0f (inclusive) to 0.5f (inclusive).
     * <br>
     * This method is extremely similar to the non-turn approximation.
     * @param n a float from -1.0f to 1.0f (both inclusive), usually the output of sin() or cos()
     * @return one of the values that would produce {@code n} if it were passed to {@link #cos_(float)}
     */
    public static float acos_(final float n)
    {
        final float ax = Math.abs(n), ay = (float) Math.sqrt(1f - n * n);
        if(ax < ay)
        {
            final float a = ax / ay, s = a * a,
                    r = 0.25f - (((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * a + a) * 0.15915494309189535f;
            return (n < 0.0f) ? 0.5f - r : r;
        }
        else {
            final float a = ay / ax, s = a * a,
                    r = (((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * a + a) * 0.15915494309189535f;
            return (n < 0.0f) ? 0.5f - r : r;
        }
    }

    /**
     * Sine-quadrilateral, for doing operations like trigonometric sine but on a square. Angles are in quarter-turns
     * where a full rotation takes 4 quarters. The square here has a "radius" of 1, so really a side length of 2, and is
     * centered on (0,0). Giving the angle (in quarters) 0 to cosq and sinq gives the point (1,1). This method can range
     * between -1.0 and 1.0, inclusive.
     * @param quarters quarter-turn measurement for the angle
     * @return sine-like double between -1.0 and 1.0
     */
    public static double sinq(double quarters)
    {
        int floor = (--quarters >= 0f ? (int) quarters : (int) quarters - 1);
        quarters -= floor;
        floor &= 3;
        return (Integer.bitCount(floor ^ floor >>> 1) - 1 << 1) * quarters + 1 - (floor & 2);
    }

    /**
     * Cosine-quadrilateral, for doing operations like trigonometric cosine but on a square. Angles are in quarter-turns
     * where a full rotation takes 4 quarters. The square here has a "radius" of 1, so really a side length of 2, and is
     * centered on (0,0). Giving the angle (in quarters) 0 to cosq and sinq gives the point (1,1). This method can range
     * between -1.0 and 1.0, inclusive.
     * @param quarters quarter-turn measurement for the angle
     * @return cosine-like double between -1.0 and 1.0
     */
    public static double cosq(double quarters)
    {
        int floor = (quarters >= 0f ? (int) quarters : (int) quarters - 1);
        quarters -= floor;
        return (Integer.bitCount((floor & 3) ^ (floor >>> 1 & 1)) - 1 << 1) * quarters + 1 - (floor & 2);
    }

    /**
     * Sine-quadrilateral, for doing operations like trigonometric sine but on a square. Angles are in quarter-turns
     * where a full rotation takes 4 quarters. The square here has a "radius" of 1, so really a side length of 2, and is
     * centered on (0,0). Giving the angle (in quarters) 0 to cosq and sinq gives the point (1,1). This method can range
     * between -1.0 and 1.0, inclusive.
     * @param quarters quarter-turn measurement for the angle
     * @return sine-like float between -1.0 and 1.0
     */
    public static float sinq(float quarters)
    {
        int floor = (--quarters >= 0f ? (int) quarters : (int) quarters - 1);
        quarters -= floor;
        floor &= 3;
        return (Integer.bitCount(floor ^ floor >>> 1) - 1 << 1) * quarters + 1 - (floor & 2);
    }

    /**
     * Cosine-quadrilateral, for doing operations like trigonometric cosine but on a square. Angles are in quarter-turns
     * where a full rotation takes 4 quarters. The square here has a "radius" of 1, so really a side length of 2, and is
     * centered on (0,0). Giving the angle (in quarters) 0 to cosq and sinq gives the point (1,1). This method can range
     * between -1.0 and 1.0, inclusive.
     * @param quarters quarter-turn measurement for the angle
     * @return cosine-like float between -1.0 and 1.0
     */
    public static float cosq(float quarters)
    {
        int floor = (quarters >= 0f ? (int) quarters : (int) quarters - 1);
        quarters -= floor;
        return (Integer.bitCount((floor & 3) ^ (floor >>> 1 & 1)) - 1 << 1) * quarters + 1 - (floor & 2);
    }

}
