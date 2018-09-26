package sarong;

import sarong.util.StringKit;

/**
 * A slightly-faster variant on {@link Mover64RNG}; replaces one of Mover64RNG's two "CMR" generators with a "CERS"
 * generator, which doesn't require multiplication and may have a longer period than an arbitrary "CMR" generator. The
 * speed difference is very small (3.8 ns per long for Mover64, 3.7 ns for Overdrive64), and likely varies between
 * platforms and architectures, but the quality on Overdrive64RNG is actually rather good as well. It passes at least
 * 1TB of PractRand with no anomalies (tests are ongoing), and there's lots of room for modifications if issues are
 * found. The period on this is unknown, but at least 2 to the 40 at the bare minimum, and probably higher than 2 to the
 * 64 (by a substantial factor). An Overdrive64RNG has two states, each updated independently. One is a CMR generator,
 * which multiplies its state by a constant and then bitwise-rotates it by a fixed amount; this has a known period of
 * less than 2 to the 64 but more than 2 to the 41. The other is a CERS generator, which sets its state to a constant
 * minus a bitwise rotation of its previous state; this has a known period of less than 2 to the 64 but more than 2 to
 * the 42. It is unknown what the longest cycle's period is for either component generator, so it can't be known if the 
 * two generators' periods share common factors (reducing the total). They probably share at least some.
 * <br>
 * The CMR and CERS generators, among many other useful and efficient subcycle generators, were discovered and described
 * by Mark Overton, hence the name Overdrive here. See <a href="http://www.drdobbs.com/tools/229625477">this article</a>
 * for more on his subcycle generators and links to his TestU01 code.
 * <br>
 * This file was created by Tommy Ettinger on 9/13/2018.  This variant was created by Tommy Ettinger on 9/25/2018.
 * @author Mark Overton
 * @author Tommy Ettinger
 */
public final class Overdrive64RNG implements RandomnessSource {
    private long stateA, stateB;
    public Overdrive64RNG()
    {
        setState((int)((Math.random() * 2.0 - 1.0) * 0x80000000));
    }
    public Overdrive64RNG(final int state)
    {
        setState(state);
    }

    /**
     * Not advised for external use; prefer {@link #Overdrive64RNG(int)} because it guarantees a good subcycle. This
     * constructor allows all subcycles to be produced, including ones with a shorter period.
     * @param stateA the state for the CMR generator; if 0 is given, this will use 1
     * @param stateB the state for the CERS generator; will not be modified
     */
    public Overdrive64RNG(final long stateA, final long stateB)
    {
        this.stateA = stateA == 0L ? 1L : stateA;
        this.stateB = stateB;
    }

