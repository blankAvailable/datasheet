package jp.ac.kyutech.ci.grouping;

import java.util.Random;

public class ScanChainGrouperAlgS1 extends ScanChainGrouper {

	private static final int RANDOM_TIMEOUT = 50;

	private FastCostFunction cost;

	public int[] calculateClocking(int clockCount) {

		if (cost == null) {
			cost = new FastCostFunction(chain2impactSet, cell2aggressorSet);
			log.info("finished setup.");
		}

		int clocking[] = new int[chains.size()];

		Random r = new Random(42);

		int cand_clocking[] = new int[clocking.length];
		int cand_cost = Integer.MAX_VALUE;
		int random_tries = 0;
		while (random_tries < RANDOM_TIMEOUT) {
			random_tries++;
			for (int c = 0; c < clocking.length; c++) {
				cand_clocking[c] = r.nextInt(clockCount);
			}
			int this_cost = cost.evaluate(cand_clocking, clockCount);
			if (this_cost < cand_cost) {
				System.arraycopy(cand_clocking, 0, clocking, 0, clocking.length);
				cand_cost = this_cost;
				log.info("Better guess " + cand_cost + " found after " + random_tries + " tries.");
				random_tries = 0;
			}
		}

		log.info("Best after random search: " + cand_cost);

		for (int i = 0; i < 10; i++) {
			int chain = findWorstChain(clocking, clockCount);
			int diff = tweakChain(clocking, clockCount, chain);
			if (diff == 0)
				break;
		}

		log.info("Cost after optimizing: " + cost.evaluate(clocking, clockCount));

		return clocking;
	}

	private int findWorstChain(int[] clocking, int clockCount) {
		int worst_chain = -1;
		int highest_cost_diff = 0;
		int base_cost = cost.evaluate(clocking, clockCount);
		for (int chain_idx = 0; chain_idx < clocking.length; chain_idx++) {
			int clk = clocking[chain_idx];
			clocking[chain_idx] = -1;
			int cost_diff = base_cost - cost.evaluate(clocking, clockCount);
			clocking[chain_idx] = clk;
			if (cost_diff > highest_cost_diff) {
				worst_chain = chain_idx;
				highest_cost_diff = cost_diff;
			}

		}
		log.debug("Worst chain " + worst_chain + " with diff " + highest_cost_diff);
		return worst_chain;
	}

	private int tweakChain(int[] clocking, int clockCount, int chain_idx) {
		int old_clk = clocking[chain_idx];
		int best_clk = old_clk;
		int highest_cost_diff = 0;
		int base_cost = cost.evaluate(clocking, clockCount);
		for (int clock = 0; clock < clockCount; clock++) {
			clocking[chain_idx] = clock;
			int cost_diff = base_cost - cost.evaluate(clocking, clockCount);
			if (cost_diff > highest_cost_diff) {
				best_clk = clock;
				highest_cost_diff = cost_diff;
			}
		}
		clocking[chain_idx] = best_clk;

		log.debug("Chain " + chain_idx + " move from " + old_clk + " to " + best_clk + " gaining " + highest_cost_diff);
		return highest_cost_diff;
	}

}
