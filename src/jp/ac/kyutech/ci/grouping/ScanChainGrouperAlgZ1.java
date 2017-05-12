package jp.ac.kyutech.ci.grouping;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ScanChainGrouperAlgZ1 extends ScanChainGrouper {
	private static final int RANDOM_CASES = 64;
	private FastCostFunction cost;

	public int[] calculateClocking(int clockCount) {
		int maxCost = Integer.MAX_VALUE;

		int clocking[] = new int[chains.size()];
		int tempClocking[] = new int[chains.size()];

		cost = new FastCostFunction(chain2impactSet, cell2aggressorSet);

		for (int counter=0; counter<RANDOM_CASES; counter++){
			tempClocking = randomGrouping(tempClocking, clockCount);
			if (cost.evaluate(tempClocking, clockCount) < maxCost) {
				maxCost = cost.evaluate(tempClocking, clockCount);
				System.arraycopy(tempClocking, 0, clocking, 0, clocking.length);
				counter = counter/2;
			}
		}

		log.info("  Best grouping from random partition " + arrayToList(clocking, clockCount));
		log.info("  Best cost from random partition " + maxCost);

		for (int counter=0; counter<16; counter++) {
            if (switchGroup(clocking, clockCount, findWorstChain(clocking, clockCount),maxCost))
                break;
        }

        log.info("  Best grouping from optimize " + arrayToList(clocking, clockCount));
		return clocking;
	}

	private boolean switchGroup(int[] clocking, int clockCount, int chainIdx, int maxCost){
	    boolean stopper = false;
	    int bestClkIdx = 0;
	    int[] tempCost = new int[clockCount];
	    tempCost[clocking[chainIdx]] = maxCost;
	    for (int clkIdx=0; clkIdx<clockCount; clkIdx++){
	        if (clkIdx == clocking[chainIdx])
	            continue;
	        clocking[chainIdx] = clkIdx;
	        tempCost[clkIdx] =  cost.evaluate(clocking, clockCount);
        }
        int tempMin = 0;
        for (int clkIdx=0; clkIdx<clockCount; clkIdx++){
            tempMin = tempCost[0];
            if (tempCost[clkIdx] < tempMin) {
                bestClkIdx = clkIdx;
                clocking[chainIdx] = bestClkIdx;
            }
        }

        log.info("  Switch chain " + chainIdx + " to group " + bestClkIdx + " get highest gain");
        log.info("  Grouping now is " + arrayToList(clocking, clockCount));
        if (bestClkIdx == clocking[chainIdx]) {
            log.info("  ");
            stopper = true;
        }
	    return stopper;
    }

    private int findWorstChain(int[] clocking, int clockCount) {
        int worst_chainIdx = -1;
        int highest_cost_diff = 0;
        int base_cost = cost.evaluate(clocking, clockCount);
        for (int chain_idx = 0; chain_idx < clocking.length; chain_idx++) {
            int clk = clocking[chain_idx];
            clocking[chain_idx] = -1;
            int cost_diff = base_cost - cost.evaluate(clocking, clockCount);
            clocking[chain_idx] = clk;
            if (cost_diff > highest_cost_diff) {
                worst_chainIdx = chain_idx;
                highest_cost_diff = cost_diff;
            }

        }
        log.debug("Worst chain " + worst_chainIdx + " with diff " + highest_cost_diff);
        return worst_chainIdx;
    }

	private int[] randomGrouping(int[] clocking, int clockCount){
		Random random = new Random();
		for (int clkIdx=0; clkIdx<clocking.length; clkIdx++){
			clocking[clkIdx] = random.nextInt(clockCount);
		}
		return clocking;
	}

	private List<List<Integer>> arrayToList (int[] clocking, int clockCount){
	    List<List<Integer>> scGrouping = new ArrayList<>();
        for (int clkIdx=0; clkIdx<clockCount; clkIdx++){
            List<Integer> oneGroup = new ArrayList<>();
            scGrouping.add(oneGroup);
        }

	    for (int chainIdx=0; chainIdx<clocking.length; chainIdx++)
	        scGrouping.get(clocking[chainIdx]).add(chainIdx);
	    return scGrouping;
    }
}