    private static final long[] startingA = {
            0x0000000000000001L, 0x770391C6587202CDL, 0x0148D0D6B055DE19L, 0x2CFFDEE4C4C8654FL, 0x3669291DE218F372L, 0x5B744ACB07F3D380L, 0x103F93C86BDF21D0L, 0x9A1D980831BCF2ABL,
            0x92D56961736A4B50L, 0x71A9832527530EADL, 0x4C524342889BCFE1L, 0xF39CFA3D37AB4038L, 0xA3E9A70AD8EEF84DL, 0x65C7AFEFFC4DA898L, 0x4D455E304CDC7741L, 0xA6EDACBD6740B1A7L,
            0xAA7F8E77C41AF5EBL, 0x96B50AD6E4AA2B18L, 0x77432395B55EDFD9L, 0x2748C2DD4565F1F0L, 0x3CAC2CDB2F8318D0L, 0x7D983C0295175158L, 0xDCFC33F629C3D00FL, 0x1EF0C5B47164F981L,
            0x3AB9A3877956251EL, 0xBA230F415A833533L, 0xA489CC2EF532A6BEL, 0xB212F25D09BFC366L, 0xB9014F210A77310DL, 0x8590EF3967A8C6E0L, 0x1011FD4E97B1F81AL, 0x1C57F18A80F4C131L,
            0x4AA90F013DB975E3L, 0xB3FAAC7A9374BD99L, 0xA15B9AA709431B2DL, 0xD3201A4C3953FFA2L, 0x0B34634F0B74BAB5L, 0x501389102E6E08EEL, 0xFCAC8A7CFCEB534DL, 0xA6A1A2C7CB5CEE8FL,
            0x5F461436431B3D6DL, 0x1F3DE41F1E991A39L, 0x96A5BD1D16EDC265L, 0xAEC3F8C461FA0749L, 0x4445933104846C0BL, 0xAD088B25A4AA0E59L, 0x862FCA81FF8B1BE5L, 0x12E5A795C17DA902L,
            0x5CA3CDC494DF2B93L, 0xCF612FCBDD25B41EL, 0xAD0CC4406EC6FCC3L, 0x352336C92FA965EAL, 0x675AB684694EE4A8L, 0x811A5D47EE8B3568L, 0x4937A92A07C372A4L, 0xE1483C680A24BEA4L,
            0x1B3E829B910E343CL, 0x0F5F8EF159F931C0L, 0x7F5DDFDA98EFE7EAL, 0xA2FA4A6C79F5C6EFL, 0xEA416C98A2A0945CL, 0x29CC34E89FCC5D02L, 0x157FC5094CCC1795L, 0x27252C1165C6E255L,
            0xAB963445C144A9BCL, 0x601530DECC304F69L, 0xC92D8F3257316572L, 0xC348074025724519L, 0x0F8305789523701EL, 0xD288EFE7BDDABF47L, 0xC428DA0AD18149BAL, 0xBA1D19D35E61A11EL,
            0x6D81979DC0110FA2L, 0x3C144A6DC2C2982BL, 0x7593425EA77652A8L, 0xBA416F84332EFD0AL, 0x691EAA02B1351B41L, 0xF1B15F5AD69A16BAL, 0x026D58B160B39D4CL, 0x813B48A15DA161E6L,
            0xCC92B59765EF4C5FL, 0x46B6C1ED44BF6877L, 0xA679D47C27EA4A03L, 0x393BEF21C904261CL, 0xE40A734EFE039992L, 0xD114E560A35EC443L, 0x85A46B901B80F546L, 0xCC8C80C6AB27F53CL,
            0xC9B5FCE7C3EE4A83L, 0x64D4B2A2A91ADC11L, 0x7157576E65940314L, 0x75BF0B0737304143L, 0x4A11300B7F32C8C5L, 0x4B4FB70D7701DD60L, 0xE877F97BEC9E8FACL, 0x151E431374EF9D79L,
            0x636214B0856DF427L, 0x088F1774DE7730CFL, 0x9E3B5CD7FF590F81L, 0x4DA157EA25850BC1L, 0xE9C7C31744E062F4L, 0x4767FCAF076B9508L, 0xC5C767D939AC8425L, 0x1ABAF0D4EC698A8FL,
            0x5035DC94FA971B81L, 0x718EE38E931713E2L, 0x497DB43133CEF0F2L, 0xF01BE721B0145805L, 0x9D6239853FF80744L, 0x256B893D4DD0689AL, 0x256647CAA07563D6L, 0xCE4087F877A6D24FL,
            0x68A0537869364FE2L, 0x32BA732DEC14AE42L, 0x3AAF6CDE0CE8DB48L, 0x552C1D9594CE212AL, 0x8BC1A33AE250B2E9L, 0xC02FCB678B465D00L, 0x496F580658AFD50AL, 0x6D0D982E45AD15A9L,
            0xC8E87307F336E8D0L, 0x257E726598418548L, 0xFADF2ED10B13D148L, 0x46FA6CC74F293535L, 0xF03227995C268856L, 0x46087E39622EA4CDL, 0x17EE09D3D2181207L, 0x9C7518A1E5AD4544L,
    }, startingB = {
            0x0000000000000001L, 0x481693B00042FFE5L, 0x632C899FFF5DFFE5L, 0xF5B98965A06CFFE5L, 0x639A896599B1E0A6L, 0x639B895961B9E0A6L, 0x603889684F11E0A6L, 0xB1B3C1BF8B19E0A6L,
            0x6E57E1BF8B45AEB3L, 0xAE54C1BAD171C34DL, 0xEE57A9C2DDF946DBL, 0x2E57D946DDF8E52DL, 0x6E57C5FAD59D7424L, 0xAE57CAFAD6CD2C64L, 0xEE2113FAD3A0FF60L, 0x054113E7B3A2524AL,
            0xC4E113E7B4404A7AL, 0xC28113E7B456227AL, 0x48A113E7B527FABCL, 0x9AC113C1F396867AL, 0x9295D0A1F487B7EFL, 0xBBC1F0A2B9FEAEDDL, 0x80F04CFE429EC37FL, 0x021C4B3E3EFEC37EL,
            0x4361E57BB99EC2E9L, 0x40667DC2CCB0BAE9L, 0x4066460B2FB712E9L, 0x40AE45EAE2C50AE9L, 0x3EF165332C0EA2E9L, 0x3B9F79133CB1A7E5L, 0xFD9D7CD24987D5D9L, 0x463612DFDB708738L,
            0x465612DCD9407769L, 0x20F612DCEAE30F69L, 0x21827645F6E32769L, 0x23169DF822E31F69L, 0x214A55085C9E9769L, 0x214A46012479AF69L, 0x2149B55044780769L, 0x25352543E47AFF69L,
            0x2E75EB0E2A8B63F5L, 0x20F1F6F02A8F6331L, 0x2221F75A20028E31L, 0x2121977E74F00D31L, 0xC6FB977B74E1D6A3L, 0x323377C4F7A7B041L, 0x569CD7C4F888C0BCL, 0x568D37C4FD6CC0BCL,
            0x567E18B89240C0BCL, 0x727FAD189584C0BCL, 0xA67FACF3C218C0BCL, 0xA27FACF3C27CC0BCL, 0x767F897E7130C0BCL, 0x7A8489860634C078L, 0x7AEED8E86AB5D673L, 0xAEEEAAC25D2AA552L,
            0xB28EF3CABC7BF552L, 0xDE8EF2887F2C6553L, 0xE28EF2C1D02C6573L, 0x6E8EF33DE12C6574L, 0x528EF4473A2C6570L, 0x7E8EF447332C6330L, 0x828EF33B6F2C616BL, 0x4E8EF033A32C37FAL,
            0x91016C7BA33DB91FL, 0x38788703A492486CL, 0x6B031C2742FF8A60L, 0x69EF01274D606160L, 0x696701272D604860L, 0x5D3E99808D604A10L, 0x173E99806D60466BL, 0x0473017FCD60494EL,
            0x32233235E111071DL, 0x2842B077C2C2EDA5L, 0x2890A47664CF8DA5L, 0x5C90D0830ACFAD97L, 0x7090D3A1ECCFAD8AL, 0xC490DDB492CFAD8AL, 0xBAD8DA31B4CFAD70L, 0xEED831A21B16F5C8L,
            0xB439340A368F234DL, 0xB438B40A38872389L, 0xB5B24CD4EEF8779DL, 0x2D3F3BF3B5B27770L, 0xEE85B613AF3C76EEL, 0xC685B65AB179F96BL, 0x5E85B69D2195D5DEL, 0xB685CC578E044EABL,
            0xCE85CC5EC652E246L, 0x7B4EF89AD9C384B4L, 0xE80F989AD9E74E07L, 0xE7FDB8A754E74E01L, 0x06ADB52103E74E08L, 0x969D49F06A81BE0DL, 0xA9FC2EEE27C0FE0DL, 0xAA0E60415ADDBE0DL,
            0xD96744762A3CFE0DL, 0x665D444E2A489865L, 0x9B5D43E62A4F07EEL, 0x2EAEBA9E2A4797ABL, 0x049EBA562A20B4B4L, 0x5571D3FA823C10B2L, 0x5571A85DAE4ED074L, 0xF66BEC5B5C4ED089L,
            0x966B7C233C8A4005L, 0x75C6F89FC1803461L, 0x35C5209FD330EC76L, 0x95C994C42E8E07FBL, 0x75C994C6E92E095EL, 0x55C925EFF64E0A25L, 0x05CED909FA44C825L, 0xB5CED0B20619CF17L,
            0xE2B09F1A064A4EEFL, 0x4B709F2D4249A198L, 0x0AB09F2D42416671L, 0xC3709F2C30816663L, 0xA2B09F9FAD120ACFL, 0x8C00D5A3AD10EE56L, 0x8E688E47AD126CB6L, 0x8DA5CA13AD17F816L,
            0x5296A7CFAD17DD0AL, 0x9297C7CBAD17E2F1L, 0xD297C7D628821414L, 0x1297B799A9704862L, 0xCF475DFBDC65C9A1L, 0xFC4E7831DC418B21L, 0xFB1D3672E46018FDL, 0x3F0D0E42E4601661L,
    };
    
