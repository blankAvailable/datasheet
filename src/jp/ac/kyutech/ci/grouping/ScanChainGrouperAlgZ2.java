package jp.ac.kyutech.ci.grouping;

import java.util.Random;

/**
 * Created by Zhang on 2017/5/11.
 */
public class ScanChainGrouperAlgZ2 extends ScanChainGrouper {
    private FastCostFunction cost;
    private int threshold = 0;

    public ScanChainGrouperAlgZ2(int threshold){
        this.threshold = threshold;
    }

    public int[] calculateClocking(int clockCount){
        Random r = new Random();
        int[] clocksFlag = new int[clockCount];

        int clocking[] = new int[chains.size()];
        for (int i = 0; i < chains.size(); i++)
            clocking[i] = -1;
        clocking[0] = r.nextInt(clockCount);
        clocksFlag[clocking[0]] = 1;

        for (int i = 1; i < chains.size(); i++){
            if (availableGroupExist(clocksFlag)){
                for (int clkIdx = 0; clkIdx < clockCount; clkIdx++){
                    if (clocksFlag[clkIdx] == 1){
                        
                    }
                }
            }else {

            }
        }

        return clocking;
    }

    private boolean availableGroupExist(int[] clocksFlag){
        boolean exist = true;
        int flagSum = 0;

        for (int i = 0; i < clocksFlag.length; i++)
            flagSum += clocksFlag[i];

        if (flagSum >= clocksFlag.length)
            exist = false;

        return exist;
    }
}
