package jp.ac.kyutech.ci.grouping;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;

import org.kyupi.graph.Graph.Node;
import org.kyupi.graph.ScanChains.ScanCell;
import org.kyupi.graph.ScanChains.ScanChain;

public class FastCostFunction {

	HashMap<Node, Integer> node2idx;
	BitSet[] impacts;
	int[][][] aregions;

	public FastCostFunction(HashMap<ScanChain, HashSet<Node>> chain2impactSet,
			HashMap<ScanCell, HashSet<Node>> cell2aggressorSet) {

		node2idx = new HashMap<>();
		int idx = 0;
		for (HashSet<Node> nodes : chain2impactSet.values())
			for (Node n : nodes)
				if (!node2idx.containsKey(n))
					node2idx.put(n, idx++);

		impacts = new BitSet[chain2impactSet.keySet().size()];
		for (ScanChain chain : chain2impactSet.keySet()) {
			idx = chain.chainIdx();
			impacts[idx] = new BitSet();
			for (Node n : chain2impactSet.get(chain))
				impacts[idx].set(node2idx.get(n));
		}

		aregions = new int[impacts.length][][];
		for (ScanChain chain : chain2impactSet.keySet()) {
			int chain_idx = chain.chainIdx();
			int cells = chain.cells.size();
			aregions[chain_idx] = new int[cells][];
			for (int cell_idx = 0; cell_idx < cells; cell_idx++) {
				ScanCell cell = chain.cells.get(cell_idx);
				HashSet<Node> agg = cell2aggressorSet.get(cell);
				idx = 0;
				for (Node n : agg)
					if (node2idx.containsKey(n))
						idx++;
				aregions[chain_idx][cell_idx] = new int[idx];
				idx = 0;
				for (Node n : agg)
					if (node2idx.containsKey(n))
						aregions[chain_idx][cell_idx][idx++] = node2idx.get(n);
			}
		}
	}

	private BitSet impactUnion = new BitSet();

	private int last_chain_idx = 0;
	
	private int last_cell_idx = 0;
	
	private int last_clock_idx = 0;
	
	public int evaluate(int[] clocking, int clocks) {

		int maxCost = 0;

		for (int c = 0; c < clocks; c++) {
			// compute sets of possibly active nodes current staggered clock.
			impactUnion.clear();
			for (int chainIdx = 0; chainIdx < impacts.length; chainIdx++) {
				if (clocking[chainIdx] == c)
					//copy impacts[chainIdx] to bitset impactUnion
					impactUnion.or(impacts[chainIdx]);
			}
			for (int chain_idx = 0; chain_idx < aregions.length; chain_idx++) {
				for (int cell_idx = 0; cell_idx < aregions[chain_idx].length; cell_idx++) {
					int cost = 0;
					for (int agg_idx = 0; agg_idx < aregions[chain_idx][cell_idx].length; agg_idx++) {
						if (impactUnion.get(aregions[chain_idx][cell_idx][agg_idx]))
							cost++;
					}
					if (cost > maxCost) {
						maxCost = cost;
						last_chain_idx = chain_idx;
						last_cell_idx = cell_idx;
						last_clock_idx = c;
					}
				}
			}
		}
		return maxCost;
	}
	
	public int getLastWorstClockIdx() {
		return last_clock_idx;
	}
	
	

}
