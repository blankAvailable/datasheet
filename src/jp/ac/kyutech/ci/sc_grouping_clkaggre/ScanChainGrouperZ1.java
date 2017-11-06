package jp.ac.kyutech.ci.sc_grouping_clkaggre;

import java.util.Arrays;
import java.util.Random;

public class ScanChainGrouperZ1 extends ScanChainGrouper {

    private static final int RANDOM_TIMEOUT = 32;

    private FastCostFunction cost;

    public int[] calculateClocking(int groupCount) {

        if (cost == null){
            cost = new FastCostFunction(chain2impactSet, cell2aggressorSet);
            log.info("Setup finished.");
        }

        int clocking[] = new int[chains.size()];

        ScanChainGrouping randGrouping = new RandomGrouping(chains.size(), groupCount, 4);

        int cand_clking[];
        int cand_cost = Integer.MAX_VALUE;
        int randTries = 0;
        while (randTries < RANDOM_TIMEOUT){
            randTries++;
            cand_clking = randGrouping.next();
            int this_cost = cost.evaluate(cand_clking, groupCount);
            // print grouping info and grouping cost
            log.info("Clocking " + Arrays.toString(cand_clking).replaceAll("\\[", "").replaceAll("\\]", "")
                    .replaceAll(",", ""));
            log.info("cost of cand_clocking " + this_cost);
            if (this_cost < cand_cost){
                System.arraycopy(cand_clking, 0, clocking, 0, clocking.length);
                cand_cost = this_cost;
                log.info("Better guess " + this_cost + " found after " + randTries + " tries");
                randTries = 0;
            }
        }

        log.info("Best after random search: " + cand_cost);

        for (int i = 0; i< 128; i++){
            int diff = SwapWorstChain(clocking, groupCount);
            if (diff <= 0)
                break;
        }

        log.info("Cost after optimizing: " + cost.evaluate(clocking, groupCount));

        return clocking;
    }

    private int SwapWorstChain(int[] clocking, int clockCount){
        int worstChain = -1;
        int highest_cost_diff = 0;
        int base_cost = cost.evaluate(clocking, clockCount);
        for (int chainId = 0; chainId < clocking.length; chainId++){
            int group = clocking[chainId];
            for (int groupId = 0; groupId < clockCount; groupId++){
                clocking[chainId] = groupId;
                int cost_diff = base_cost - cost.evaluate(clocking, clockCount);
                clocking[chainId] = group;
                if (cost_diff > highest_cost_diff){
                    worstChain = chainId;
                    highest_cost_diff = cost_diff;
                }
            }
        }
        log.debug("Worst chain " + worstChain + " with diff " + highest_cost_diff);
        return highest_cost_diff;
    }


}
