package jp.ac.kyutech.ci.grouping;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
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
		options.addOption("clk", true, "number of staggered clocks/groups");
		options.addOption("arx", true, "horizontal size of aggressor regions in units of NAND2X1 widths");
		options.addOption("ary", true, "vertical size of aggressor regions in units of rows");
		options.addOption("prt_method", true, "partitioning method: seq, random, s1, z1, ... (default seq)");
		options.addOption("prt_start", true, "start partition index (seq) or start seed (random) (default 0)");
		options.addOption("prt_cases", true, "number of partitions to evaluate (for seq, random only) (default 1)");

		// specific operations to perform
		options.addOption("sep_clk", true, "safe a new design with separate chain clocks to given file and exit");
		options.addOption("sim", true, "evaluate by WSA sim with given number of blocks (1 block = 32 shift cycles)");

		// output control parameters
		options.addOption("table", true, "output a data table for latex into given file");
		options.addOption("gp_correlation", true, "output a gnuplot file for correlation between structural and WSA");
		options.addOption("max_overlap", true, "output maximum structural impact/aggressor overlap to given file");

	}

	private Graph circuit;

	@Override
	public Void call() throws Exception {

		printWelcome();

		// load circuit and print basic statistics
		setLib(new LibraryOldSAED());
		circuit = loadCircuitFromArgs();
		circuit.printStats();
		int nodecount = circuit.countNonPseudoNodes();
		log.info("NonPseudoNodes " + nodecount);

		// extract scan chains and clock tree
		ScanChains chains = new ScanChains(circuit);
		CBInfo cbinfo = collectClockBuffers(circuit, chains);
		log.info("ScanChainCount " + chains.size());
		log.info("MaxChainLength " + chains.maxChainLength());

		// sep_clk operation. Exits here if executed.
		if (argsParsed().hasOption("sep_clk")) {
			String filename = argsParsed().getOptionValue("sep_clk");
			separateClocks(chains, cbinfo);
			FileOutputStream os = new FileOutputStream(filename);
			FormatVerilog.save(os, circuit);
			os.close();
			printGoodbye();
			return null;
		}

		log.info("Calculating impact sets...");
		HashMap<ScanChain, HashSet<Node>> chain2impactSet = new HashMap<>();
		calculateImpactSets(chains, cbinfo, chain2impactSet);

		// load placement
		Placement placement = new Placement(circuit);
		if (argsParsed().hasOption("def")) {
			String filename = argsParsed().getOptionValue("def");
			placement.parseDefFile(filename, new StringFilter() {
				public String filter(String source) {
					return source.replaceAll("\\\\", "");
				}
			});
		}

		// read aggressor region size parameters
		double arx = doubleFromArgsOrDefault("arx", 600);
		double ary = doubleFromArgsOrDefault("ary", 14);
		int arxnm = (int) (arx * NAND_WIDTH);
		int arynm = (int) (ary * ROW_HEIGHT);
		log.info("AggressorRegionSize X " + arx + "  Y " + ary);
		log.info("AggressorRegionSizeNM X " + arxnm + "  Y " + arynm);

		log.info("Calculating aggressor sets...");
		HashMap<ScanCell, HashSet<Node>> cell2aggressorSet = new HashMap<>();
		HashMap<ScanChain, HashSet<Node>> chain2aggressorSet = new HashMap<>();
		calculateAggressorSets(chains, placement, arxnm, arynm, cell2aggressorSet, chain2aggressorSet);
		printAggressorAndImpactStatistics(chains, cell2aggressorSet, chain2aggressorSet, chain2impactSet);

		int maxAggressors = 0;
		for (ScanCell cell : cell2aggressorSet.keySet()) {
			int size = cell2aggressorSet.get(cell).size();
			maxAggressors = Math.max(maxAggressors, size);
		}
		log.info("MaxAggressors " + maxAggressors);

		log.info("Calculating self aggressor sets...");
		HashMap<ScanCell, HashSet<Node>> cell2selfAggressorSet = calculateSelfAggressors(chains, cell2aggressorSet,
				chain2impactSet);
		int maxSelfAggressors = printSizeHistogram(cell2selfAggressorSet, cell2aggressorSet);
		log.info("  MaxSelfAggressors " + maxSelfAggressors);

		// read partitioning parameters
		int clocks = intFromArgsOrDefault("clk", 1);
		clocks = Math.min(clocks, chains.size());
		log.info("StaggeredClockCount " + clocks);
		printPartitionCount(chains.size(), clocks);
		PartitionGenerator partGen = null;
		ScanChainGrouper partAlg = null;
		String prt_method = stringFromArgsOrDefault("prt_method", "seq").toLowerCase();
		long prt_start = longFromArgsOrDefault("prt_start", 0);
		long prt_cases = longFromArgsOrDefault("prt_cases", 1);
		if (prt_method.startsWith("se")) {
			log.info("PartitionMethod Sequential");
			log.info("PartitionStart " + prt_start);
			partGen = new PartitionGeneratorSeq(chains.size(), clocks);
			for (int i = 0; i < prt_start; i++) {
				if (partGen.hasNext()) {
					partGen.next();
				} else {
					log.error("prt_start out of bounds, starting over.");
					partGen.iterator();
				}
			}
		} else if (prt_method.startsWith("r")) {
			log.info("PartitionMethod Random");
			log.info("PartitionStart " + prt_start);
			partGen = new PartitionGeneratorRandom(chains.size(), clocks, prt_start);
		} else if (prt_method.startsWith("s1")) {
			log.info("PartitionMethod S1");
			partAlg = new ScanChainGrouperAlgS1();
		} else if (prt_method.startsWith("z1")) {
			log.info("PartitionMethod Z1");
			partAlg = new ScanChainGrouperAlgZ1();
		} else {
			log.error("unknown partitioning method: " + prt_method);
			printGoodbye();
			return null;
		}

		// set algorithm parameters, if an algorithm is selected
		if (partAlg != null) {
			partAlg.setChains(chains);
			partAlg.setCell2aggressorSet(cell2aggressorSet);
			partAlg.setChain2impactSet(chain2impactSet);
			if (prt_cases > 1)
				log.warn("prt_cases is ignored. Only a single partitioning is evaluated.");
			if (prt_start > 0)
				log.warn("prt_start is ignored. Only a single partitioning is evaluated.");
			prt_cases = 1;
			prt_start = 0;
		}

		for (int case_idx = 0; case_idx < prt_cases; case_idx++) {
			log.info("PartitioningCase " + case_idx);
			int clocking[];
			if (partAlg != null) {
				log.info("ScanChainGrouping start...");
				clocking = partAlg.calculateClocking(clocks);
				log.info("ScanChainGrouping finished.");
			} else {
				if (!partGen.hasNext()) {
					log.error("prt_start+case_idx out of bounds, starting over.");
					partGen.iterator();
				}
				clocking = partGen.next();
			}
			log.info("Calculating active aggressor sets...");

			HashMap<ScanCell, HashSet<Node>> cell2activeAggressorSet = calculateMaxActiveAggressors(chains, clocking,
					cell2aggressorSet, chain2impactSet);

			int maxActiveAggressors = printSizeHistogram(cell2activeAggressorSet, cell2aggressorSet);
			log.info("  MaxActiveAggressors " + maxActiveAggressors);

			if (argsParsed().hasOption("max_overlap")) {
				String fileName = argsParsed().getOptionValue("max_overlap");
				FileWriter fileWriter = new FileWriter(fileName, true);
				fileWriter.write("" + maxActiveAggressors + "\n");
				fileWriter.close();
			}

			if (!argsParsed().hasOption("sim"))
				continue; // to next case

			int blocks = Integer.parseInt(argsParsed().getOptionValue("sim"));

			PrintWriter gp_correlation = null;
			if (argsParsed().hasOption("gp_correlation")) {
				gp_correlation = newGpCorrelationFileWithHeader(maxAggressors, clocks, blocks);
				if (prt_cases > 1)
					log.warn("only one gp correlation plot file will be generated!");
			}

			log.info("WSA Simulation Setup...");
			QBSource shifts = prepareExpandedRandomPatterns(chains, clocking);
			QBWeightedSwitchingActivitySim sim = new QBWeightedSwitchingActivitySim(circuit, shifts);
			HashMap<ScanCell, WeightedNodeSet> aggressor_wns = new HashMap<>();
			for (ScanCell sc : cell2aggressorSet.keySet()) {
				WeightedNodeSet wns = sim.new WeightedNodeSet();
				for (Node n : cell2aggressorSet.get(sc)) {
					wns.add(n, 1.0);
				}
				aggressor_wns.put(sc, wns);
			}
			log.info("WSA Simulation Start...");
			for (int i = 0; i < blocks; i++) {
				sim.next();
			}
			log.info("WSA Simulation Finished.");

			double overallActivityMax = 0.0;
			for (int chainIdx = 0; chainIdx < chains.size(); chainIdx++) {

				ScanChain chain = chains.get(chainIdx);
				// int clock_phase = clocking[chainIdx];
				log.info("Chain " + chainIdx + " ScanInPort " + chain.in.node.queryName());
				double chainActivityMax = 0.0;
				for (ScanCell cell : chain.cells) {
					WeightedNodeSet wns = aggressor_wns.get(cell);
					double activityMax = wns.getMaxActivity();
					double activityAvg = wns.getAverageActivity();
					// double activitySum = 0.0;
					// for (int c = 0; c < wns.activitySize(); c++) {
					// if ((c % clocks) == clock_phase) {
					// activityMax = Math.max(activityMax, wns.getActivity(c));
					// activitySum += wns.getActivity(c);
					// }
					// }
					if (gp_correlation != null) {
						gp_correlation.println("" + cell2activeAggressorSet.get(cell).size() + " " + activityMax);
					}
					chainActivityMax = Math.max(chainActivityMax, activityMax);
					overallActivityMax = Math.max(overallActivityMax, activityMax);
					log.info("  ScanCell " + cell.node.queryName() + " AvgWSA " + (activityAvg) + " MaxWSA "
							+ activityMax);
				}
				log.info("  Chain " + chainIdx + " MaxWSA " + chainActivityMax);
			}
			log.info("OverallMaxWSA " + overallActivityMax);

			if (gp_correlation != null) {
				gp_correlation.println("e");
				gp_correlation.close();
			}

		} // case_idx loop

		return null;
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

	private void calculateImpactSets(ScanChains chains, CBInfo cbinfo,
			HashMap<ScanChain, HashSet<Node>> chain2impactSet) {
		for (int chainIdx = 0; chainIdx < chains.size(); chainIdx++) {
			ScanChain chain = chains.get(chainIdx);
			HashSet<Node> impactSet = new HashSet<Node>();
			chain2impactSet.put(chain, impactSet);
			for (ScanCell cell : chain.cells) {
				impactSet.add(cell.node);
				impactSet.addAll(GraphTools.collectCombinationalOutputCone(cell.node));
				if (cbinfo.sff_to_clock_buffer_set.get(cell) != null)
					impactSet.addAll(cbinfo.sff_to_clock_buffer_set.get(cell));
			}
			impactSet.removeIf(new Predicate<Node>() {
				public boolean test(Node n) {
					return n.isPseudo();
				}
			});
		}
	}

	private void calculateAggressorSets(ScanChains chains, Placement placement, int arxnm, int arynm,
			HashMap<ScanCell, HashSet<Node>> cell2aggressors, HashMap<ScanChain, HashSet<Node>> chain2aggressors) {
		for (int chainIdx = 0; chainIdx < chains.size(); chainIdx++) {
			ScanChain chain = chains.get(chainIdx);
			HashSet<Node> chainaggressors = new HashSet<Node>();
			chain2aggressors.put(chain, chainaggressors);
			for (ScanCell cell : chain.cells) {
				int x = placement.getX(cell.node);
				int y = placement.getY(cell.node);
				cell2aggressors.put(cell,
						placement.getRectangle(x - arxnm / 2, y - arynm / 2, x + arxnm / 2, y + arynm / 2));
				chainaggressors.addAll(cell2aggressors.get(cell));
			}
		}
	}

	private void printAggressorAndImpactStatistics(ScanChains chains,
			HashMap<ScanCell, HashSet<Node>> cell2aggressorSet, HashMap<ScanChain, HashSet<Node>> chain2aggressorSet,
			HashMap<ScanChain, HashSet<Node>> chain2impactSet) throws IOException {
		BufferedWriter out = null;
		if (argsParsed().hasOption("table")) {
			String filename = argsParsed().getOptionValue("table");
			File tableWriter = new File(filename);
			tableWriter.createNewFile();
			out = new BufferedWriter(new FileWriter(tableWriter));
		}

		for (int chainIdx = 0; chainIdx < chains.size(); chainIdx++) {
			ScanChain chain = chains.get(chainIdx);
			log.info("Chain " + chainIdx + " ScanInPort " + chain.in.node.queryName());
			log.info("  ChainLength " + chain.cells.size());
			int aggmin = Integer.MAX_VALUE;
			int aggmax = 0;
			int aggsum = 0;
			for (ScanCell sff : chain.cells) {
				int agg = cell2aggressorSet.get(sff).size();
				aggmin = Math.min(aggmin, agg);
				aggmax = Math.max(aggmax, agg);
				aggsum += agg;
			}
			int aggavg = aggsum / chain.cells.size();
			log.info("  AggressorsPerScanCell Min " + aggmin + " Avg " + aggavg + " Max " + aggmax);
			log.info("  AggressorsForChain SimpleSum " + aggsum + " UniqueAggressorCount "
					+ chain2aggressorSet.get(chain).size());
			log.info("  ImpactCellCount " + chain2impactSet.get(chain).size());
			if (out != null)
				out.write(chainIdx + " & " + chain2aggressorSet.get(chain).size() + " & "
						+ chain2impactSet.get(chain).size() + " \\\\\n");
		}

		if (out != null) {
			out.close();
		}
	}

	private HashMap<ScanCell, HashSet<Node>> calculateSelfAggressors(ScanChains chains,
			HashMap<ScanCell, HashSet<Node>> cell2aggressorSet, HashMap<ScanChain, HashSet<Node>> chain2impactSet) {
		HashMap<ScanCell, HashSet<Node>> selfAggressors = new HashMap<>();

		// intersect the impact set with all sff aggressor sets
		for (ScanCell sff : cell2aggressorSet.keySet()) {
			HashSet<Node> agg = new HashSet<>(cell2aggressorSet.get(sff));
			agg.retainAll(chain2impactSet.get(chains.get(sff.chainIdx())));
			selfAggressors.put(sff, agg);
		}
		return selfAggressors;
	}

	private void printPartitionCount(int size, int clocks) {
		BigInteger cnt = sterling(BigInteger.valueOf(size), BigInteger.valueOf(clocks));
		NumberFormat formatter = new DecimalFormat("0.###E0", DecimalFormatSymbols.getInstance(Locale.ROOT));

		log.info("PartitionCount " + formatter.format(cnt));
	}

	public static BigInteger sterling(BigInteger n, BigInteger k) {
		if (n.equals(k))
			return BigInteger.ONE;
		if (k.compareTo(BigInteger.ZERO) == 0 || n.compareTo(BigInteger.ZERO) == 0)
			return BigInteger.ZERO;
		return k.multiply(sterling(n.subtract(BigInteger.ONE), k)
				.add(sterling(n.subtract(BigInteger.ONE), k.subtract(BigInteger.ONE))));
	}

	private HashMap<ScanCell, HashSet<Node>> calculateMaxActiveAggressors(ScanChains chains, int[] clocking,
			HashMap<ScanCell, HashSet<Node>> cell2aggressorSet, HashMap<ScanChain, HashSet<Node>> chain2impactSet) {
		HashMap<ScanCell, HashSet<Node>> activeAggressors = new HashMap<>();

		int clocks = ArrayTools.max(clocking) + 1;

		// compute sets of possibly active nodes for each staggered clock.
		ArrayList<HashSet<Node>> clockImpactSets = new ArrayList<>();
		for (int c = 0; c < clocks; c++) {
			HashSet<Node> clockImpactSet = new HashSet<>();
			for (int chainIdx = 0; chainIdx < chains.size(); chainIdx++) {
				if (clocking[chainIdx] != c)
					continue;
				ScanChain chain = chains.get(chainIdx);
				clockImpactSet.addAll(chain2impactSet.get(chain));
			}
			clockImpactSets.add(clockImpactSet);
		}

		// intersect the computed sets with all sff aggressor sets
		for (ScanCell sff : cell2aggressorSet.keySet()) {
			HashSet<Node> aggMax = new HashSet<>();
			for (int c = 0; c < clocks; c++) {
				// int clk = clocking[sff.chainIdx()];
				HashSet<Node> agg = new HashSet<>(cell2aggressorSet.get(sff));
				agg.retainAll(clockImpactSets.get(c));
				if (agg.size() > aggMax.size())
					aggMax = agg;
			}
			activeAggressors.put(sff, aggMax);
		}
		return activeAggressors;
	}

	private int printSizeHistogram(HashMap<ScanCell, HashSet<Node>> set, HashMap<ScanCell, HashSet<Node>> base) {
		int hist[] = new int[11];
		int maxActiveAggressors = 0;
		for (ScanCell sff : set.keySet()) {
			int size = set.get(sff).size();
			maxActiveAggressors = Math.max(size, maxActiveAggressors);
			int base_size = base.get(sff).size();
			int percent = 100 * size / base_size;
			hist[percent / 10]++;
		}
		int sccount = set.size();
		int sum = 0;
		for (int i = hist.length - 1; i >= 0; i--) {
			sum += hist[i];
			int p = sum * 100 / sccount;
			log.info("  >= " + i * 10 + "%% for " + sum + " ScanCells (" + p + "%%)");
		}
		return maxActiveAggressors;
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

	private PrintWriter newGpCorrelationFileWithHeader(int maxSize, int clocks, int blocks)
			throws FileNotFoundException {
		PrintWriter gp_correlation;
		String fn = argsParsed().getOptionValue("gp_correlation");
		gp_correlation = new PrintWriter(new File(fn));
		gp_correlation.println("set terminal png size 1024,1024");
		gp_correlation.println("set output '" + fn + ".png'");
		gp_correlation.println("set title 'Correlation between structural overlap and maximum WSA. " + circuit.getName()
				+ " " + (blocks * 32) + " shifts, " + clocks + " clock(s)'");
		gp_correlation.println("set xlabel 'Structural overlap between aggressor region and active impact areas'");
		gp_correlation.println("set ylabel 'Maximum WSA in aggressor region'");

		gp_correlation.println("set xrange [0:" + maxSize + "]");
		gp_correlation.println("set yrange [0:" + maxSize + "]");
		gp_correlation.println("plot '-' w p t 'scan cell'");
		return gp_correlation;
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
}
