package sarong.discouraged;

import sarong.Mover64RNG;
import sarong.RandomnessSource;
import sarong.util.StringKit;

import java.io.Serializable;

/**
 * The fastest generator here that still passes statistical quality tests (and does so extremely well, to boot). This is
 * a slightly-faster variant on {@link Mover64RNG}; it replaces one of Mover64RNG's two "CMR" generators with a "CERS"
 * generator, which doesn't require multiplication and may have a longer period than an arbitrary "CMR" generator. The
 * speed difference is very small (3.8 ns per long for Mover64, 3.7 ns for Overdrive64), and likely varies between
 * platforms and architectures, but the quality on Overdrive64RNG is actually rather good as well. It passes all 32TB
 * of PractRand with no anomalies (a rare feat). The period on this is unknown, but at least 2 to the 47 at the bare
 * minimum, and probably higher than 2 to the 64 (by a substantial factor). An Overdrive64RNG has two states, each
 * updated independently. One is a CMR generator, which multiplies its state by a constant and then bitwise-rotates it
 * by a fixed amount; this has a known period of less than 2 to the 64 but more than 2 to the 47. The other is a CERS
 * generator, which sets its state to a constant minus a bitwise rotation of its previous state; this has a known period
 * of less than 2 to the 64 but more than 2 to the 47. CMR is considered to have very random output, while CERS is not
 * considered to have especially random output, and coupling the two seems to yield a high-quality generator. It is 
 * unknown what the longest cycle's period is for either component generator, so it can't be known if the two
 * generators' periods share common factors (reducing the total). They probably share at least some. It is possible with
 * some constructors and state-setting methods that you can cause one or both component generators to enter a low-period
 * cycle; to avoid such cycles, you should use {@link #Overdrive64RNG()} or {@link #Overdrive64RNG(int)} to construct,
 * or {@link #setState(int)} to set the state (although you can't retrieve it in the same format).
 * <br>
 * The CMR and CERS generators, among many other useful and efficient subcycle generators, were discovered and described
 * by Mark Overton, hence the name Overdrive here. See <a href="http://www.drdobbs.com/tools/229625477">this article</a>
 * for more on his subcycle generators and links to his TestU01 code.
 * <br>
 * This file was created by Tommy Ettinger on 9/13/2018.  This variant was created by Tommy Ettinger on 9/25/2018.
 * @author Mark Overton
 * @author Tommy Ettinger
 */
