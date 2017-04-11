package jp.ac.kyutech.ci.datasheet;

import org.kyupi.graph.Graph;
import org.kyupi.graph.Graph.Node;
import org.kyupi.graph.GraphTools;
import org.kyupi.graph.Library;
import org.kyupi.graph.LibrarySAED;
import org.kyupi.misc.KyupiApp;

import java.io.File;
import java.io.IOException;

/**
 * Created by ZhangYucong on 2017/4/9.
 * read .v file to get reachable aggressor set of every scan chain
 */
public class ReachableAggre extends KyupiApp{
    private Graph circuit;
    private Library lib;
    private String filePath = ".\\testdata\\";
    private String extension = "_flat.v";

    public ReachableAggre(String circuitName) throws Exception {
        filePath = filePath.concat(circuitName + "\\");
        circuitName = circuitName.concat(extension);
        filePath = filePath.concat(circuitName);
        System.out.println(filePath);
        call();
    }

    protected Graph loadCircuitFromArgs(){
        File f = new File(filePath);
        System.out.println("LoadingCircuit " + f.getAbsolutePath());
        try {
            return GraphTools.loadGraph(f, lib);
        } catch (IOException e) {
            new IllegalArgumentException("Please specify a circuit with -c (String circuit name) ...");
        }
        return null;
    }

    protected void setLib(Library i){
        lib = i;
    }

    @Override
    public Void call() throws Exception {
        printWelcome();
        setLib(new LibrarySAED());
        circuit = loadCircuitFromArgs();

        Node[] nodeSet = circuit.accessInterface();
        for (Node n : nodeSet){
            System.out.println("check node: " + n);
        }

        return null;
    }
}