    public final void setState(final int s) {
        stateA = startingA[s >>> 9 & 0x7F];
        for (int i = s & 0x1FF; i > 0; i--) {
            stateA *= 0x41C64E6BL;
            stateA = (stateA << 28 | stateA >>> 36);
        }
        stateB = startingB[s >>> 25];
        for (int i = s >>> 16 & 0x1FF; i > 0; i--) {
//            stateB *= 0x9E3779B9L;
//            stateB = (stateB << 37 | stateB >>> 27);
            stateB = 0xC6BC279692B5CC8BL - (stateB << 45 | stateB >>> 19);
        }
    }

    public final int nextInt()
    {
//        final long a = stateA * 0x41C64E6BL;
//        final long b = stateB * 0x9E3779B9L;
//        return (int)((stateA = (a << 26 | a >>> 38)) + (stateB = (b << 37 | b >>> 27)));
        final long a = stateA * 0x41C64E6BL;
        return (int)((stateB = 0xC6BC279692B5CC8BL - (stateB << 45 | stateB >>> 19)) ^ (stateA = (a << 28 | a >>> 36)));
    }
    @Override
    public final int next(final int bits)
    {
//        final long a = stateA * 0x41C64E6BL;
//        final long b = stateB * 0x9E3779B9L;
//        return (int)((stateA = (a << 26 | a >>> 38)) + (stateB = (b << 37 | b >>> 27))) >>> (32 - bits);
        final long a = stateA * 0x41C64E6BL;
        return (int)((stateB = 0xC6BC279692B5CC8BL - (stateB << 45 | stateB >>> 19)) ^ (stateA = (a << 28 | a >>> 36))) >>> (32 - bits);
    }
    @Override
    public final long nextLong()
    {
        //0x9E3779B97F4A7C15L
//        final long a = stateA * 0x41C64E6BL;
//        final long b = stateB * 0x9E3779B9L;
//        return (stateA = (a << 26 | a >>> 38)) ^ (stateB = (b << 37 | b >>> 27));
        final long a = stateA * 0x41C64E6BL;
        return (stateB = 0xC6BC279692B5CC8BL - (stateB << 45 | stateB >>> 19)) ^ (stateA = (a << 28 | a >>> 36));
    }

//    public final long nextLong1()
//    {
//        final long a = stateA * 0x41C64E6BL;
//        final long b = stateB * 0x9E3779B9L;
//        return (stateA = (a << 26 | a >>> 38)) ^ (stateB = (b << 37 | b >>> 27));
//    }
//    
//    public final long nextLong2()
//    {
////        return (stateA = (stateA << 26 | stateA >>> 38) * 0x41C64E6BL)
////                ^ (stateB = (stateB << 37 | stateB >>> 27) * 0x9E3779B9L);
////        return (stateB = 0xC6BC279692B5CC8BL - (stateB << 45 | stateB >>> 19))
////                ^ (stateA = (stateA << 28 | stateA >>> 36) * 0x41C64E6BL);
//        final long a = stateA * 0x41C64E6BL;
//        final long b = stateB;
//        return (stateA = (a << 28 | a >>> 36)) ^
//                (stateB = 0xC6BC279692B5CC8BL - (b << 45 | b >>> 19));
//
//    }
//    
//    public final long nextLong3()
//    {
////        final long a = stateA * 0x41C64E6BL;
////        //final long b = stateB * 0x9E3779B9L;
////        return (stateB += 0x9E3779B97F4A7C15L) ^ (stateA = (a << 26 | a >>> 38));
////        return (stateA = (a << 43 | a >>> 21) - a)
////                ^ (stateB = 0xC6BC279692B5CC8BL - (b << 45 | b >>> 19));
//        final long a = stateA * 0x41C64E6BL;
//        return (stateB = 0xC6BC279692B5CC8BL - (stateB << 45 | stateB >>> 19)) ^ (stateA = (a << 28 | a >>> 36));
//
//        //return stateA ^ stateB;
//
//    }
//
//    public final long nextLong4()
//    {
//        final long a = stateA * 0x41C64E6BL;
//        return (stateB = 0xC6BC279692B5CC83L - (stateB << 39 | stateB >>> 25)) ^ (stateA = (a << 28 | a >>> 36));
//    }

