package jp.ac.kyutech.ci.grouping;

import java.util.Arrays;
import java.util.Random;

/**
 * Created by Zhang on 2017/5/11.
 */
public class ScanChainGrouperAlgZ2 extends ScanChainGrouper {
    private Random r = new Random();
    private FastCostFunction cost;
    private int threshold = 0;

    public ScanChainGrouperAlgZ2(){

    }

    public int[] calculateClocking(int clockCount){
        if (cost == null){
            cost = new FastCostFunction(chain2impactSet, cell2aggressorSet);
            log.info("FastCostFunction initialized");
        }
        int[] thrClocking = new  int[chains.size()];
        for (int i = 0; i < thrClocking.length; i++)
            thrClocking[i] = i;
        threshold = cost.evaluate(thrClocking, chains.size());
        log.info("Lower bound: " + threshold);

        Random r = new Random();
        int[] clocksFlag = new int[clockCount];

        int[] clocking = new int[chains.size()];
        for (int i = 0; i < chains.size(); i++)
            clocking[i] = -1;
        clocking[0] = r.nextInt(clockCount);
        clocksFlag[clocking[0]] = 1;

        for (int chainIdx = 1; chainIdx < chains.size(); chainIdx++){

            System.out.println("Clocking " + Arrays.toString(clocking).replaceAll("\\[", "").replaceAll("\\]", "")
                    .replaceAll(",", ""));

            if (availableGroupExist(clocksFlag)){
                for (int clkIdx = 0; clkIdx < clockCount; clkIdx++){
                    if (clocksFlag[clkIdx] == 1){
                        clocking[chainIdx] = clkIdx;
                        if (cost.evaluate_usable(clocking, clockCount, threshold)) {
                            log.info("group " + clkIdx + " is reusable");
                            break;
                        }
                        clocking[chainIdx] = -1;
                    }
                }
                if (clocking[chainIdx] == -1){
                    int i;
                    do{
                        i = r.nextInt(clockCount);
                        clocking[chainIdx] = i;
                    }while(clocksFlag[i] == 1);
                    log.info("randomly assign a available group " + i);
                    clocksFlag[i] = 1;
                }
            }else {
                int tempMinCost = Integer.MAX_VALUE;
                int tempBestGroup = -1;
                for (int clkIdx = 0; clkIdx < clockCount; clkIdx++){
                    clocking[chainIdx] = clkIdx;
                    if (tempMinCost > cost.evaluate(clocking, clockCount)){
                        tempMinCost = cost.evaluate(clocking, clockCount);
                        tempBestGroup = clkIdx;
                    }
                }
                log.info("The most suitable group is " + tempBestGroup);
                clocking[chainIdx] = tempBestGroup;
                clocksFlag[tempBestGroup] = 1;
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

        log.info("Available group exist? " + exist);
        return exist;
    }
}
