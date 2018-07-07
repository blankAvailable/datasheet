package jp.ac.kyutech.ci.grouping;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.kyupi.circuit.Cell;
import org.kyupi.circuit.ScanChains.ScanCell;
import org.kyupi.circuit.ScanChains.ScanChain;

import jp.ac.kyutech.ci.grouping.Main.CBInfo;

public class FastCostFunction {

	HashMap<Cell, Integer> node2idx;
	BitSet[] impacts;
	int[][][] aregions;
	float[] nodeCost;

	public FastCostFunction(HashMap<ScanChain, HashSet<Cell>> chain2impactSet,
							HashMap<ScanCell, HashSet<Cell>> cell2aggressorSet, CBInfo cbInfo){
		node2idx = new HashMap<>();
		int idx = 0;
		for (HashSet<Cell> nodes : chain2impactSet.values())
			for (Cell n : nodes)
				if (!node2idx.containsKey(n))
					node2idx.put(n, idx++);

		impacts = new BitSet[chain2impactSet.keySet().size()];
		for (ScanChain chain : chain2impactSet.keySet()) {
			idx = chain.chainIdx();
			impacts[idx] = new BitSet();
			for (Cell n : chain2impactSet.get(chain))
				impacts[idx].set(node2idx.get(n));
		}

		aregions = new int[impacts.length][][];
		for (ScanChain chain : chain2impactSet.keySet()) {
			int chain_idx = chain.chainIdx();
			int cells = chain.cells.size();
			aregions[chain_idx] = new int[cells][];
			for (int cell_idx = 0; cell_idx < cells; cell_idx++) {
				ScanCell cell = chain.cells.get(cell_idx);
				HashSet<Cell> agg = cell2aggressorSet.get(cell);
				idx = 0;
				for (Cell n : agg)
					if (node2idx.containsKey(n))
						idx++;
				aregions[chain_idx][cell_idx] = new int[idx];
				idx = 0;
				for (Cell n : agg)
					if (node2idx.containsKey(n))
						aregions[chain_idx][cell_idx][idx++] = node2idx.get(n);
			}
		}

		nodeCost = new float[node2idx.size()];
		for (Map.Entry<Cell, Integer> entry : node2idx.entrySet()){
			String typename = entry.getKey().typeName();
			if (cbInfo.all_clock_buffers.contains(entry.getKey())) {
				nodeCost[entry.getValue()] = 1 * entry.getKey().inputCount();
			}else if (typename.contains("AND")){
				if (typename.contains("NAND")) {
					nodeCost[entry.getValue()] = (float) 0.75;
				}else {
					nodeCost[entry.getValue()] = (float) 0.25;
				}
			}else if (typename.contains("OR")){
				if (typename.contains("NOR")){
					nodeCost[entry.getValue()] = (float) 0.25;
				}else {
					nodeCost[entry.getValue()] = (float) 0.75;
				}
			}else {
				nodeCost[entry.getValue()] = (float) 0.5;
			}
		}
	}

	public FastCostFunction(HashMap<ScanChain, HashSet<Cell>> chain2impactSet,
			HashMap<ScanCell, HashSet<Cell>> cell2aggressorSet) {

		node2idx = new HashMap<>();
		int idx = 0;
		for (HashSet<Cell> nodes : chain2impactSet.values())
			for (Cell n : nodes)
				if (!node2idx.containsKey(n))
					node2idx.put(n, idx++);

		impacts = new BitSet[chain2impactSet.keySet().size()];
		for (ScanChain chain : chain2impactSet.keySet()) {
			idx = chain.chainIdx();
			impacts[idx] = new BitSet();
			for (Cell n : chain2impactSet.get(chain))
				impacts[idx].set(node2idx.get(n));
		}

		aregions = new int[impacts.length][][];
		for (ScanChain chain : chain2impactSet.keySet()) {
			int chain_idx = chain.chainIdx();
			int cells = chain.cells.size();
			aregions[chain_idx] = new int[cells][];
			for (int cell_idx = 0; cell_idx < cells; cell_idx++) {
				ScanCell cell = chain.cells.get(cell_idx);
				HashSet<Cell> agg = cell2aggressorSet.get(cell);
				idx = 0;
				for (Cell n : agg)
					if (node2idx.containsKey(n))
						idx++;
				aregions[chain_idx][cell_idx] = new int[idx];
				idx = 0;
				for (Cell n : agg)
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

	public float evaluate_float(int[] clocking, int clocks) {

		float maxCost = 0;

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
					float cost = 0;
					for (int agg_idx = 0; agg_idx < aregions[chain_idx][cell_idx].length; agg_idx++) {
						if (impactUnion.get(aregions[chain_idx][cell_idx][agg_idx]))
							cost += 1.1;
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

	public boolean evaluate_usable(int[] clocking, int clocks, float threshold) {
		boolean usable = true;

		for (int c = 0; c < clocks; c++) {
			impactUnion.clear();
			for (int chainIdx = 0; chainIdx < impacts.length; chainIdx++) {
				if (clocking[chainIdx] == c)
					//copy impacts[chainIdx] to bitset impactUnion
					impactUnion.or(impacts[chainIdx]);
			}
			for (int chainIdx = 0; chainIdx < aregions.length; chainIdx++) {
				for (int cellIdx = 0; cellIdx < aregions[chainIdx].length; cellIdx++){
					float cost = 0;
					for (int aggIdx = 0; aggIdx < aregions[chainIdx][cellIdx].length; aggIdx++){
						if (impactUnion.get(aregions[chainIdx][cellIdx][aggIdx]))
							cost++;
					}
					if (cost == 0)
						continue;
					if (cost > threshold)
						usable = false;
				}
			}
			if (usable == false)
				break;
		}
		return usable;
	}
	
	public int getLastWorstClockIdx() {
		return last_clock_idx;
	}

}
