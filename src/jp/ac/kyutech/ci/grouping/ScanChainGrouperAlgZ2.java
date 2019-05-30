package jp.ac.kyutech.ci.grouping;

import java.util.Arrays;
import java.util.Random;

/**
 * Created by Zhang on 2017/5/11.
 * heuristic greedy
 */
public class ScanChainGrouperAlgZ2 extends ScanChainGrouper {
    private Random r = new Random();
    private FastCostFunction cost;
    private float threshold = 0;

    public ScanChainGrouperAlgZ2(){

    }

    public int[] calculateClocking(int clockCount){
        if (cost == null){
            cost = new FastCostFunction(chain2impactSet, cell2aggressorSet, row_height, placement);
            log.info("FastCostFunction initialized");
        }
        int[] thrClocking = new  int[chains.size()];
        for (int i = 0; i < thrClocking.length; i++)
            thrClocking[i] = i;
        threshold = cost.evaluate_float(thrClocking, chains.size());
        log.info("Lower bound: " + threshold);

        if (clockCount >= chains.size()){
            return thrClocking;
        }else {
            Random r = new Random();
            int[] clocking = new int[chains.size()];

            float[] costlog = new float[chains.size()];
            for (int initialchain = 0; initialchain < chains.size(); initialchain++) {

                //initial clocksflag
                int[] clocksFlag = new int[clockCount];

                //initial clocking
                for (int i = 0; i < chains.size(); i++)
                    clocking[i] = -1;

                clocking[initialchain] = r.nextInt(clockCount);
                clocksFlag[clocking[initialchain]] = 1;

                for (int chainIdx = 0; chainIdx < chains.size(); chainIdx++) {

                    if (clocking[chainIdx] != -1)
                        continue;
                    System.out.println("Clocking " + Arrays.toString(clocking).replaceAll("\\[", "").replaceAll("\\]", "")
                            .replaceAll(",", ""));

                    if (availableGroupExist(clocksFlag)) {
                        for (int clkIdx = 0; clkIdx < clockCount; clkIdx++) {
                            if (clocksFlag[clkIdx] == 1) {
                                clocking[chainIdx] = clkIdx;
                                if (cost.evaluate_float(clocking, clockCount) < threshold || Math.abs(cost.evaluate_float(clocking, clockCount) - threshold) < 0.001) {
                                    log.info("group " + clkIdx + " is reusable");
                                    break;
                                }
                                clocking[chainIdx] = -1;
                            }
                        }
                        if (clocking[chainIdx] == -1) {
                            int i;
                            do {
                                i = r.nextInt(clockCount);
                                clocking[chainIdx] = i;
                            } while (clocksFlag[i] == 1);
                            log.info("randomly assign a available group " + i);
                            clocksFlag[i] = 1;
                        }
                    } else {
                        float tempMinCost = Float.MAX_VALUE;
                        int tempBestGroup = -1;
                        for (int clkIdx = 0; clkIdx < clockCount; clkIdx++) {
                            clocking[chainIdx] = clkIdx;
                            if (tempMinCost > cost.evaluate_float(clocking, clockCount)) {
                                tempMinCost = cost.evaluate_float(clocking, clockCount);
                                tempBestGroup = clkIdx;
                            }
                        }
                        log.info("The most suitable group is " + tempBestGroup);
                        clocking[chainIdx] = tempBestGroup;
                        clocksFlag[tempBestGroup] = 1;
                    }
                }
                costlog[initialchain] = cost.evaluate_float(clocking, clockCount);
                log.info("check cost: " + costlog[initialchain]);
            }

            float sumcost = 0;
            float maxcost = 0;
            float mincost = Float.MAX_VALUE;
            for (int chainIdx = 0; chainIdx < costlog.length; chainIdx++) {
                sumcost += costlog[chainIdx];
                if (costlog[chainIdx] > maxcost)
                    maxcost = costlog[chainIdx];
                if (costlog[chainIdx] < mincost)
                    mincost = costlog[chainIdx];
            }

            log.info("AverageCost: " + sumcost/costlog.length);
            log.info("MaxCost: " + String.format("%.2f", maxcost));
            log.info("MinCost: " + String.format("%.2f", mincost));

            return clocking;
        }
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
