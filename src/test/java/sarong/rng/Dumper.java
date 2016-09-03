package sarong.rng;

import sarong.util.CrossHash;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Created by Tommy Ettinger on 8/24/2016.
 */
public class Dumper {
    public static void blast(String filename, RNG[] r)
    {
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new FileOutputStream("target/" + filename + ".dat", false));

            for (int i = 0; i < 64; i++) {
                for (int j = 0; j < 0x20000; j++) {
                    dos.writeLong(r[62].nextLong());
                }
            }
            dos.flush();
            dos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void blastInt(String filename, RNG[] r)
    {
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new FileOutputStream("target/" + filename + ".dat", false));

            for (int i = 0; i < 64; i++) {
                for (int j = 0; j < 0x40000; j++) {
                    dos.writeInt(r[62].next(32));
                }
            }
            dos.flush();
            dos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static long[] seeds = new long[64];
    public static void main(String[] args)
    {
        seeds[0] = 0;
        seeds[1] = 3;
        seeds[2] = -1;
        seeds[3] = 31;
        seeds[4] = -31;
        seeds[5] = 1;
        seeds[6] = Long.MAX_VALUE;
        seeds[7] = Long.MIN_VALUE;
        for (int i = 8; i < 64; i++) {
            seeds[i] = CrossHash.hash64(seeds);
        }
        RNG[] rs = new RNG[64];
        for (int i = 0; i < 64; i++) {
            rs[i] = new RNG(new ThunderRNG(seeds[i], seeds[(i + 12) & 63]));
        }
        System.out.println(seeds[62]);
        blast("Thunder", rs);
        /*
        for (int i = 0; i < 64; i++) {
            rs[i] = new RNG(new LightRNG(seeds[i]));
        }
        blast("Light", rs);
        for (int i = 0; i < 64; i++) {
            rs[i] = new RNG(new XoRoRNG(seeds[i]));
        }
        blast("XoRo", rs);
        for (int i = 0; i < 64; i++) {
            rs[i] = new RNG(new PermutedRNG(seeds[i]));
        }
        blast("Permuted", rs);
        for (int i = 0; i < 64; i++) {
            rs[i] = new RNG(new LongPeriodRNG(seeds[i]));
        }
        blast("LongPeriod", rs);
        */

        /*
        DataOutputStream dos = null;
        Random jre = new Random(seeds[62]);
        try {
            dos = new DataOutputStream(new FileOutputStream("target/JRE.dat", false));

            for (int i = 0; i < 256; i++) {
                for (int j = 0; j < 0x20000; j++) {
                    dos.writeInt(jre.nextInt());
                }
            }
            dos.flush();
            dos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
        DataOutputStream dos = null;
        Random jre = new Random(seeds[62]);
        /*
        try {
            dos = new DataOutputStream(new FileOutputStream("target/JRE.dat", false));

            for (int i = 0; i < 128; i++) {
                for (int j = 0; j < 0x20000; j++) {
                    dos.writeLong(jre.nextLong());
                }
            }
            dos.flush();
            dos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }
}
