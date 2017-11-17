package jp.ac.kyutech.ci.sc_grouping_clkaggre;

import jp.ac.kyutech.ci.sc_grouping_clkaggre.QBWeightedSwitchingActivitySim.WeightedNodeSet;
import org.kyupi.data.QVExpander;
import org.kyupi.data.item.QVector;
import org.kyupi.data.source.BBSource;
import org.kyupi.data.source.QBSource;
import org.kyupi.data.source.QVSource;
import org.kyupi.graph.*;
import org.kyupi.graph.Graph.Node;
import org.kyupi.graph.ScanChains.ScanCell;
import org.kyupi.graph.ScanChains.ScanChain;
import org.kyupi.misc.KyupiApp;
import org.kyupi.misc.StringFilter;
import org.kyupi.sim.BBPlainSim;

import java.io.*;
import java.util.*;
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
        // output data for excel
        options.addOption("plot", true, "output a data table for latex into given file");
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
        HashMap<ScanCell, ArrayList<Node>> cell2aggressorSet = new HashMap<>();
        calculateAggressorSets(cbinfo, chains, placement, arxnm, arynm, cbuf2aggressorSet, cell2aggressorSet);
        printAggressorAndImpactInfo(chains, cell2aggressorSet, chain2impactset);

        FastCostFunction cost = new FastCostFunction(chain2impactset, cell2aggressorSet);
        // read in grouping paremeters
        int clocks = intFromArgsOrDefault("clk", 1);
        clocks = Math.min(clocks, chains.size());
        log.info("AvailableGroupCount " + clocks);
        ScanChainGrouping grouping = null;
        ScanChainGrouper grouper = null;
        String groupingMethod = stringFromArgsOrDefault("prt_method", "random").toLowerCase();
        long startSeed = longFromArgsOrDefault("prt_start", 0);
        long groupingCases = longFromArgsOrDefault("prt_cases", 1);
        if (groupingMethod.startsWith("r")){
            log.info("GroupingMethod Random");
            log.info("GroupingStart " + startSeed);
            grouping = new RandomGrouping(chains.size(), clocks, startSeed);
        }else if (groupingMethod.startsWith("se")){
            log.info("GroupingMethod Sequential");
            log.info("GroupingStart " + startSeed);
            grouping = new seqGrouping(chains.size(), clocks);
            for (int i = 0; i < startSeed; i++){
                if (grouping.hasNext()){
                    grouping.next();
                }else {
                    log.info("startSeed out of bound");
                    grouping.iterator();
                }
            }
        }
        else if(groupingMethod.startsWith("z1")){
            log.info("GroupingMethod Z1");
            grouper = new ScanChainGrouperZ1();
        }else if (groupingMethod.startsWith("z2")){
            log.info("GroupingMethod Z2");
            grouper = new ScanChainGrouperZ2();
        } else {
            log.info("unknown grouping method: " + groupingMethod);
            printGoodbye();
            return null;
        }

        // set algorithm parameters, if an algorithm is selected
        if (grouper != null){
            grouper.setChainSize(chains.size());
            if (groupingCases > 1)
                log.warn("prt_cases is ignored, only a sigle grouping is evaluated.");
            if (startSeed > 0)
                log.warn("prt_start is ignored, only a sigle grouping is evaluated");
            groupingCases = 1;
            startSeed = 0;
        }

        //start grouping
        for (int caseId = 0; caseId < groupingCases; caseId++){
            log.info("GroupingCase " + caseId);
            int clocking[];
            if (grouper != null){
                log.info("ScanChainGrouping start with " + clocks +" available groups... ");
                clocking = grouper.calculateClocking(clocks, cost);
                log.info("ScanChainGrouping finished.");
            }else{
                if (!grouping.hasNext()){
                    log.error("prt_start+caseId out of bounds, starting over");
                    grouping.iterator();
                }
                clocking = grouping.next();
            }

            if (argsParsed().hasOption("plot")){
                String filename = argsParsed().getOptionValue("plot");
                log.info("CostDifference " + cost.evaluate(clocking, clocks, filename, caseId));
            }else{
                log.info("CostDifference " + cost.evaluate(clocking, clocks));
            }

            // print grouping info and grouping cost
            log.info("Clocking " + Arrays.toString(clocking).replaceAll("\\[", "").replaceAll("\\]", "")
                    .replaceAll(",", ""));

            if (!argsParsed().hasOption("sim"))
                continue;

            int blocks = Integer.parseInt(argsParsed().getOptionValue("sim"));

            BufferedWriter plot = null;
            if (argsParsed().hasOption("plot")){
                String filename = argsParsed().getOptionValue("plot");
                File plotWriter = new File(filename + "_correlation" + caseId + ".txt");
                plotWriter.createNewFile();
                plot = new BufferedWriter(new FileWriter(plotWriter));
            }

            log.info("WSA simulation setup... ");
            QBSource shifts = prepareExpandedRandomPatterns(chains, clocking);
            QBWeightedSwitchingActivitySim sim = new QBWeightedSwitchingActivitySim(circuit, shifts);
            HashMap<ScanCell, WeightedNodeSet> aggressorWNSet = new HashMap<>();
            for (ScanCell saff : cell2aggressorSet.keySet()){
                WeightedNodeSet wnSet = sim.new WeightedNodeSet();

                // remove duplicate node
                ArrayList<Node> dupRemovedCell2aggr = new ArrayList();
                for (int i = 0; i < cell2aggressorSet.get(saff).size(); i++){
                    if (!dupRemovedCell2aggr.contains(cell2aggressorSet.get(saff).get(i)))
                        dupRemovedCell2aggr.add(cell2aggressorSet.get(saff).get(i));
                }

                for (Node n : dupRemovedCell2aggr){
                    wnSet.add(n, 1.0);
                }
                aggressorWNSet.put(saff, wnSet);
            }
            log.info("WSA simulation started... ");
            for (int i = 0; i < blocks; i++)
                sim.next();
            log.info("WSA simulation finished.");

            double overallActivityMax= 0.0;
            for (int chainId = 0; chainId < chains.size(); chainId++){
                ScanChain chain = chains.get(chainId);
                // int clock_phase = clocking[chainId]
                log.info("Chain " + chainId + " ScanInPort " + chain.in.node.queryName());
                for (ScanCell cell : chain.cells){
                    WeightedNodeSet wns = aggressorWNSet.get(cell);
                    double activityMax = wns.getMaxActivity();
                    if(plot != null)
                        plot.write("" + cell2aggressorSet.get(cell).size() + " " + activityMax + "\n");
                }
            }
            if (plot != null)
                plot.close();

        } // caseId loop
        printGoodbye();
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

    private void calculateAggressorSets(CBInfo cbInfo, ScanChains chains, Placement placement, int arxnm, int arynm, HashMap<Node,
            HashSet<Node>> cbuf2aggressorSet, HashMap<ScanCell, ArrayList<Node>> cell2aggressorSet){
        for (int chainId = 0; chainId < chains.size(); chainId++){
            ScanChain chain = chains.get(chainId);
            for (ScanCell cell : chain.cells){
                ArrayList<Node> saffaggressors = new ArrayList<>();
                cell2aggressorSet.put(cell, saffaggressors);
                for (Node n : cbInfo.sff_to_clock_buffer_set.get(cell.node)){
                    int x = placement.getX(n);
                    int y = placement.getY(n);
                    cbuf2aggressorSet.put(n, placement.getRectangle(x - arxnm / 2, y - arynm / 2, x + arxnm / 2,
                            y+ arynm / 2));
                    saffaggressors.addAll(cbuf2aggressorSet.get(n));
                }
            }
        }
    }

    private void printAggressorAndImpactInfo(ScanChains chains, HashMap<ScanCell, ArrayList<Node>> cell2aggressorSet, HashMap<ScanChain, HashSet<Node>> chain2impactSet) throws IOException {
        BufferedWriter table = null;
        if (argsParsed().hasOption("table")){
            String filename = argsParsed().getOptionValue("table");
            File tableWriter = new File(filename);
            tableWriter.createNewFile();
            table = new BufferedWriter(new FileWriter(tableWriter));
        }
        for (int chainId = 0; chainId < chains.size(); chainId++){
            ScanChain chain = chains.get(chainId);
            log.info("Chain " + chainId + " ScanInPort " + chain.in.node.queryName());
            log.info("  ChainLength " + chain.cells.size());
            int aggmin = Integer.MAX_VALUE;
            int aggmax = 0;
            int aggsum = 0;
            int aggsizePredecessor = 0;
            int maxAggDiff = 0;
            for (ScanCell saff : chain.cells){
                int aggsize = cell2aggressorSet.get(saff).size();
                aggmin = Math.min(aggmin, aggsize);
                aggmax = Math.max(aggmax, aggsize);
                aggsum += aggsize;
                if (aggsizePredecessor == 0){
                    aggsizePredecessor = aggsize;
                    continue;
                }else if (Math.abs((aggsizePredecessor - aggsize)) > maxAggDiff){
                    maxAggDiff = Math.abs((aggsizePredecessor - aggsize));
                    aggsizePredecessor = aggsize;
                }
            }
            int aggavg = aggsum / chain.cells.size();
            log.info(" AggressorsPerScanCell Min" + aggmin + " Avg " + aggavg + " Max " + aggmax + " MaxDifference "
                    + maxAggDiff);
            log.info(" ImpactCellCount " + chain2impactSet.get(chain).size());
            if (table != null)
                table.write(chainId + " & " + " & " + aggavg + " & " + maxAggDiff + "\\\\\n");
        }
        if (table != null)
            table.close();
    }

    private QBSource prepareExpandedRandomPatterns(ScanChains chains, int[] clocking) {
        int stimuliExpansionMap[][] = expandForWsa(chains.scanInMapping(clocking));
        int responseExpansionMap[][] = expandForWsa(chains.scanOutMapping(clocking));
        BBSource stimuli = BBSource.random(circuit.accessInterface().length, 42);
        BBSource responses = BBPlainSim.from(stimuli);
        // FIXME remove first pattern from stimuli for proper alignment
        QVSource stimuliExpanded = new QVExpander(QVSource.from(stimuli), stimuliExpansionMap);
        QVSource responsesExpanded = new QVExpander(QVSource.from(responses), responseExpansionMap);

        QVSource shifts = new QVSource(stimuli.length()) {

            @Override
            public void reset() {
                stimuliExpanded.reset();
                responsesExpanded.reset();
            }

            @Override
            protected QVector compute() {
                if (!stimuliExpanded.hasNext() || !responsesExpanded.hasNext())
                    return null;
                QVector combined = pool.alloc();
                QVector s = stimuliExpanded.next();
                s.copyTo(0, combined);
                s.free();
                QVector r = responsesExpanded.next();
                combined.or(r);
                r.free();
                return combined;
            }
        };
        return QBSource.from(shifts);
    }

    /**
     * WSA simulator always calculates WSA between pattern pairs 0~1, 2~3, 4~5,
     * and so on. This function doubles the appropriate rows in a scan mapping
     * for shift-cycle WSA.
     *
     * a b -> a b
     *
     * a b c -> a b b c
     *
     * a b c d -> a b b c c d
     *
     * @param map
     * @return
     */
    private int[][] expandForWsa(int[][] map) {
        int expandedMap[][] = new int[(map.length - 1) * 2][];
        for (int i = 0; i < map.length; i++) {
            if (expandedMap.length > i * 2) {
                expandedMap[i * 2] = Arrays.copyOf(map[i], map[i].length);
            }
            if (i > 0) {
                expandedMap[i * 2 - 1] = Arrays.copyOf(map[i], map[i].length);
            }
        }
        return expandedMap;
    }
}
