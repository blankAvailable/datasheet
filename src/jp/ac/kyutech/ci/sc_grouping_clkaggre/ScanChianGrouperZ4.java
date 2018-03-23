package jp.ac.kyutech.ci.sc_grouping_clkaggre;

import org.kyupi.graph.Graph.Node;
import org.kyupi.graph.ScanChains.ScanCell;
import org.kyupi.graph.ScanChains.ScanChain;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;

/**
 * integer linear programming
 */

/**
 * initialize a sample circuit with 3 chains (s1, s2, s3)
 * s1, s2 has one ff, s3 has 2,
 */
class  MinCircuit {

    BitSet[] impacts = new BitSet[3];
    BitSet[] chain2aggressor = new BitSet[3];
    int[][][] aregion;
    public MinCircuit(){
        impacts[0] = new BitSet();
        impacts[0].set(0);
        impacts[0].set(1);

        impacts[1] = new BitSet();
        impacts[1].set(1);
        impacts[1].set(2);

        impacts[2] = new BitSet();

        for (int i = 0; i < chain2aggressor.length; i ++)
            chain2aggressor[i] = new BitSet();
        chain2aggressor[0].set(1);
        chain2aggressor[0].set(2);
        chain2aggressor[2].set(0);
        chain2aggressor[2].set(1);

        aregion = new int[impacts.length][][];
        aregion[0] = new int[2][];
        aregion[0][0] = new int[1];
        aregion[0][1] = new int[1];
        aregion[0][0][0] = 1;
        aregion[0][1][0] = 2;

        aregion[1] = new int[1][1];
        aregion[1][0][0] = -1;

        aregion[2] = new int[2][];
        aregion[2][0] = new int[2];
        aregion[2][0][0] = 0;
        aregion[2][0][1] = 0;

        aregion[2][1] = new int[1];
        aregion[2][1][0] = 1;

    }

}

/**
 * generate the integer linear problem model, write it in zimpl format(.zpl file).
 */
public class ScanChianGrouperZ4 {
    HashMap<Node, Integer> node2idx;
    BitSet[] chain2aggressors;
    BitSet[] impacts;
    int[][][] aregions;
    int[][] chainaregions;
    int skewthreshold;
    int conflict;

    public void testCase() throws IOException {
        BufferedWriter test = null;
        File testWriter = new File("test.zpl");
        testWriter.createNewFile();
        test = new BufferedWriter(new FileWriter(testWriter));
        long constrainId = 0;
        int conflict = 0;

        MinCircuit testcase = new MinCircuit();

        VariableWriter(testcase.chain2aggressor, test, 2, 'x');
        VariableWriter(testcase.impacts, test, 2, 'y');
        VariableWriter(testcase.chain2aggressor, test, 2, 'z');
        test.write("\n");
        constrainId = Var2ChainConsWriter(testcase.chain2aggressor, test, 2, 'x', constrainId);
        constrainId = Var2ChainConsWriter(testcase.impacts, test, 2, 'y', constrainId);
        constrainId = OnevOnegConsWriter(testcase.chain2aggressor, test, 2, 'x', constrainId);
        constrainId = OnevOnegConsWriter(testcase.impacts, test, 2, 'y', constrainId);
        constrainId = ZConsWriter(testcase.chain2aggressor, testcase.impacts, test, 2, constrainId);
        constrainId = SelfConsWriter(testcase.chain2aggressor, testcase.impacts, test, 2, constrainId);
        conflict = ThrConsWeiter(testcase.aregion, test, 2, 0, constrainId);
        test.write("\n");
        ObjectiveWriter(conflict, test);


        if (test != null)
            test.close();
    }

