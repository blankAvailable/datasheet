package jp.ac.kyutech.ci.sc_grouping_clkaggre;

import org.kyupi.graph.Graph;
import org.kyupi.graph.Graph.Node;
import org.kyupi.graph.LibraryOldSAED;
import org.kyupi.graph.ScanChains;
import org.kyupi.misc.KyupiApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


public class Main extends KyupiApp {

    // SAED90 row eight is 2880nm
    // NAND2X0 and NAND2X1 cell width is 1920nm
    // def file units are nm.

    private static final int NAND_WIDTH = 1920;
    private static final int ROW_HEIGHT = 2880;

    public static void main(String[] args) throws Exception {
        new jp.ac.kyutech.ci.grouping.Main().setArgs(args).call();
    }

    public Main() {
        // input parameters
        options.addOption("def", true, "read cell placement from given def file");
        options.addOption("clk", true, "number of staggered groups");
        options.addOption("arx", true, "horizontal size of aggressor regions in units of NAND2X1 widths");
        options.addOption("ary", true, "vertical size of aggressor regions in units of rows");
        options.addOption("prt_method", true, "partitioning method: seq, random, s1, ... (default seq)");
        options.addOption("prt_start", true, "start partition index (seq) or start seed (random) (default 0)");
        options.addOption("prt_cases", true, "number of partitions to evaluate (for seq, random only) (default 1)");

        // specific operations to perform
        options.addOption("sep_clk", true, "safe a new design with separate chain clocks to given file and exit");
        options.addOption("sim", true, "evaluate by WSA sim with given number of blocks (1 block = 32 shift cycles)");

        // output control parameters
        options.addOption("table", true, "output a data table for latex into given file");
    }

    private Graph circuit;
    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    @Override
    public Void call() throws Exception {
        printWelcome();

        // load circuit and print basic statistics
        setLib(new LibraryOldSAED());
        circuit = loadCircuitFromArgs();
        circuit.printStats();
        int nodecount = circuit.countNonPseudoNodes();
        log.info("NonPseudoNodes " + nodecount);

        //extract scan chains and clock paths
        ScanChains chains = new ScanChains(circuit);
        CBInfo cbinfo = collectClockBuffers(circuit, chains);
        log.info("ScanChainCount " + chains.size());
        log.info("MaxChainLength " + chains.maxChainLength());

        return null;
    }

    private CBInfo collectClockBuffers(Graph circuit, ScanChains sc){
        log.info("Collecting clock buffers for each scan cell");

        CBInfo clkbuf = new CBInfo();
        HashMap<Node, ArrayList<Node>> sff2cb = new HashMap<>();
        HashSet<Node> all_cbuf = new HashSet<>();



        return clkbuf;
    }

}
