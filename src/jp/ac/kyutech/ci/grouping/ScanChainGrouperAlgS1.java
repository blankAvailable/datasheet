package jp.ac.kyutech.ci.grouping;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import org.kyupi.graph.Graph.Node;
import org.kyupi.graph.ScanChains.ScanCell;
import org.kyupi.graph.ScanChains.ScanChain;

public class ScanChainGrouperAlgS1 extends ScanChainGrouper {

	HashMap<Node, Integer> node2idx;

	BitSet[] impacts;

	int[][][] aregions;

	public int[] calculateClocking(int clockCount) {

		ensure_impacts();
		ensure_aregions();

		log.info("finished setup.");

		int clocking[] = new int[chains.size()];

		Random r = new Random(42);

		int cand_clocking[] = new int[clocking.length];
		int cand_cost = Integer.MAX_VALUE;
		for (int i = 0; i < 50; i++) {
			for (int c = 0; c < clocking.length; c++) {
				cand_clocking[c] = r.nextInt(clockCount);
			}
			int this_cost = cost(cand_clocking, clockCount);
			if (this_cost < cand_cost) {
				System.arraycopy(cand_clocking, 0, clocking, 0, clocking.length);
				cand_cost = this_cost;
			}
		}

		log.info("Best after random search: " + cand_cost);

		for (int i = 0; i < 10; i++) {
			int chain = findWorstChain(clocking, clockCount);
			int diff = tweakChain(clocking, clockCount, chain);
			if (diff == 0)
				break;
		}

		log.info("Cost after optimizing: " + cost(clocking, clockCount));

		return clocking;
	}

	private int findWorstChain(int[] clocking, int clockCount) {
		int worst_chain = -1;
		int highest_cost_diff = 0;
		int base_cost = cost(clocking, clockCount);
		for (int chain_idx = 0; chain_idx < clocking.length; chain_idx++) {
			int clk = clocking[chain_idx];
			clocking[chain_idx] = -1;
			int cost_diff = base_cost - cost(clocking, clockCount);
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
		int base_cost = cost(clocking, clockCount);
		for (int clock = 0; clock < clockCount; clock++) {
			clocking[chain_idx] = clock;
			int cost_diff = base_cost - cost(clocking, clockCount);
			if (cost_diff > highest_cost_diff) {
				best_clk = clock;
				highest_cost_diff = cost_diff;
			}
		}
		clocking[chain_idx] = best_clk;

		log.debug("Chain " + chain_idx + " move from " + old_clk + " to " + best_clk + " gaining " + highest_cost_diff);
		return highest_cost_diff;
	}

	private BitSet[] clk_impacts;

	private int cost(int[] clocking, int clocks) {

		// compute sets of possibly active nodes for each staggered clock.
		ensure_clk_impacts(clocks);
		for (int c = 0; c < clocks; c++) {
			for (int chainIdx = 0; chainIdx < impacts.length; chainIdx++) {
				if (clocking[chainIdx] == c)
					clk_impacts[c].or(impacts[chainIdx]);
			}
		}

		// intersect the computed sets with all sff aggressor sets
		int maxSize = 0;
		for (int chain_idx = 0; chain_idx < aregions.length; chain_idx++) {
			for (int cell_idx = 0; cell_idx < aregions[chain_idx].length; cell_idx++) {
				int[] ar = aregions[chain_idx][cell_idx];
				for (int clk = 0; clk < clocks; clk++) {
					BitSet ci = clk_impacts[clk];
					int overlap = 0;
					for (int agg_idx = 0; agg_idx < ar.length; agg_idx++) {
						if (ci.get(ar[agg_idx]))
							overlap++;
					}
					maxSize = Math.max(maxSize, overlap);
				}
			}
		}
		return maxSize;
	}

	// data structures setup

	private void ensure_clk_impacts(int clocks) {
		if (clk_impacts == null || clk_impacts.length != clocks) {
			clk_impacts = new BitSet[clocks];
			for (int i = 0; i < clk_impacts.length; i++)
				clk_impacts[i] = new BitSet();
		} else {
			for (int i = 0; i < clk_impacts.length; i++)
				clk_impacts[i].clear();
		}
	}

	private void ensure_aregions() {
		if (aregions != null)
			return;
		ensure_node2idx();
		aregions = new int[chains.size()][][];
		for (int chain_idx = 0; chain_idx < chains.size(); chain_idx++) {
			ScanChain chain = chains.get(chain_idx);
			int cells = chain.cells.size();
			aregions[chain_idx] = new int[cells][];
			for (int cell_idx = 0; cell_idx < cells; cell_idx++) {
				ScanCell cell = chain.cells.get(cell_idx);
				HashSet<Node> agg = cell2aggressorSet.get(cell);
				int cnt = 0;
				for (Node n : agg) {
					if (node2idx.containsKey(n))
						cnt++;
				}
				aregions[chain_idx][cell_idx] = new int[cnt];
				int idx = 0;
				for (Node n : agg) {
					if (node2idx.containsKey(n))
						aregions[chain_idx][cell_idx][idx++] = node2idx.get(n);
				}
			}
		}
		ensure_impacts();

	}

	private void ensure_impacts() {
		if (impacts != null)
			return;
		ensure_node2idx();
		impacts = new BitSet[chains.size()];
		for (ScanChain chain : chain2impactSet.keySet()) {
			int idx = chain.chainIdx();
			impacts[idx] = new BitSet();
			for (Node n : chain2impactSet.get(chain)) {
				impacts[idx].set(node2idx.get(n));
			}
		}

	}

	private void ensure_node2idx() {
		if (node2idx != null)
			return;
		node2idx = new HashMap<>();
		int idx = 0;
		for (HashSet<Node> nodes : chain2impactSet.values()) {
			for (Node n : nodes) {
				if (!node2idx.containsKey(n)) {
					node2idx.put(n, idx++);
				}
			}
		}
	}
}
