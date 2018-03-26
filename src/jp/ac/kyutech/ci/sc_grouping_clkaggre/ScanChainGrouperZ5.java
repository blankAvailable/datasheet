package jp.ac.kyutech.ci.sc_grouping_clkaggre;

import java.util.Arrays;
import java.util.Random;

public class ScanChainGrouperZ5 extends ScanChainGrouper {
    private int threshold = 0;

    public ScanChainGrouperZ5(int threshold){
        this.threshold = threshold;
    }
    @Override
    public int[] calculateClocking(int groupCount, FastCostFunction cost) {
        Random r = new Random();
        int[] clocksFlag = new int[groupCount];

        int[] clocking = new int[chainSize];
        for (int i = 0; i < chainSize; i++)
            clocking[i] = -1;
        clocking[0] = r.nextInt(groupCount);
        clocksFlag[clocking[0]] = 1;

        for (int chainIdx = 1; chainIdx < chainSize; chainIdx++) {

            System.out.println("Clocking " + Arrays.toString(clocking).replaceAll("\\[", "").
                    replaceAll("\\]", "").replaceAll(",", ""));

            if (availableGroupExist(clocksFlag)) {
                for (int clkIdx = 0; clkIdx < groupCount; clkIdx++) {
                    if (clocksFlag[clkIdx] == 1) {
                        clocking[chainIdx] = clkIdx;
                        if (cost.evaluate_usable(clocking, groupCount, threshold)) {
                            log.info("group " + clkIdx + " is reusable");
                            break;
                        }
                        clocking[chainIdx] = -1;
                    }
                }
                if (clocking[chainIdx] == -1) {
                    int i;
                    do {
                        i = r.nextInt(groupCount);
                        clocking[chainIdx] = i;
                    } while (clocksFlag[i] == 1);
                    log.info("randomly assign a available group " + i);
                    clocksFlag[i] = 1;
                }
            } else {
                int tempMinCost = Integer.MAX_VALUE;
                int tempBestGroup = -1;
                for (int clkIdx = 0; clkIdx < groupCount; clkIdx++) {
                    clocking[chainIdx] = clkIdx;
                    if (tempMinCost > cost.evaluate(clocking, groupCount)) {
                        tempMinCost = cost.evaluate(clocking, groupCount);
                        tempBestGroup = clkIdx;
                    }
                }
                log.info("The most suitable group is " + tempBestGroup);
                clocking[chainIdx] = tempBestGroup;
                clocksFlag[tempBestGroup] = 1;
            }
            SwapWorstChian(clocking, clocksFlag, groupCount, clocking[chainIdx], cost);
        }
        log.info("ConflictCounter: " + cost.evaluate_counter(clocking, groupCount, threshold));

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

    private void SwapWorstChian(int[] clocking, int[]clocksFlag, int groupCount, int currentGroup, FastCostFunction cost){
        int worstChain = -1;
        int bestGroup = 0;
        int highestCostImprove = 0;
        int baseCost = cost.evaluate(clocking, groupCount);
        for (int chainId = 0; chainId < clocking.length; chainId++){
            int group = clocking[chainId];
            if (group == -1)
                break;
            for (int groupId = 0; groupId < groupCount; groupId++){
                if (clocksFlag[groupId] == 0)
                    continue;
                clocking[chainId] = groupId;
                int cost_diff = baseCost - cost.evaluate(clocking, groupCount);
                clocking[chainId] = group;
                if (cost_diff >= highestCostImprove){
                    worstChain = chainId;
                    highestCostImprove = cost_diff;
                    bestGroup = groupId;
                }
            }
        }
        if (highestCostImprove > 0) {
            log.debug("Worst chain " + worstChain + " changed from group " + clocking[worstChain] + " to "
                    + bestGroup + " with the cost improved " + highestCostImprove);
            clocking[worstChain] = bestGroup;
        }else {
            log.debug("No chain was moved as no improvement could be made");
        }
    }
}
