package jp.ac.kyutech.ci.grouping;

import java.util.Arrays;
import java.util.HashSet;

public class ScanChainGrouperAlgS2 extends ScanChainGrouper {

	private FastCostFunction cost;

	private int[][] paircost;

	public int[] calculateClocking(int clockCount) {

		if (cost == null) {
			cost = new FastCostFunction(chain2impactSet, cell2aggressorSet);
			log.info("FastCostFunction initialized.");
		}

		int clocking[] = new int[chains.size()];

		int upperBound = cost.evaluate(clocking, 1);
		log.info("UpperBound (by c=1) " + upperBound);

		for (int i = 0; i < clocking.length; i++)
			clocking[i] = i;
		int lowerBound = cost.evaluate(clocking, clocking.length);
		log.info("LowerBound (by c=∞) " + lowerBound);

		paircost = new int[clocking.length][clocking.length];
		Arrays.fill(clocking, -1);
		int[] chain_min_pair_cost = new int[clocking.length];
		Arrays.fill(chain_min_pair_cost, Integer.MAX_VALUE);
		for (int i = 0; i < clocking.length; i++) {
			log.debug("PairCost calculation " + i + " of " + clocking.length + " ...");
			clocking[i] = 0;
			for (int j = i + 1; j < clocking.length; j++) {
				clocking[j] = 0;
				paircost[i][j] = cost.evaluate(clocking, 1);
				chain_min_pair_cost[i] = Math.min(chain_min_pair_cost[i], paircost[i][j]);
				chain_min_pair_cost[j] = Math.min(chain_min_pair_cost[j], paircost[i][j]);
				clocking[j] = -1;
			}
			clocking[i] = -1;
		}

		// for (int i = 0; i < clocking.length; i++)
		// lowerBound = Math.max(lowerBound, chain_min_pair_cost[i] + 1);
		// log.info("LowerBound (after pair) " + lowerBound);

		int lower_bound_clique = recursiveColorizableBoundSearch(lowerBound, upperBound, clocking, clockCount);
		lowerBound = Math.max(lowerBound, lower_bound_clique);
		log.info("LowerBound (after clique) " + lowerBound);

		int solution_cost = cost.evaluate(clocking, clockCount);
		log.info("BestKnownSolution (after clique) " + solution_cost);

		if (lowerBound == solution_cost) {
			log.info("Returning best possible solution.");
			return clocking;
		}

		GraphColorizer g = new GraphColorizer(clocking.length, clockCount);
		for (int i = 0; i < clocking.length; i++) {
			for (int j = i + 1; j < clocking.length; j++) {
				if (paircost[i][j] > lowerBound)
					g.addEdge(i, j);
			}
		}
		log.info("graph size nodes " + g.size() + " edges " + g.countEdges());
		clocking = g.colorize();

		if (clocking == null) {
			log.error("can't verify last clique solution!");
			return null;
		}

		solution_cost = cost.evaluate(clocking, clockCount);

		int[] edge = new int[chains.size()];
		int[] clocking_tmp = new int[chains.size()];

		int best_cost = solution_cost;

		while (true) {

			int worstClk = cost.getLastWorstClockIdx();

			HashSet<Integer> conflictChains = new HashSet<>();
			StringBuffer buf = new StringBuffer();

			int constraint_size = 0;
			for (int c = 0; c < clocking.length; c++) {
				clocking_tmp[c] = -1;
				if (clocking[c] == worstClk) {
					conflictChains.add(c);
					buf.append(" " + c);
					clocking_tmp[c] = 0;
					constraint_size++;
				}
			}
			
			int nchains = constraint_size;

			int last_chain = 0;
			while (last_chain < clocking_tmp.length && constraint_size > 3) {
				int chain = last_chain;
				while (chain < clocking_tmp.length && clocking_tmp[chain] < 0)
					chain++;
				if (chain >= clocking_tmp.length || clocking_tmp[chain] == -1)
					break;
				clocking_tmp[chain] = -1;
				int cost_tmp = cost.evaluate(clocking_tmp, 1);
				// log.info("cost " + cost_tmp);
				if (solution_cost == cost_tmp) {
					// log.info("remove " + chain);
					conflictChains.remove(chain);
					constraint_size--;
				} else {
					// log.info("keep " + chain);
					clocking_tmp[chain] = 0;
				}
				last_chain = chain + 1;
			}
			log.info("Last worst clock: " + worstClk + " containing " + nchains + " chains. adding constraint of size " + constraint_size);

			int edgeSize = 0;
			for (Integer cidx : conflictChains) {
				edge[edgeSize++] = cidx;
			}
			g.addEdge(edge, edgeSize);
			//log.info("graph size nodes " + g.size() + " edges " + g.countEdges());
			int[] new_sol = g.colorize();
			if (new_sol == null) {
				log.info("no new solution");
				log.info("LowerBound " + solution_cost);
				log.info("BestKnownSolution " + solution_cost);
				log.info("returning best possible solution");
				return clocking;
			}
			clocking = new_sol;
			solution_cost = cost.evaluate(clocking, clockCount);
			//log.info("this solution " + solution_cost);
			best_cost = Math.min(best_cost, solution_cost);
			log.info("BestKnownSolution " + best_cost);
		}

	}

	private int recursiveColorizableBoundSearch(int lb, int ub, int[] solution, int colorCount) {
		int middle = (ub - lb) / 2 + lb;
		log.info("trying " + lb + " " + middle + " " + ub);
		GraphColorizer g = new GraphColorizer(solution.length, colorCount);
		for (int i = 0; i < solution.length; i++) {
			for (int j = i + 1; j < solution.length; j++) {
				if (paircost[i][j] > middle)
					g.addEdge(i, j);
			}
		}
		log.info("graph size nodes " + g.size() + " edges " + g.countEdges());
		int[] s = g.colorize();
		if (s != null) {
			System.arraycopy(s, 0, solution, 0, solution.length);
			log.info("solution for " + middle);
			if (middle > lb)
				return recursiveColorizableBoundSearch(lb, middle, solution, colorCount);
			else
				return middle;
		} else {
			log.info("impossible for " + middle);
			if (middle < (ub - 1))
				return recursiveColorizableBoundSearch(middle, ub, solution, colorCount);
			else
				return middle + 1;
		}
	}
}
