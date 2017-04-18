package jp.ac.kyutech.ci.grouping;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.Test;
import org.kyupi.graph.FormatVerilog;
import org.kyupi.graph.Graph;
import org.kyupi.graph.Graph.Node;
import org.kyupi.graph.LibraryOldSAED;
import org.kyupi.graph.LibrarySAED;
import org.kyupi.graph.ScanChains;
import org.kyupi.graph.ScanChains.ScanCell;
import org.kyupi.graph.ScanChains.ScanChain;
import org.kyupi.misc.KyupiApp;

public class Main extends KyupiApp {

	public static void main(String[] args) throws Exception {
		new Main().setArgs(args).call();
	}

	public Main() {
		options.addOption("def", true, "input def file");
		options.addOption("separate_clocks", true,
				"safe a new design with separate clock for each chain to given file.");
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

		for (int chainIdx = 0; chainIdx < chains.size(); chainIdx++) {
			ScanChain chain = chains.get(chainIdx);
			log.info("Chain " + chainIdx + " ScanInPort " + chain.in.node.queryName());
			for (ScanCell cell : chain.cells) {
				log.info("  ScanCell " + cell.node.queryName());
			}
		}

		CBInfo cbinfo = collectClockBuffers(circuit, chains);

		if (argsParsed().hasOption("separate_clocks")) {
			String filename = argsParsed().getOptionValue("separate_clocks");
			separateClocks(chains, cbinfo);
			FileOutputStream os = new FileOutputStream(filename);
			FormatVerilog.save(os, circuit, "foo");
			os.close();
		}

		return null;
	}

	private void separateClocks(ScanChains chains, CBInfo cbinfo) {
		// TODO Auto-generated method stub
	}

	@Test
	public void testB20() throws Exception {
		setArgs("-d", "testdata/b20/b20_flat.v");
		call();
		assertEquals(63372, circuit.countNodes());
		assertEquals(45, circuit.countInputs());
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
