package jp.ac.kyutech.ci.sc_grouping_clkaggre;

import org.kyupi.graph.Graph.Node;
import org.kyupi.graph.ScanChains.ScanCell;
import org.kyupi.graph.ScanChains.ScanChain;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;

public class FastCostFunction {
    HashMap<Node, Integer> node2idx;
    BitSet[] impacts;
    int[][][] aregions;

    public FastCostFunction(HashMap<ScanChain, HashSet<Node>> chain2impactSet, HashMap<ScanCell, HashSet<Node>> cell2aggressorSet){

        node2idx = new HashMap<>();
        int idx = 0;
        for (HashSet<Node> nodes : chain2impactSet.values())
            for (Node n : nodes)
                if (!node2idx.containsKey(n))
                    node2idx.put(n, idx++);

        impacts = new BitSet[chain2impactSet.keySet().size()];
        for (ScanChain chain : chain2impactSet.keySet()){
            idx = chain.chainIdx();
            impacts[idx] = new BitSet();
            for (Node n : chain2impactSet.get(chain))
                impacts[idx].set(node2idx.get(n));
        }

        aregions = new int[impacts.length][][];
        for (ScanChain chain : chain2impactSet.keySet()){
            int chainId = chain.chainIdx();
            int cells = chain.cells.size();
            aregions[chainId] = new int[cells][];
            for (int cellId = 0; cellId < cells; cellId++){
                ScanCell cell = chain.cells.get(cellId);
                HashSet<Node> agg = cell2aggressorSet.get(cell);
                idx = 0;
                for (Node n : agg)
                    if (node2idx.containsKey(n))
                        idx++;
                aregions[chainId][cellId] = new int[idx];
                idx = 0;
                for (Node n : agg)
                    if (node2idx.containsKey(n))
                        aregions[chainId][cellId][idx++] = node2idx.get(n);
            }
        }
    }

    private BitSet impactUnion = new BitSet();

    private int last_chain_id = 0;

    private int last_cell_id = 0;

    private int last_clock_id = 0;

    // need test
    public int evaluate(int[] clocking, int clocks){
        int maxcost = 0;

        for (int c = 0; c < clocks; c++){
            // compute sets of possibly active nodes of current group
            impactUnion.clear();
            for (int chainId = 0; chainId < impacts.length; chainId++){
                if (clocking[chainId] == c)
                    impactUnion.or(impacts[chainId]);
            }
            for (int chainId = 0; chainId < aregions.length; chainId++){
                int cost_predecessor = 0;
                for (int cellId = 0; cellId < aregions[chainId].length; cellId++){
                    int cost = 0;
                    for (int aggId = 0; aggId < aregions[chainId][cellId].length; aggId++){
                        if (impactUnion.get(aregions[chainId][cellId][aggId]))
                            cost++;
                    }
                    if (cost_predecessor == 0){
                        cost_predecessor = cost;
                        continue;
                    }else if (Math.abs((cost_predecessor - cost)) > maxcost){
                        maxcost = Math.abs((cost_predecessor - cost));
                        cost_predecessor = cost;
                        last_chain_id = chainId;
                        last_cell_id = cellId;
                        last_clock_id = c;
                    }
                }
            }
        }

        return maxcost;
    }

    public int getLastWorstClockId(){ return last_clock_id; }
}
