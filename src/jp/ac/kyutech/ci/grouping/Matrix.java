package jp.ac.kyutech.ci.grouping;

import jp.ac.kyutech.ci.datasheet.ScAggre;
import jp.ac.kyutech.ci.datasheet.ScffAggre;
import jp.ac.kyutech.ci.datasheet.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ZhangYucong on 2017/3/22.
 */
public class Matrix {
    private String circuitName = null;

    public Matrix(String circuitName){
        this.circuitName = circuitName;
    }

    /** calculate the biggest WSA difference between every two neighboring flip flips in one grouping */
    public int groupEvaluate(List<List<Integer>> scGroup) throws IOException  {
        int maxWsa = 0;
        int maxWsaDiff = 0;
        List<Integer> maxWsaList = new ArrayList<>();
        List<Integer> reachableAggre = new ArrayList<>();
        List<Integer> tempffAggre = new ArrayList<>();
        ScffAggre scffAggre = new ScffAggre(circuitName);
        ScAggre scAggre = new ScAggre(circuitName);
        Util util = new Util();

        for (int i=0; i<scGroup.size(); i++){
            //get reachable aggressor set of one scan chain group
            for (int j=0; j<scGroup.get(i).size(); j++){
                reachableAggre.removeAll(scAggre.scAggreId.get(scGroup.get(i).get(j)));
                reachableAggre.addAll(scAggre.scAggreId.get(scGroup.get(i).get(j)));
            }
            System.out.println("reachable aggressor set: " + reachableAggre);

            //calculate the biggest max WSA difference of every group
            for (int j=0; j<scGroup.get(i).size(); j++){
                for (int k=0; k<scffAggre.scFFid.get(scGroup.get(i).get(j)).size(); k++){
                    System.out.println("flip flop id: " + scffAggre.scFFid.get(scGroup.get(i).get(j)).get(k));
                    tempffAggre.addAll(util.clone(scffAggre.ffidAggre.get(scffAggre.scFFid.get(scGroup.get(i).get(j)).get(k))));
                    tempffAggre.retainAll(reachableAggre);
                    System.out.println("impact aggressor set: " + tempffAggre);
                    for (int l=0; l<tempffAggre.size(); l++){
                        maxWsa = maxWsa + scffAggre.pidFanout[tempffAggre.get(l)];
                    }
                    System.out.println("maxWsa of ff: " + scffAggre.scFFid.get(scGroup.get(i).get(j)).get(k) + " is " + maxWsa);
                    maxWsaList.add(maxWsa);
                    tempffAggre.clear();
                    maxWsa = 0;
                    for (int l=0; l<maxWsaList.size(); l++){
                        if ((l+1)<maxWsaList.size()) {
                            int wsaDiff = maxWsaList.get(l + 1) - maxWsaList.get(l);
                            if (maxWsaDiff<wsaDiff)
                                maxWsaDiff = wsaDiff;
                        }else{
                            break;
                        }
                    }
                }
            }
            System.out.println();
            reachableAggre.clear();
        }
        //return the biggest max WSA difference of one scan chain grouping
        return maxWsaDiff;
    }
}
