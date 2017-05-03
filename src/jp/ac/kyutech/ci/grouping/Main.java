package jp.ac.kyutech.ci.grouping;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.*;

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
import org.kyupi.misc.KyupiApp;
import org.kyupi.misc.StringFilter;
import org.kyupi.sim.BBPlainSim;

import jp.ac.kyutech.ci.grouping.QBWeightedSwitchingActivitySim.WeightedNodeSet;

public class Main extends KyupiApp {

	public static void main(String[] args) throws Exception {
		new Main().setArgs(args).call();
	}

	public Main() {
		options.addOption("def", true, "input def file");
		options.addOption("sim", true, "simulate given number of blocks (1 block = 32 shift cycles).");
		options.addOption("separate_clocks", true,
				"safe a new design with separate clock for each chain to given file.");
		options.addOption("clk", true, "number of staggered clocks.");
		options.addOption("prt_index", true, "specify the partition to use out of a sorted sequence of all partitions.");
		options.addOption("prt_seed", true, "generate a random partitioning based on specified seed");
		options.addOption("table", true, "choose to output the data table for latex or not, need to input design name. ");
	}

	private Graph circuit;

	@Override
	public Void call() throws Exception {

		printWelcome();

		setLib(new LibraryOldSAED());

		circuit = loadCircuitFromArgs();

		circuit.printStats();

		ScanChains chains = new ScanChains(circuit);

		log.info("ScanChainCount " + chains.size());

		CBInfo cbinfo = collectClockBuffers(circuit, chains);

		if (argsParsed().hasOption("separate_clocks")) {
			String filename = argsParsed().getOptionValue("separate_clocks");
			separateClocks(chains, cbinfo);
			FileOutputStream os = new FileOutputStream(filename);
			FormatVerilog.save(os, circuit);
			os.close();
			printGoodbye();
			return null;
		}

		Placement placement = new Placement(circuit);

		if (argsParsed().hasOption("def")) {
			String filename = argsParsed().getOptionValue("def");
			placement.parseDefFile(filename, new StringFilter() {
				@Override
				public String filter(String source) {
					return source.replaceAll("\\\\", "");
				}
			});
		}

		// SAED90 row eight is 2880nm
		// NAND2X0 and NAND2X1 cell width is 1920nm
		// def file units are nm.

		int NAND_WIDTH = 1920;

		int ROW_HEIGHT = 2880;

		int X_RADIUS = 10 * NAND_WIDTH;

		int Y_RADIUS = 3 * ROW_HEIGHT;

		int nodecount = circuit.countNodes();

		HashMap<ScanCell, HashSet<Node>> aggressors = new HashMap<>();
		HashMap<ScanChain, HashSet<Node>> reach = new HashMap<>();
		HashSet<Node> aggressorSetTemp = new HashSet<>();
		int[] aggressorCounter = new int[chains.size()];
		int[] combinationalReach = new int[chains.size()];

		for (int chainIdx = 0; chainIdx < chains.size(); chainIdx++) {
			ScanChain chain = chains.get(chainIdx);
			HashSet<Node> r = new HashSet<Node>();
			reach.put(chain, r);
			log.info("Chain " + chainIdx + " ScanInPort " + chain.in.node.queryName());
			for (ScanCell cell : chain.cells) {
				int x = placement.getX(cell.node);
				int y = placement.getY(cell.node);
				aggressors.put(cell, placement.getRectangle(x - X_RADIUS, y - Y_RADIUS, x + X_RADIUS, y + Y_RADIUS));
				log.info("  ScanCell " + cell.node.queryName() + " Aggressors " + aggressors.get(cell).size());
				aggressorSetTemp.removeAll(aggressors.get(cell));
				aggressorSetTemp.addAll(aggressors.get(cell));
				r.addAll(GraphTools.collectCombinationalOutputCone(cell.node));
			}
			aggressorCounter[chainIdx] = aggressorSetTemp.size();
			aggressorSetTemp.clear();
			int percent = r.size() * 100 / nodecount;
			log.info("  CombinationalReach " + r.size() + " " + percent + "%%");
			combinationalReach[chainIdx] = percent;
		}

		if (argsParsed().hasOption("table")) {
			String designName = argsParsed().getOptionValue("table");
			String symbol;
			if (System.getProperty("os.name").contains("Win")||System.getProperty("os.name").contains("win")){
				symbol = "\\";
			}else {
				symbol = "/";
			}
			File tableWriter = new File("."+ symbol+ "table" + symbol + designName + "_statistics.tex");
			tableWriter.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(tableWriter));

			for (int chainIdx = 0; chainIdx < chains.size(); chainIdx++) {
				out.write(chainIdx + " & " + aggressorCounter[chainIdx] + " & " + combinationalReach[chainIdx]
						+ "\\\\" + "\n");
			}
			out.flush();
			out.close();
		}

