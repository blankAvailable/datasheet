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

    public int evaluate(int[] clocking, int clocks){
        int maxcost = 0;
        return maxcost;
    }
}