    /**
     * Produces a copy of this Overdrive64RNG that, if next() and/or nextLong() are called on this object and the
     * copy, both will generate the same sequence of random numbers from the point copy() was called. This just need to
     * copy the state so it isn't shared, usually, and produce a new value with the same exact state.
     *
     * @return a copy of this Overdrive64RNG
     */
    @Override
    public Overdrive64RNG copy() {
        return new Overdrive64RNG(stateA, stateB);
    }

    /**
     * Gets the "A" part of the state; if this generator was set with {@link #Overdrive64RNG()}, {@link #Overdrive64RNG(int)},
     * or {@link #setState(int)}, then this will (probably) be on the optimal subcycle, otherwise it may not be. 
     * @return the "A" part of the state, an int
     */
    public long getStateA()
    {
        return stateA;
    }

    /**
     * Gets the "B" part of the state; if this generator was set with {@link #Overdrive64RNG()}, {@link #Overdrive64RNG(int)},
     * or {@link #setState(int)}, then this will (probably) be on the optimal subcycle, otherwise it may not be. 
     * @return the "B" part of the state, an int
     */
    public long getStateB()
    {
        return stateB;
    }
    /**
     * Sets the "A" part of the state to any long, which may put the generator in a low-period subcycle.
     * Use {@link #setState(int)} to guarantee a good subcycle.
     * @param stateA any int
     */
    public void setStateA(final long stateA)
    {
        this.stateA = (stateA == 0L ? 1L : stateA);
    }