		if (argsParsed().hasOption("sim")) {
			int blocks = Integer.parseInt(argsParsed().getOptionValue("sim"));
			log.info("WSA Simulation Setup...");
			int clocking[] = getClocking(chains);
			int stimuliExpansionMap[][] = expandForWsa(chains.scanInMapping(clocking));
			int responseExpansionMap[][] = expandForWsa(chains.scanOutMapping(clocking));
			BBSource stimuli = BBSource.random(circuit.accessInterface().length, 42);
			BBSource responses = BBPlainSim.from(stimuli);
			// FIXME pad responses with leading zero pattern for proper pattern alignment
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

			QBWeightedSwitchingActivitySim sim = new QBWeightedSwitchingActivitySim(circuit, QBSource.from(shifts));
			HashMap<ScanCell, WeightedNodeSet> aggressor_wns = new HashMap<>();
			for (ScanCell sc : aggressors.keySet()) {
				WeightedNodeSet wns = sim.new WeightedNodeSet();
				for (Node n : aggressors.get(sc)) {
					wns.add(n, 1.0);
				}
				aggressor_wns.put(sc, wns);
			}
			log.info("WSA Simulation Start...");
			for (int i = 0; i < blocks; i++) {
				sim.next();
			}
			log.info("WSA Simulation Finished.");

			for (int chainIdx = 0; chainIdx < chains.size(); chainIdx++) {
				ScanChain chain = chains.get(chainIdx);
				log.info("Chain " + chainIdx + " ScanInPort " + chain.in.node.queryName());
				for (ScanCell cell : chain.cells) {
					log.info("  ScanCell " + cell.node.queryName() + " AvgWSA "
							+ aggressor_wns.get(cell).getAverageActivity());
				}
			}
		}

		return null;
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

	private int[] getClocking(ScanChains chains) {
		int clocking[] = new int[chains.size()];
		Arrays.fill(clocking, 0);
		int clocks = 1;
		if (argsParsed().hasOption("clk")) {
			clocks = Integer.parseInt(argsParsed().getOptionValue("clk"));
		}
		log.info("StaggeredClockCount " + clocks);
		if (argsParsed().hasOption("prt_seed")) {
			// do a random partitioning
			long prt_seed = Long.parseLong(argsParsed().getOptionValue("prt_seed"));
			Random rnd = new Random(prt_seed);
			for (int i = 0; i < clocking.length; i++) {
				clocking[i] = rnd.nextInt(clocks);
			}
			log.info("Partitioning random " + prt_seed);
		} else if (argsParsed().hasOption("prt_index")) {
			// do a partitioning with given sequence number
			long prt_index = Long.parseLong(argsParsed().getOptionValue("prt_index"));
			PartitionGenerator gen = new PartitionGenerator(chains.size(), clocks);
			log.info("Partitioning index " + prt_index);
			for (int i = 0; i < prt_index; i++) {
				if (gen.hasNext()) {
					clocking = gen.next();
				}
			}
			if (gen.hasNext()) {
				clocking = gen.next();
			} else {
				log.warn("prt_index out of range: " + prt_index);
			}
		} else {
			if (clocks > 1) {
				log.warn("No partitioning specified using -prt_index or -prt_seed. No clock staggering will be used.");
			}
		}
		return clocking;
	}

