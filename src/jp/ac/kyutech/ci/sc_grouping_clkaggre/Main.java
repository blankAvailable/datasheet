package jp.ac.kyutech.ci.sc_grouping_clkaggre;

import org.kyupi.graph.*;
import org.kyupi.graph.Graph.Node;
import org.kyupi.graph.ScanChains.ScanCell;
import org.kyupi.graph.ScanChains.ScanChain;
import org.kyupi.misc.KyupiApp;
import org.kyupi.misc.StringFilter;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Predicate;


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
     * balance clock aggressor number through scan chain grouping
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

        // realize sep_clk operation. Exits here if executed.
        if(argsParsed().hasOption("sep_clk")){
            String filename = argsParsed().getOptionValue("sep_clk");
            separateClks(chains, cbinfo);
            FileOutputStream output = new FileOutputStream(filename);
            FormatVerilog.save(output, circuit);
            output.close();
            printGoodbye();
            return null;
        }

        // extract impact area/set
        log.info("Calculating impact sets...");
        HashMap<ScanChain, HashSet<Node>> chain2impactset = new HashMap<>();
        calculateImpactSets(chains, cbinfo, chain2impactset);

        // load placement info from .def file
        Placement placement = new Placement(circuit);
        if (argsParsed().hasOption("def")){
            String filename = argsParsed().getOptionValue("def");
            placement.parseDefFile(filename, new StringFilter() {
                @Override
                public String filter(String source) { return source.replaceAll("\\\\", ""); }
            });
        }

        // read aggressor region size parameters
        double arx = doubleFromArgsOrDefault("arx", 600);
        double ary = doubleFromArgsOrDefault("ary", 14);
        int arxnm = (int) (arx * NAND_WIDTH);
        int arynm = (int) (ary * ROW_HEIGHT);
        log.info("AggressorRegionSize X " + arx + " Y " + ary);
        log.info("AggressorRegionSizeNM X " + arxnm + " Y " + arynm);

        // extract clock aggressor sets
        HashMap<Node, HashSet<Node>> cbuf2aggressorSet = new HashMap<>();
        HashMap<ScanCell, HashSet<Node>> cell2aggressorSet = new HashMap<>();
        HashMap<ScanChain, HashSet<Node>> chain2aggressorSet = new HashMap<>();
        calculateAggressorSets(cbinfo, chains, placement, arxnm, arynm, cbuf2aggressorSet, cell2aggressorSet,
                chain2aggressorSet);
        printAggressorAndImpactInfo(chains, cell2aggressorSet, chain2aggressorSet, chain2impactset);

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

    private void separateClks(ScanChains chains, CBInfo cbinfo){
        Node oriClk = circuit.searchNode("clock");
        oriClk.remove();
        int intfNodeIdx = circuit.accessInterface().length;
        for(int chainId = 0; chainId < chains.size(); chainId++){
            ScanChain chain = chains.get(chainId);
            Node clk = circuit.new Node(String.format("clk%03d", chainId), LibrarySAED.TYPE_BUF | Library.FLAG_INPUT);
            clk.setIntfPosition(intfNodeIdx++);
            for(ScanCell cell : chain.cells){
                circuit.connect(clk, -1, cell.node, circuit.library().getClockPin(cell.node.type()));
            }
        }
    }

    private void calculateImpactSets(ScanChains chains, CBInfo cbinfo, HashMap<ScanChain, HashSet<Node>> chain2impactset){
        for(int chainId = 0; chainId < chains.size(); chainId++){
            ScanChain chain = chains.get(chainId);
            HashSet<Node> impactset = new HashSet<Node>();
            chain2impactset.put(chain, impactset);
            for(ScanCell cell : chain.cells){
                impactset.add(cell.node);
                impactset.addAll(GraphTools.collectCombinationalOutputCone(cell.node));
                if (cbinfo.sff_to_clock_buffer_set.get(cell) != null)
                    impactset.addAll(cbinfo.sff_to_clock_buffer_set.get(cell));
            }
            impactset.removeIf(new Predicate<Node>() {
                @Override
                public boolean test(Node node) { return node.isPseudo(); }
            });
        }
    }

    // need test
    private void calculateAggressorSets(CBInfo cbInfo, ScanChains chains, Placement placement, int arxnm, int arynm, HashMap<Node,
            HashSet<Node>> cbuf2aggressorSet, HashMap<ScanCell, HashSet<Node>> cell2aggressorSet, HashMap<ScanChain,
            HashSet<Node>> chain2aggressorSet){
        for (int chainId = 0; chainId < chains.size(); chainId++){
            ScanChain chain = chains.get(chainId);
            HashSet<Node> chainaggressors = new HashSet<>();
            chain2aggressorSet.put(chain, chainaggressors);
            for (ScanCell cell : chain.cells){
                HashSet<Node> saffaggressors = new HashSet<>();
                cell2aggressorSet.put(cell, saffaggressors);
                for (Node n : cbInfo.sff_to_clock_buffer_set.get(cell.node)){
                    int x = placement.getX(n);
                    int y = placement.getY(n);
                    cbuf2aggressorSet.put(n, placement.getRectangle(x - arxnm / 2, y - arynm / 2, x + arxnm / 2,
                            y+ arynm / 2));
                    saffaggressors.addAll(cbuf2aggressorSet.get(n));
                }
                chainaggressors.addAll(saffaggressors);
            }
        }
    }

    // need test
    private void printAggressorAndImpactInfo(ScanChains chains, HashMap<ScanCell, HashSet<Node>> cell2aggressorSet, HashMap<ScanChain,
            HashSet<Node>> chain2aggressorSet, HashMap<ScanChain, HashSet<Node>> chain2impactSet) throws IOException {
        BufferedWriter out = null;
        if (argsParsed().hasOption("table")){
            String filename = argsParsed().getOptionValue("table");
            File tableWriter = new File(filename);
            tableWriter.createNewFile();
            out = new BufferedWriter(new FileWriter(tableWriter));
        }
        for (int chainId = 0; chainId < chains.size(); chainId++){
            ScanChain chain = chains.get(chainId);
            log.info("Chain " + chainId + " ScanInPort " + chain.in.node.queryName());
            log.info("  ChainLength " + chain.cells.size());
            int aggmin = Integer.MAX_VALUE;
            int aggmax = 0;
            int aggsum = 0;
            for (ScanCell saff : chain.cells){
                int aggsize = cell2aggressorSet.get(saff).size();
                aggmin = Math.min(aggmin, aggsize);
                aggmax = Math.max(aggmax, aggsize);
                aggsum += aggsize;
            }
            int aggavg = aggsum / chain.cells.size();
            log.info(" AggressorsPerScanCell Min" + aggmin + " Avg " + aggavg + " Max " + aggmax);
            log.info(" AggressorsForChain SimpleSum " + aggsum + " UniqueAggressorCount " + chain2aggressorSet.get
                    (chain).size());
            log.info(" ImpactCellCount " + chain2impactSet.get(chain).size());
            if (out != null)
                out.write(chainId + " & " + chain2aggressorSet.get(chain).size() + " & " + aggmin + " & " + aggavg + " & " + aggmax + "\\\\\n");
        }
        if (out != null)
            out.close();
    }
}
