package jp.ac.kyutech.ci.datasheet;

import org.kyupi.graph.*;
import org.kyupi.graph.Graph.Node;
import org.kyupi.graph.ScanChains.ScanCell;
import org.kyupi.graph.ScanChains.ScanChain;
import org.kyupi.misc.KyupiApp;

import java.io.File;
import java.io.IOException;

/**
 * Created by ZhangYucong on 2017/4/9.
 * read .v file to get reachable aggressor set of every scan chain
 */
public class reachableAggre{
    private Graph circuit;
    private Library lib;

    public reachableAggre(String circuitName){
        circuit = loadCircuitFromArgs(circuitName);
        lib = circuit.library();

        Node[] nodeSet = circuit.accessInterface();
        for (Node n : nodeSet){
            System.out.println("check node: " + n);
        }
    }

    protected Graph loadCircuitFromArgs(String circuitName){
        String path = ".\\testdata\\".concat(circuitName).concat("\\").concat(circuitName).concat("_flat.v");
        File f = new File(path).getAbsoluteFile();
        System.out.println("LoadingCircuit " + f.getAbsolutePath());
        try {
            return GraphTools.loadGraph(f, lib);
        } catch (IOException e) {
            new IllegalArgumentException("Please specify a circuit with -c (String circuit name) ...");
        }
        return null;
    }
}