public final class Overdrive64RNG implements RandomnessSource, Serializable {
    private static final long serialVersionUID = 1L;
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
            0x0000000000000001L, 0xE400A8A8DFFCDBB8L, 0xD48F52BDA014F338L, 0x1B348EF2FB81A072L, 0x52953C3D77622AFCL, 0x22CEA1428F6FAAAAL, 0x11C4D45E3F37ACCDL, 0x709F5A366A542F61L,
            0xD093E219F7E32762L, 0xC2986214F61E4935L, 0x9841F301821A420DL, 0x8BC8F2F2B2D421DAL, 0x8819E1ED2169B636L, 0x019D281DB10747F3L, 0x018E1EA12FD10EFFL, 0xB5EDD9A1AF9852C8L,
            0x606393B52FB4FE4BL, 0xDD1AC582299E7B24L, 0xD6AAB23FC69E967AL, 0x594E61948FAB7E27L, 0x474EF8B0F7AB6E45L, 0x494D82A15FB1A8F5L, 0xFE4DC58A2A26636DL, 0x1CE41B69F88A436CL,
            0x1CDBAD85DF9D8DC8L, 0x268187ADA0F9843EL, 0xA6A9FD24B5F20971L, 0x43BD3DE92518DE85L, 0x08693D533C9E0253L, 0x39294885073F9B25L, 0x34694C33DC7B5E4DL, 0x2A9B79A87C7058FFL,
            0x8C8D0152100D5921L, 0xF19FAB4D1E387EE0L, 0xDBC2D53FDEC850E2L, 0x5BCA216BC9B1C99AL, 0xC409048CF9E1CC8BL, 0x025105B820B1CDBFL, 0xE0C57B5A27453EF5L, 0xE0E91F20508585E9L,
            0x928C7D3A4FD7C7A7L, 0xA562A6BDF1015D80L, 0x016294BA8883E09AL, 0xBFAB35783CC3DA97L, 0xBA4305AEBE03D91BL, 0xC6E306B12006310CL, 0x2F8305B416C912CEL, 0x1EC26EB303D9BDF2L,
            0x2EBF2DB4B3DAE292L, 0xDA532E18FA229293L, 0x053F869333A352B7L, 0x653FD7FF33D3171EL, 0x85A9E9FFDD180311L, 0xCB43C7B03489D26CL, 0x9F10A148350AD2CFL, 0xA71FF57039ABD697L,
            0x62A6809DC83FEE89L, 0x3BFE4C425850BA89L, 0x62D2B0C0AC1424A6L, 0x720F1FF7AD159B39L, 0x620FC06702156F39L, 0x69175E436E54D339L, 0x6A535CF4A6645739L, 0x272DAE2591C17FEFL,
            0xB5E32375F56F1FF7L, 0x15B3265CD7A0EE6EL, 0x426D4A9BADB7BC05L, 0xCF169B92AD877067L, 0xB212C6FE7575A4EDL, 0xCE42C49D2D7C7AEEL, 0x8492C339B59939DFL, 0x04DA5E053365C9B3L,
            0x6D2241057E3E7383L, 0x00C1286E2E3B37EFL, 0x00CB78687D54DF4BL, 0xFF29D9EAB6771277L, 0xF4B489E676CF73DFL, 0x1FC3C899124F900EL, 0x42A30B587EDD30D3L, 0xA6CC47B8042A6DF5L,
            0x6230D76E4D065860L, 0x1B6C922EA7A2583AL, 0xFFD35FAA261D09F9L, 0xD608602F4DC9A277L, 0x140C2A804A58DE38L, 0xB5EC5505D402DE8AL, 0x15B309B8FFAA896FL, 0x9A87391F605087B3L,
            0xA9CBB441389237B4L, 0x484BDC4B4E9CA56AL, 0xFE31344AA267873CL, 0x14F8333096E7A879L, 0x09F7505F5A67C77FL, 0x4CF79EE399E5073FL, 0x611EC9D57F153234L, 0xFC37D35F7CEC1FA1L,
            0xF54EBD813E05D901L, 0xA97E15B924E7500DL, 0x1581AA01C467A6AEL, 0xBDADD62D68E7A1C8L, 0x45AF8A8FAAC02002L, 0x5A517A8AAAC6A6ACL, 0x4E710A9437BA1F46L, 0x5541D562BF1294D9L,
            0x269D548125D1B97AL, 0xD056D48B6F668605L, 0x45E64E1AA85D7B43L, 0x354FD09228C67B24L, 0x5E8FC612828FB3BAL, 0x69CFCB52C50BC09AL, 0xBA2DD0CD530A2A8FL, 0x0854F25414696819L,
            0x9F4578D0D43735CCL, 0x5F5FE60CFA3EE2F4L, 0x0B4BE5B17A413AF7L, 0x5B3FE34D820740A7L, 0x8D977D1830B404A6L, 0xD1FCA3BAD54E3953L, 0x0F69F02CE2902FA4L, 0x0849A89D71201C15L,
            0x44F39E5C979007F3L, 0x7E8D24FF09FE44F3L, 0x4160C07D0F111A35L, 0x76718DD168041ACFL, 0xAF5D3F916FE3CC23L, 0x24B3DDF4EFBF834AL, 0x5E1689A56F9F58D0L, 0x3251CFCB89D7BF0AL,
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
            stateB = 0xC6BC279692B5CC8BL - (stateB << 35 | stateB >>> 29);
        }
    }

    public final int nextInt()
    {
//        final long a = stateA * 0x41C64E6BL;
//        final long b = stateB * 0x9E3779B9L;
//        return (int)((stateA = (a << 26 | a >>> 38)) + (stateB = (b << 37 | b >>> 27)));
        final long a = stateA * 0x41C64E6BL;
        return (int)((stateB = 0xC6BC279692B5CC8BL - (stateB << 35 | stateB >>> 29)) ^ (stateA = (a << 28 | a >>> 36)));
    }
    @Override
    public final int next(final int bits)
    {
//        final long a = stateA * 0x41C64E6BL;
//        final long b = stateB * 0x9E3779B9L;
//        return (int)((stateA = (a << 26 | a >>> 38)) + (stateB = (b << 37 | b >>> 27))) >>> (32 - bits);
        final long a = stateA * 0x41C64E6BL;
        return (int)((stateB = 0xC6BC279692B5CC8BL - (stateB << 35 | stateB >>> 29)) ^ (stateA = (a << 28 | a >>> 36))) >>> (32 - bits);
    }
    @Override
    public final long nextLong()
    {
        //0x9E3779B97F4A7C15L
//        final long a = stateA * 0x41C64E6BL;
//        final long b = stateB * 0x9E3779B9L;
//        return (stateA = (a << 26 | a >>> 38)) ^ (stateB = (b << 37 | b >>> 27));
        final long a = stateA * 0x41C64E6BL;
        return (stateB = 0xC6BC279692B5CC8BL - (stateB << 35 | stateB >>> 29)) ^ (stateA = (a << 28 | a >>> 36));
//        final long a = stateA * 0x41C64E6BL;
//        final long b = stateB;
//        stateA = (a << 28 | a >>> 36);
//        stateB = 0xC6BC279692B5CC8BL - (b << 35 | b >>> 29);
//        return a ^ b;
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
////        return (stateB = 0xC6BC279692B5CC8BL - (stateB << 35 | stateB >>> 29))
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
//        return (stateB = 0xC6BC279692B5CC8BL - (stateB << 35 | stateB >>> 29)) ^ (stateA = (a << 28 | a >>> 36));
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
//                stateB = 0xC6BC279692B5CC8BL - (stateB << 35 | stateB >>> 29);
//            }
//        }
//        System.out.println("};");
//    }
}
