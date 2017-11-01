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

    }
}
