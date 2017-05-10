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
public class MatrixZ1 {
    private ScanChains chains;
    private HashMap<ScanChains.ScanCell, HashSet<Graph.Node>> cell2aggressorSet;
    private HashMap<ScanChains.ScanChain, HashSet<Graph.Node>> chain2impactSet;

    public MatrixZ1(ScanChains chains, HashMap cell2aggressorSet, HashMap chain2impactSet){
        this.chains = chains;
        this.cell2aggressorSet = cell2aggressorSet;
        this.chain2impactSet = chain2impactSet;
    }

    /** calculate the biggest impacted weight in one grouping */
    public int groupEvaluate(List<List<Integer>> scGroup){
        int[] maxImpact = new int[scGroup.size()];

        HashSet<Integer> group2impactSet = new HashSet<>();
        HashSet<Integer> tempAggressorSet = new HashSet<>();
        Util util = new Util();

        for (int i=0; i<scGroup.size(); i++){

            //calculate the impact node set of one scan chain group
            for (int chainIdx=0; chainIdx<scGroup.get(i).size(); chainIdx++){
                ScanChains.ScanChain chain = chains.get(chainIdx);
                for (Iterator<Graph.Node> iterator = chain2impactSet.get(chain).iterator(); iterator.hasNext();){
                    int idx = iterator.next().id();
                    group2impactSet.add(idx);
                }
            }

            //get reachable aggressor set of one scan chain group
            for (int chainIdx=0; chainIdx<scGroup.get(i).size(); chainIdx++){
                ScanChains.ScanChain chain = chains.get(chainIdx);
                for (ScanChains.ScanCell cell : chain.cells){
                    for (Iterator<Graph.Node> iterator = cell2aggressorSet.get(cell).iterator(); iterator.hasNext();){
                        int idx = iterator.next().id();
                        tempAggressorSet.add(idx);
                    }
                    tempAggressorSet.retainAll(group2impactSet);
                   //calculate the max impacted weight for every scan cell
                    maxImpact[i] = Math.max(tempAggressorSet.size(), maxImpact[i]);
                    tempAggressorSet.clear();
                }
            }
            group2impactSet.clear();

        }
        //return the biggest impacted weight of one scan chain grouping
        System.out.println("maxAvtiveAggressor set size: " + util.findMax(maxImpact));
        return util.findMax(maxImpact);
    }
}
