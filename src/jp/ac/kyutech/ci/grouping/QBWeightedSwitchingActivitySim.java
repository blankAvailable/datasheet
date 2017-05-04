package jp.ac.kyutech.ci.grouping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.kyupi.data.item.QBlock;
import org.kyupi.data.source.QBSource;
import org.kyupi.graph.Graph;
import org.kyupi.graph.Graph.Node;
import org.kyupi.sim.Simulator;
import org.kyupi.sim.Simulator.State;

public class QBWeightedSwitchingActivitySim extends QBSource {

	protected static Logger log = Logger.getLogger(QBWeightedSwitchingActivitySim.class);

	private QBSource source;
	private State state;

	private ArrayList<WeightedNodeSet> groups = new ArrayList<>();

	public class WeightedNodeSet {
		private HashMap<Node, Double> weights = new HashMap<>();

		public WeightedNodeSet() {
			groups.add(this);
		}

		public double add(Node n, double weight) {
			double w = weights.getOrDefault(n, Double.valueOf(0.0));
			weights.put(n, w + weight);
			return w + weight;
		}
		
		private ArrayList<Double> activity = new ArrayList<>();
		
		public double getActivity(int pattern_idx) {
			return activity.get(pattern_idx);
		}
		
		public double getAverageActivity() {
			double sum = 0.0;
			for (Double d: activity) {
				sum += d;
			}
			return sum / activity.size();
		}
		public double getMaxActivity() {
			double max = 0.0;
			for (Double d : activity) {
				if (d > max)
					max = d;
				}
			return max;
		}
	}

	public QBWeightedSwitchingActivitySim(Graph circuit, QBSource source) {
		super(source.length());
		if (circuit.accessInterface().length > source.length()) {
			throw new IllegalArgumentException("insufficient data width for the interface of the netlist.");
		}
		this.source = source;
		this.state = (new Simulator(circuit)).new State();
	}

	private double[] tmp = new double[32];

	@Override
	protected QBlock compute() {
		if (!source.hasNext())
			return null;
		QBlock b = source.next();
		state.loadInputsFrom(b);
		state.propagate();
		for (WeightedNodeSet g : groups) {
			Arrays.fill(tmp, 0.0);
			for (Node n : g.weights.keySet()) {
				long v = state.getV(n.level(), n.levelPosition());
				long diff = v ^ (v >> 1);
				for (int i = 0; i < 32; i++) {
					tmp[i] += g.weights.get(n) * (diff & 1L);
					diff = diff >> 2;
				}
			}
			for (int i = 0; i < 32; i++) {
				g.activity.add(tmp[i]);
			}
		}
		state.capture();
		state.storeOutputsTo(b);
		state.clear();
		return b;
	}

	@Override
	public void reset() {
		source.reset();
	}
}
