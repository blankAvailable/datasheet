package jp.ac.kyutech.ci.grouping;

import org.kyupi.graph.Graph;
import org.kyupi.graph.ScanChains;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Created by ZhangYucong on 2017/3/22.
 */
public class Matrix {
    private ScanChains chains;
    private HashMap<ScanChains.ScanCell, HashSet<Graph.Node>> cell2aggressorSet;
    private HashMap<ScanChains.ScanChain, HashSet<Graph.Node>> chain2impactSet;

    public Matrix(ScanChains chains, HashMap cell2aggressorSet, HashMap chain2impactSet){
        this.chains = chains;
        this.cell2aggressorSet = cell2aggressorSet;
        this.chain2impactSet = chain2impactSet;
    }

    /** calculate the biggest impacted weight in one grouping */
    public int groupEvaluate(List<List<Integer>> scGroup){
        int maxImpact = 0;

        for (int i=0; i<scGroup.size(); i++){

            //calculate the impact node set of one scan chain group
            HashSet<Graph.Node> group2impactSet = new HashSet<>();
            for (int chainIdx=0; chainIdx<scGroup.get(i).size(); chainIdx++){
                ScanChains.ScanChain chain = chains.get(chainIdx);
                group2impactSet.addAll(chain2impactSet.get(chain));
            }
            //get reachable aggressor set of one scan chain group
            HashSet<Graph.Node> tempAggressorSet = new HashSet<>();
            for (int chainIdx=0; chainIdx<scGroup.get(i).size(); chainIdx++){
                ScanChains.ScanChain chain = chains.get(chainIdx);
                for (ScanChains.ScanCell cell : chain.cells){
                   tempAggressorSet =  cell2aggressorSet.get(cell);
                   tempAggressorSet.retainAll(group2impactSet);
                   for (Graph.Node node : tempAggressorSet){
                       if (node.countOuts() > maxImpact)
                           maxImpact = node.countOuts();
                   }
                }
            }

        }
        //return the biggest impacted weight of one scan chain grouping
        return maxImpact;
    }
}
