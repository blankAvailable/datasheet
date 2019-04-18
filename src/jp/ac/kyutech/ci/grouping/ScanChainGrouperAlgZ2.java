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
    private int threshold = 0;

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
        threshold = cost.evaluate(thrClocking, chains.size());
        log.info("Lower bound: " + threshold);

        if (clockCount >= chains.size()){
            return thrClocking;
        }else {
            Random r = new Random();
            int[] clocking = new int[chains.size()];

            int[] costlog = new int[chains.size()];
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
                                if (cost.evaluate_usable(clocking, clockCount, threshold)) {
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
                        int tempMinCost = Integer.MAX_VALUE;
                        int tempBestGroup = -1;
                        for (int clkIdx = 0; clkIdx < clockCount; clkIdx++) {
                            clocking[chainIdx] = clkIdx;
                            if (tempMinCost > cost.evaluate(clocking, clockCount)) {
                                tempMinCost = cost.evaluate(clocking, clockCount);
                                tempBestGroup = clkIdx;
                            }
                        }
                        log.info("The most suitable group is " + tempBestGroup);
                        clocking[chainIdx] = tempBestGroup;
                        clocksFlag[tempBestGroup] = 1;
                    }
                }
                costlog[initialchain] = cost.evaluate(clocking, clockCount);
                log.info("check cost: " + costlog[initialchain]);
            }

            int sumcost = 0;
            int maxcost = 0;
            int mincost = Integer.MAX_VALUE;
            for (int chainIdx = 0; chainIdx < costlog.length; chainIdx++) {
                sumcost += costlog[chainIdx];
                if (costlog[chainIdx] > maxcost)
                    maxcost = costlog[chainIdx];
                if (costlog[chainIdx] < mincost)
                    mincost = costlog[chainIdx];
            }

            log.info("AverageCost: " + sumcost/costlog.length);
            log.info("MaxCost: " + maxcost);
            log.info(("MinCost: " + mincost));

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