    /**
     * Sets the "B" part of the state to any long, which may put the generator in a low-period subcycle.
     * Use {@link #setState(int)} to guarantee a good subcycle.
     * @param stateB any int
     */
    public void setStateB(final long stateB)
    {
        this.stateB = stateB;
    }
    
    @Override
    public String toString() {
        return "Overdrive64RNG with stateA 0x" + StringKit.hex(stateA) + " and stateB 0x" + StringKit.hex(stateB);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Overdrive64RNG overdrive64RNG = (Overdrive64RNG) o;

        return stateA == overdrive64RNG.stateA && stateB == overdrive64RNG.stateB;
    }

    @Override
    public int hashCode() { 
        long h = 31L * stateA + stateB;
        return (int)(h ^ h >>> 32);
    }

//    public static void main(String[] args)
//    {
//        // A 10 0xC010AEB4
//        // B 22 0x195B9108
//        // all  0x04C194F3485D5A68
//
//        // A 17 0xF7F87D28
//        // B 14 0xF023E25B 
//        // all  0xE89BB7902049CD38
//
//
//        // A11 B14 0xBBDA9763B6CA318D
//        // A8  B14 0xC109F954C76CB09C
//        // A17 B14 0xE89BB7902049CD38
////        BigInteger result = BigInteger.valueOf(0xF7F87D28L);
////        BigInteger tmp = BigInteger.valueOf(0xF023E25BL);
////        result = tmp.divide(result.gcd(tmp)).multiply(result);
////        System.out.printf("0x%016X\n", result.longValue());
//        int stateA = 1, i = 0;
//        for (; ; i++) {
//            if((stateA = Integer.rotateLeft(stateA * 0x9E37, 17)) == 1)
//            {
//                System.out.printf("0x%08X\n", i);
//                break;
//            }
//        }
//        BigInteger result = BigInteger.valueOf(i & 0xFFFFFFFFL);
//        i = 0;
//        for (; ; i++) {
//            if((stateA = Integer.rotateLeft(stateA * 0x4E6D, 14)) == 1)
//            {
//                System.out.printf("0x%08X\n", i);
//                break;
//            }
//        }         
//        BigInteger tmp = BigInteger.valueOf(i & 0xFFFFFFFFL);
//        result = tmp.divide(result.gcd(tmp)).multiply(result);
//        System.out.printf("\n0x%016X\n", result.longValue());
//
//    }

//    public static void main(String[] args)
//    {
//        Mover32RNG m = new Mover32RNG();
//        System.out.println("int[] startingA = {");
//        for (int i = 0, ctr = 0; ctr < 128; ctr++, i+= 0x00000200) {
//            m.setState(i);
//            System.out.printf("0x%08X, ", m.stateA);
//            if((ctr & 7) == 7)
//                System.out.println();
//        }
//        System.out.println("}, startingB = {");
//        for (int i = 0, ctr = 0; ctr < 128; ctr++, i+= 0x02000000) {
//            m.setState(i);
//            System.out.printf("0x%08X, ", m.stateB);
//            if((ctr & 7) == 7)
//                System.out.println();
//        }
//        System.out.println("};");
//    }
    
///////// BEGIN subcycle finder code and period evaluator
//    public static void main(String[] args)
//    {
//        // multiplying
//        // A refers to 0x9E377
//        // A 10 0xC010AEB4
//        // B refers to 0x64E6D
//        // B 22 0x195B9108
//        // all  0x04C194F3485D5A68
//
//        // A=Integer.rotateLeft(A*0x9E377, 17) 0xF7F87D28
//        // B=Integer.rotateLeft(A*0x64E6D, 14) 0xF023E25B 
//        // all  0xE89BB7902049CD38
//
//
//        // A11 B14 0xBBDA9763B6CA318D
//        // A8  B14 0xC109F954C76CB09C
//        // A17 B14 0xE89BB7902049CD38
////        BigInteger result = BigInteger.valueOf(0xF7F87D28L);
////        BigInteger tmp = BigInteger.valueOf(0xF023E25BL);
////        result = tmp.divide(result.gcd(tmp)).multiply(result);
////        System.out.printf("0x%016X\n", result.longValue());
//        // 0x9E37
//        // rotation 27: 0xEE06F34D
//        // 0x9E35
//        // rotation 6 : 0xE1183C3A
//        // rotation 19: 0xC4FCFC55
//        // 0x9E3B
//        // rotation 25: 0xE69313ED
//        // 0xDE4D
//        // rotation 3 : 0xF6C16607
//        // rotation 23: 0xD23AD58D
//        // rotation 29: 0xC56DC41F
//        // 0x1337
//        // rotation 7: 0xF41BD009
//        // rotation 20: 0xF5846878
//        // rotation 25: 0xF38658F9
//        // 0xACED
//        // rotation 28: 0xFC98CC08
//        // rotation 31: 0xFA18CD57
//        // 0xBA55
//        // rotation 19: 0xFB059E43
//        // 0xC6D5
//        // rotation 05: 0xFFD78FD4
//        // 0x5995
//        // rotation 28: 0xFF4AB87D
//        // rotation 02: 0xFF2AA5D5
//        // 0xA3A9
//        // rotation 09: 0xFF6B3AF7
//        // 0xB9EF
//        // rotation 23: 0xFFAEB037
//        // 0x3D29
//        // rotation 04: 0xFF6B92C5
//        // 0x5FAB
//        // rotation 09: 0xFF7E3277 // seems to be very composite
//        // 0xCB7F
//        // rotation 01: 0xFF7F28FE
//        // 0x89A7
//        // rotation 13: 0xFFFDBF50 // wow! note that this is a multiple of 16
//        // 0xBCFD
//        // rotation 17: 0xFFF43787 // second-highest yet, also an odd number
//        // 0xA01B
//        // rotation 28: 0xFFEDA0B5
//        // 0xC2B9
//        // rotation 16: 0xFFEA9001
//        
//        
//        // adding
//        // 0x9E3779B9
//        // rotation 2 : 0xFFCC8933
//        // rotation 7 : 0xF715CEDF
//        // rotation 25: 0xF715CEDF
//        // rotation 30: 0xFFCC8933
//        // 0x6C8E9CF5
//        // rotation 6 : 0xF721971A
//        // 0x41C64E6D
//        // rotation 13: 0xFA312DBF
//        // rotation 19: 0xFA312DBF
//        // rotation 1 : 0xF945B8A7
//        // rotation 31: 0xF945B8A7
//        // 0xC3564E95
//        // rotation 1 : 0xFA69E895 also 31
//        // rotation 5 : 0xF2BF5E23 also 27
//        // 0x76BAF5E3
//        // rotation 14: 0xF4DDFC5A also 18
//        // 0xA67943A3 
//        // rotation 11: 0xF1044048 also 21
//        // 0x6C96FEE7
//        // rotation 2 : 0xF4098F0D
//        // 0xA3014337
//        // rotation 15: 0xF3700ABF also 17
//        // 0x9E3759B9
//        // rotation 1 : 0xFB6547A2 also 31
//        // 0x6C8E9CF7
//        // rotation 7 : 0xFF151D74 also 25
//        // rotation 13: 0xFD468E2B also 19
//        // rotation 6 : 0xF145A7EB also 26
//        // 0xB531A935
//        // rotation 13: 0xFF9E2F67 also 19
//        // 0xC0EF50EB
//        // rotation 07: 0xFFF8A98D also 25
//        // 0x518DC14F
//        // rotation 09: 0xFFABD755 also 23 // probably not prime
//        // 0xA5F152BF
//        // rotation 07: 0xFFB234B2 also 27
//        // 0x8092D909
//        // rotation 10: 0xFFA82F7C also 22
//        // 0x73E2CCAB
//        // rotation 09: 0xFF9DE8B1 also 23
//        // stateB = rotate32(stateB + 0xB531A935, 13)
//        // stateC = rotate32(stateC + 0xC0EF50EB, 7)
//
//        // subtracting, rotating, and bitwise NOT:
//        // 0xC68E9CF3
//        // rotation 13: 0xFEF97E17, also 19 
//        // 0xC68E9CB7
//        // rotation 12: 0xFE3D7A2E
//
//        // left xorshift
//        // 5
//        // rotation 15: 0xFFF7E000
//        // 13
//        // rotation 17: 0xFFFD8000
//
//        // minus left shift, then xor
//        // state - (state << 12) ^ 0xC68E9CB7, rotation 21: 0xFFD299CB
//        // add xor
//        // state + 0xC68E9CB7 ^ 0xDFF4ECB9, rotation 30: 0xFFDAEDF7
//        // state + 0xC68E9CB7 ^ 0xB5402ED7, rotation 01: 0xFFE73631
//        // state + 0xC68E9CB7 ^ 0xB2B386E5, rotation 24: 0xFFE29F5D
//        // sub xor
//        // state - 0x9E3779B9 ^ 0xE541440F, rotation 22: 0xFFFC9E3E
//
//
//        // best power of two:
//        // can get 63.999691 with: (period is 0xFFF1F6F18B2A1330)
//        // multiplying A by 0x89A7 and rotating left by 13
//        // multiplying B by 0xBCFD and rotating left by 17
//        // can get 63.998159 with: (period is 0xFFAC703E2B6B1A30)
//        // multiplying A by 0x89A7 and rotating left by 13
//        // multiplying B by 0xB9EF and rotating left by 23
//        // can get 63.998 with:
//        // adding 0x9E3779B9 for A and rotating left by 2
//        // xorshifting B left by 5 (B ^ B << 5) and rotating left by 15
//        // can get 63.99 with:
//        // adding 0x9E3779B9 for A and rotating left by 2
//        // adding 0x6C8E9CF7 for B and rotating left by 7
//        // can get 63.98 with:
//        // adding 0x9E3779B9 for A and rotating left by 2
//        // multiplying by 0xACED, NOTing, and rotating left by 28 for B
//        // 0xFF6B3AF7L 0xFFAEB037L 0xFFD78FD4L
//        
//        // 0xFF42E24AF92DCD8C, 63.995831
//        //BigInteger result = BigInteger.valueOf(0xFF6B3AF7L), tmp = BigInteger.valueOf(0xFFD78FD4L);
//
//        BigInteger result = BigInteger.valueOf(0xFFFDBF50L), tmp = BigInteger.valueOf(0xFFF43787L);
//        result = tmp.divide(result.gcd(tmp)).multiply(result);
//        tmp = BigInteger.valueOf(0xFFEDA0B5L);
//        result = tmp.divide(result.gcd(tmp)).multiply(result);
//        System.out.printf("\n0x%s, %2.6f\n", result.toString(16).toUpperCase(), Math.log(result.doubleValue()) / Math.log(2));
////        tmp = BigInteger.valueOf(0xFFABD755L);
////        result = tmp.divide(result.gcd(tmp)).multiply(result);
////        System.out.printf("\n0x%s, %2.6f\n", result.toString(16).toUpperCase(), Math.log(result.doubleValue()) / Math.log(2));
//        int stateA = 1, i;
//        LinnormRNG lin = new LinnormRNG();
//        System.out.println(lin.getState());
//        Random rand = new RNG(lin).asRandom();
//        for (int c = 1; c <= 200; c++) {
//            //final int r = (Light32RNG.determine(20007 + c) & 0xFFFF)|1;
//            final int r = BigInteger.probablePrime(20, rand).intValue();
//            //System.out.printf("(x ^ x << %d) + 0xC68E9CB7\n", c);
//            System.out.printf("%03d/200, testing r = 0x%08X\n", c, r);
//            for (int j = 1; j < 32; j++) {
//                i = 0;
//                for (; ; i++) {
//                    if ((stateA = Integer.rotateLeft(stateA * r, j)) == 1) {
//                        if (i >>> 24 == 0xFF)
//                            System.out.printf("(state * 0x%08X, rotation %02d: 0x%08X\n", r, j, i);
//                        break;
//                    }
//                }
//            }
//        }
//
////        int stateA = 1, i = 0;
////        for (; ; i++) {
////            if((stateA = Integer.rotateLeft(~(stateA * 0x9E37), 7)) == 1)
////            {
////                System.out.printf("0x%08X\n", i);
////                break;
////            }
////        }
////        BigInteger result = BigInteger.valueOf(i & 0xFFFFFFFFL);
////        i = 0;
////        for (; ; i++) {
////            if((stateA = Integer.rotateLeft(~(stateA * 0x4E6D), 17)) == 1)
////            {
////                System.out.printf("0x%08X\n", i);
////                break;
////            }
////        }         
////        BigInteger tmp = BigInteger.valueOf(i & 0xFFFFFFFFL);
////        result = tmp.divide(result.gcd(tmp)).multiply(result);
////        System.out.printf("\n0x%016X\n", result.longValue());
//
//    }
///////// END subcycle finder code and period evaluator
    
    
//    public static void main(String[] args)
//    {
//        long stateA = 1, stateB = 1;
//        System.out.println("long[] startingA = {");
//        for (int ctr = 0; ctr < 128; ctr++) {
//            System.out.printf("0x%016XL, ", stateA);
//            if((ctr & 7) == 7)
//                System.out.println();
//            for (int i = 0; i < 512; i++) {
//                stateA *= 0x41C64E6BL;
//                stateA = (stateA << 28 | stateA >>> 36);
//            }
//        }
//        System.out.println("}, startingB = {");
//        for (int ctr = 0; ctr < 128; ctr++) {
//            System.out.printf("0x%016XL, ", stateB);
//            if((ctr & 7) == 7)
//                System.out.println();
//            for (int i = 0; i < 512; i++) {
////                stateB += 0x9E3779B97F4A7C15L;
////                stateB *= 0x9E3779B9L;
////                stateB = (stateB << 37 | stateB >>> 27);
//                stateB = 0xC6BC279692B5CC8BL - (stateB << 45 | stateB >>> 19);
//            }
//        }
//        System.out.println("};");
//    }
}