	private void separateClocks(ScanChains chains, CBInfo cbinfo) {
		Node clock = circuit.searchNode("clock");
		clock.remove();
		int intfNodeIdx = circuit.accessInterface().length;
		for (int chainIdx = 0; chainIdx < chains.size(); chainIdx++) {
			ScanChain chain = chains.get(chainIdx);
			Node clk = circuit.new Node(String.format("clk%03d", chainIdx), LibrarySAED.TYPE_BUF | Library.FLAG_INPUT);
			clk.setIntfPosition(intfNodeIdx++);
			for (ScanCell cell : chain.cells) {
				circuit.connect(clk, -1, cell.node, circuit.library().getClockPin(cell.node.type()));
			}
		}
	}

	@Test
	public void testB20() throws Exception {
		setArgs("-d", "testdata/b20/b20_25_10_layout.v", "-def", "testdata/b20/b20_25_10_layout.def");
		call();
		assertEquals(67741, circuit.countNodes());
		assertEquals(54, circuit.countInputs());
		assertEquals(32, circuit.countOutputs());
	}

	class CBInfo {
		public HashSet<Node> all_clock_buffers = new HashSet<>();
		public HashMap<Node, HashSet<Node>> sff_to_clock_buffer_set = new HashMap<>();
	}

	private CBInfo collectClockBuffers(Graph graph, ScanChains ci) {
		log.info("Collecting clock buffers for each scan cell");

		CBInfo cbi = new CBInfo();
		HashMap<Node, ArrayList<Node>> sff_to_cb = new HashMap<>();
		HashSet<Node> all_cb = new HashSet<>();

		for (int chain = 0; chain < ci.size(); chain++) {
			ScanChain c = ci.get(chain);
			for (int chainpos = 0; chainpos < c.cells.size(); chainpos++) {
				ScanCell sff = c.cells.get(chainpos);
				Node sffnode = sff.node;
				Node buf = sffnode.in(getLib().getClockPin(sffnode.type()));
				ArrayList<Node> cb = collectClockBuffers(buf, new ArrayList<Node>());
				StringBuffer sbuf = new StringBuffer();
				for (Node n : cb) {
					sbuf.append(" " + n.queryName());
				}
				// log.debug("buffer " + sbuf);
				sff_to_cb.put(sffnode, cb);
				all_cb.addAll(cb);
				// log.debug(sffnode + " " + clock_buffers.get(sffnode));
			}
		}

		int sff_num = sff_to_cb.size();
		int cbuf_sum = 0;
		int cbuf_max = 0;
		for (Node sff : sff_to_cb.keySet()) {
			HashSet<Node> cbset = new HashSet<>();
			cbi.sff_to_clock_buffer_set.put(sff, cbset);
			int cbuf = 0;
			for (Node cb : sff_to_cb.get(sff)) {
				if ((!cb.isPseudo() && !cb.isInput())) {
					cbset.add(cb);
					cbuf++;
				}
			}
			cbuf_sum += cbuf;
			cbuf_max = Math.max(cbuf_max, cbuf);
		}
		int cb_count = 0;
		log.debug("hash set size " + all_cb.size());
		for (Node cb : all_cb) {
			if ((!cb.isPseudo() && !cb.isInput())) {
				cbi.all_clock_buffers.add(cb);
				cb_count++;
			}
		}
		log.info("ClockBufferCount " + cb_count);
		log.info("ScanElementCount " + sff_num);
		log.info("AvgClockBufferPerScanElement " + (1.0 * cbuf_sum / sff_num));
		log.info("MaxClockBufferPerScanElement " + cbuf_max);

		return cbi;
	}

	private ArrayList<Node> collectClockBuffers(Node head, ArrayList<Node> tail) {
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
