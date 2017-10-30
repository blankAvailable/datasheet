package jp.ac.kyutech.ci.sc_grouping_clkaggre;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.Predicate;

import org.junit.Test;
import org.kyupi.data.QVExpander;
import org.kyupi.data.item.QVector;
import org.kyupi.data.source.BBSource;
import org.kyupi.data.source.QBSource;
import org.kyupi.data.source.QVSource;
import org.kyupi.graph.FormatVerilog;
import org.kyupi.graph.Graph;
import org.kyupi.graph.Graph.Node;
import org.kyupi.graph.GraphTools;
import org.kyupi.graph.Library;
import org.kyupi.graph.LibraryOldSAED;
import org.kyupi.graph.LibrarySAED;
import org.kyupi.graph.Placement;
import org.kyupi.graph.ScanChains;
import org.kyupi.graph.ScanChains.ScanCell;
import org.kyupi.graph.ScanChains.ScanChain;
import org.kyupi.misc.ArrayTools;
import org.kyupi.misc.KyupiApp;
import org.kyupi.misc.StringFilter;
import org.kyupi.sim.BBPlainSim;

import jp.ac.kyutech.ci.grouping.QBWeightedSwitchingActivitySim.WeightedNodeSet;


public class Main extends KyupiApp {

    // SAED90 row eight is 2880nm
    // NAND2X0 and NAND2X1 cell width is 1920nm
    // def file units are nm.

    private static final int NAND_WIDTH = 1920;
    private static final int ROW_HEIGHT = 2880;

    public static void main(String[] args) throws Exception {
        new Main().setArgs(args).call();
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
        HashMap<Node, ArrayList<Node>> saff2cb = new HashMap<>();
        HashSet<Node> all_cbuf = new HashSet<>();

        for(int chain = 0; chain < sc.size(); chain++){
            ScanChain c = sc.get(chain);
            for(int chainpos = 0; chainpos < c.cells.size(); chainpos++){
                ScanCell saff = c.cells.get(chainpos);
                Node saffnode = saff.node;
                Node cbuf = saffnode.in(getLib().getClockPin(saffnode.type()));
                ArrayList<Node> cb = collectClockBuffers(cbuf, new ArrayList<Node>());
                StringBuffer strbuf = new StringBuffer();
                for(Node n : cb){
                    strbuf.append(" " + n.queryName());
                }
                saff2cb.put(saffnode, cb);
                log.debug("clk buffer " + strbuf);
                all_cbuf.addAll(cb);
            }
        }

        int saff_num = saff2cb.size();
        int cbuf_num = 0;
        int cbuf_max = 0;
        for(Node saff : saff2cb.keySet()){
            HashSet<Node> cbufset = new HashSet<>();
            clkbuf.sff_to_clock_buffer_set.put(saff, cbufset);
            int cbuf = 0;
            for(Node cb : saff2cb.get(saff)){
                if((!cb.isPseudo() && !cb.isInput())){
                    cbufset.add(cb);
                    cbuf++;
                }
            }
            cbuf_num +=cbuf;
            cbuf_max = Math.max(cbuf_max, cbuf);
        }
        int cbuf_count = 0;
        log.debug("hash set size " + all_cbuf.size());
        for(Node cb : all_cbuf){
            if((!cb.isPseudo() && !cb.isInput())){
                clkbuf.all_clock_buffers.add(cb);
                cbuf_count++;
            }
        }
        log.info("ClockBufferCount " + cbuf_count);
        log.info("ScanCellCount " + saff_num);
        log.info("MaxClockBufferPerScanCell " + cbuf_max);

        return clkbuf;
    }

    private ArrayList<Node> collectClockBuffers(Node head, ArrayList<Node> tail){
        if (head == null)
            return tail;
        tail.add(head);
        if (head.countIns() > 1) {
            if (head.isType(LibrarySAED.TYPE_CGLPPR)) {
                return collectClockBuffers(head.in(getLib().pinIndex(LibrarySAED.TYPE_CGLPPR, "CLK")), tail);
            } else {
                log.error("found odd gate in clock tree, terminating here: " + head);
                return tail;
            }
        } else {
            return collectClockBuffers(head.in(0), tail);
        }
    }

}