    public ScanChianGrouperZ4(HashMap<ScanChain, HashSet<Node>> chain2impactSet, HashMap<ScanCell, ArrayList<Node>> cell2aggressorSet, int skewthreshold){

        this.skewthreshold = skewthreshold;

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
        chainaregions = new int[impacts.length][];
        for (ScanChain chain : chain2impactSet.keySet()){
            int chainId = chain.chainIdx();
            int scancells = chain.cells.size();
            aregions[chainId] = new int[scancells][];
            for (int cellId = 0; cellId < scancells; cellId++){
                ScanCell cell = chain.cells.get(cellId);
                ArrayList<Node> agg = cell2aggressorSet.get(cell);
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

        //generate chain2aggressors list for constrains and x, z variables
        chain2aggressors = new BitSet[chain2impactSet.keySet().size()];
        for (ScanChain chain : chain2impactSet.keySet()){
            int chainId = chain.chainIdx();
            int scancells = chain.cells.size();
            chain2aggressors[chainId] = new BitSet();
            for (int cellId = 0; cellId < scancells; cellId++){
                ScanCell cell = chain.cells.get(cellId);
                ArrayList<Node> agg = cell2aggressorSet.get(cell);
                for (Node n : agg)
                    if (node2idx.containsKey(n))
                        chain2aggressors[chainId].set(node2idx.get(n));
            }
        }
    }

    private void VariableWriter(BitSet[] objectivelist, BufferedWriter var, int groupCount, char variablename) throws IOException {
        for (int chainIdx = 0; chainIdx < objectivelist.length; chainIdx++){
            for (int nodeIdx = 0; nodeIdx < objectivelist[chainIdx].length(); nodeIdx++){
                if (objectivelist[chainIdx].get(nodeIdx)){
                    for (int g = 0; g < groupCount; g++)
                        var.write("var " + variablename + "_"  + nodeIdx + "_" + chainIdx + "_" + g + " binary;\n");
                }
            }
        }
    }

    private long Var2ChainConsWriter(BitSet[] objectivelist, BufferedWriter cons, int groupCount, char variablename, long constrainId) throws IOException {
        for (int chainIdx = 0; chainIdx < objectivelist.length; chainIdx++){
            for (int g = 0; g < groupCount; g++){
                ArrayList<Integer> nodecounter = new ArrayList<>();
                for (int nodeIdx = 0; nodeIdx < objectivelist[chainIdx].size(); nodeIdx++){
                    if (objectivelist[chainIdx].get(nodeIdx)) {
                        System.out.println(nodeIdx);
                        nodecounter.add(nodeIdx);
                    }
                }

                if (nodecounter.size() == 0)
                    continue;

                //write a constrain
                cons.write("subto c" + constrainId + ": vif " + variablename + "_" + nodecounter.get(0) + "_" + chainIdx + "_" + g + " == 1 then ");
                for (int i = 0; i < nodecounter.size()-1; i ++)
                    cons.write(variablename + "_" + nodecounter.get(i) + "_" + chainIdx + "_" + g + " + ");
                cons.write(variablename + "_" + nodecounter.get(nodecounter.size()-1) + "_" + chainIdx + "_" + g + " == " + nodecounter.size() + " else ");
                for (int i = 0; i < nodecounter.size()-1; i ++)
                    cons.write(variablename + "_" + nodecounter.get(i) + "_" + chainIdx + "_" + g + " + ");
                cons.write(variablename + "_" + nodecounter.get(nodecounter.size()-1) + "_" + chainIdx + "_" + g + " == 0 end;\n");
                constrainId++;
            }
        }
        return constrainId;
    }

    private long OnevOnegConsWriter(BitSet[] objectivelist, BufferedWriter cons, int groupCount, char variablename, long constrainId) throws IOException {
        for (int chainIdx  = 0; chainIdx < objectivelist.length; chainIdx++){
            for (int nodeIdx = 0; nodeIdx < objectivelist[chainIdx].length(); nodeIdx++) {
                if (objectivelist[chainIdx].get(nodeIdx)) {
                    cons.write("subto c" + constrainId + ": ");
                    for (int g = 0; g < groupCount-1; g++)
                        cons.write(variablename + "_" + nodeIdx + "_" + chainIdx + "_" + g + " + ");
                    cons.write(variablename + "_" + nodeIdx + "_" + chainIdx + "_" + (groupCount-1) + " == 1;\n");
                    constrainId++;
                }
            }
        }
        return constrainId;
    }

    private long ZConsWriter(BitSet[] objectivelist, BitSet[] objectivelist1, BufferedWriter cons, int groupCount, long constrainId) throws IOException {
        for (int chainIdx = 0; chainIdx < objectivelist.length; chainIdx++){
            for (int nodeIdx = 0; nodeIdx < objectivelist[chainIdx].length(); nodeIdx++){
                if (objectivelist[chainIdx].get(nodeIdx)){
                    for (int g = 0; g < groupCount; g++) {
                        cons.write("subto c" + constrainId + ": ");
                        //write vif x_node_chain_g == 1 and
                        cons.write("vif x_" + nodeIdx + "_" + chainIdx + "_" + g + " == 1 and ");
                        ArrayList<Integer> impactlist = new ArrayList<>();
                        for (int impchianIdx = 0; impchianIdx < objectivelist1.length; impchianIdx++){
                            if (objectivelist1[impchianIdx].get(nodeIdx))
                                impactlist.add(impchianIdx);
                        }

                        if (impactlist.size() == 0)
                            continue;

                        //write y_node_impchain_g + ... >= 1 then
                        for (int i = 0; i < impactlist.size()-1; i++)
                            cons.write("y_" + nodeIdx + "_" + impactlist.get(i) + "_" + g + " + ");
                        cons.write("y_" + nodeIdx + "_" + impactlist.get(impactlist.size()-1) + "_" + g + " >= 1 then ");

                        cons.write("z_" + nodeIdx + "_" + chainIdx + "_" + g + " == 1 else ");
                        cons.write("z_" + nodeIdx + "_" + chainIdx + "_" + g + " == 0 end;\n");
                        constrainId++;
                    }
                }
            }
        }
        return constrainId;
    }

    private long SelfConsWriter(BitSet[] objectivelist, BitSet[] objectivelist1, BufferedWriter cons, int groupCount, long constrainId) throws IOException {
        for (int chainIdx = 0; chainIdx < objectivelist.length; chainIdx++){
            for (int nodeIdx = 0; nodeIdx < objectivelist[chainIdx].length(); nodeIdx++){
                if (objectivelist[chainIdx].get(nodeIdx)){
                    if (objectivelist1[chainIdx].get(nodeIdx)){
                        for (int g = 0; g < groupCount; g++) {
                            cons.write("subto c" + constrainId + ": ");
                            cons.write("x_" + nodeIdx + "_" + chainIdx + "_" + g + " == ");
                            cons.write("y_" + nodeIdx + "_" + chainIdx + "_" + g + ";\n");
                            constrainId++;
                        }
                    }
                }
            }

        }
        return constrainId;
    }

    private int ThrConsWeiter(int[][][] aregions, BufferedWriter cons, int groupCount, int thr, long constrainId) throws IOException {
        int conflict = 0;
        boolean pairingflag = true;
        StringBuilder thrbuilder = new StringBuilder();
        for (int chainIdx = 0; chainIdx < aregions.length; chainIdx++){
            for (int scancellIdx = 0; scancellIdx < aregions[chainIdx].length; scancellIdx++){
                if (aregions[chainIdx][scancellIdx][0] == -1)
                    continue;
                if (pairingflag){
                    cons.write("var conf" + conflict + " binary;\n");
                    cons.write("subto c" + constrainId + ": vif vabs(");
                    if (scancellIdx > 0)
                        scancellIdx--;
                }
//                for (int nodeIdx = 0; nodeIdx < aregions[chainIdx][scancellIdx].length; nodeIdx++){
//                    for (int g = 0; g < groupCount; g++){
//                        if (pairingflag)
//                            thrbuilder.append(" + ");
//                        else
//                            thrbuilder.append(" - ");
//                        thrbuilder.append("z_" + aregions[chainIdx][scancellIdx][nodeIdx] + "_" + chainIdx + "_" + g);
//                        if (nodeIdx + 1 == aregions[chainIdx][scancellIdx].length && g + 1 == groupCount && !pairingflag){
//                            cons.write(thrbuilder.toString() + " < " + thr + " or" + thrbuilder.toString() + " > "
//                                    + thr + " then conf" + conflict + " == 1 " + "else conf" + conflict + " == 0 end;\n");
//                            conflict++;
//                            constrainId++;
//                        }
//                    }
//                }
                for (int nodeIdx = 0; nodeIdx < aregions[chainIdx][scancellIdx].length; nodeIdx++){
                    for (int g = 0; g < groupCount; g++){
                        if (pairingflag)
                            cons.write(" + ");
                        else
                            cons.write(" - ");
                        cons.write("z_" + aregions[chainIdx][scancellIdx][nodeIdx] + "_" + chainIdx + "_" + g);
                        if (nodeIdx+1 == aregions[chainIdx][scancellIdx].length && g+1 == groupCount && !pairingflag) {
                            cons.write(" ) > " + thr + " then conf" + conflict + " == 1 " + "else conf" + conflict + " == 0 end;\n");
                            conflict++;
                            constrainId++;
                        }
                    }
                }
                pairingflag = !pairingflag;
            }
        }
        return conflict;
    }

    private void ObjectiveWriter(int conflict, BufferedWriter obj) throws IOException  {
        obj.write("minimize conflict:");
        for (int conf = 0; conf < conflict-1; conf++){
            obj.write(" + conf" + conf);
        }
        obj.write(" + conf" + (conflict-1) + ";\n");
    }

    public void ZplWriter(String filename, int groupCount) throws IOException {
        BufferedWriter zpl = null;
        File zplWriter = new File(filename);
        zplWriter.createNewFile();
        zpl = new BufferedWriter(new FileWriter(zplWriter));
        long constrainId = 0;

        //write all x variables
        VariableWriter(chain2aggressors, zpl, groupCount, 'x');
        //write all z variables
        VariableWriter(chain2aggressors, zpl, groupCount, 'z');
        //write all y variables
        VariableWriter(impacts, zpl, groupCount, 'y');

        zpl.write("\n");

        //write the constrain that x of one chain should belong to the same group
        constrainId = Var2ChainConsWriter(chain2aggressors, zpl, groupCount, 'x', constrainId);
        //write the constrains that y for one chain should belong to the same group
        constrainId = Var2ChainConsWriter(impacts, zpl, groupCount, 'y', constrainId);

        //write the constrains that one x can only belong to one group
        constrainId = OnevOnegConsWriter(chain2aggressors, zpl, groupCount, 'x', constrainId);
        //write the constrains that one y can only belong to one group
        constrainId = OnevOnegConsWriter(impacts, zpl, groupCount, 'y', constrainId);

        //write the constrains of z
        constrainId = ZConsWriter(chain2aggressors, impacts, zpl, groupCount, constrainId);

        //write the self impacted constrains
        constrainId = SelfConsWriter(chain2aggressors, impacts, zpl, groupCount, constrainId);

        //write the threshold constrains
        conflict = ThrConsWeiter(aregions, zpl, groupCount, skewthreshold, constrainId);

        //write the objective function
        ObjectiveWriter(conflict, zpl);

        if (zpl != null)
            zpl.close();
    }

    public int[] SolReader(String filename, int groupCount) throws IOException {
        int[] clocking = new int[impacts.length];
        int[] flag = new int[impacts.length];
        BufferedReader sol = null;
        FileReader solreader = new FileReader(filename);
        sol = new BufferedReader(solreader);

        String variable;
        String x = "x_";
        String y = "y_";
        while ((variable = sol.readLine()) != null){
            int flagsum = 0;
            String[] temp = variable.split("\\s+");
            System.out.println(variable);
            if (temp[0].indexOf(x) >= 0 || temp[0].indexOf(y) >= 0){
                String[] readgroup = temp[0].split("_");
                System.out.println(readgroup[2]);
                clocking[Integer.parseInt(readgroup[2])] = Integer.parseInt(readgroup[3]);
                flag[Integer.parseInt(readgroup[2])] = 1;
            }
            for (int i = 0; i < flag.length; i++)
                flagsum += flag[i];
            if (flagsum == flag.length)
                break;

        }
        System.out.println(Arrays.toString(clocking));
        return clocking;
    }

}
