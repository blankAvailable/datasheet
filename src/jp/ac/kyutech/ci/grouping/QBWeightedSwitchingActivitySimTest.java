package jp.ac.kyutech.ci.grouping;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;
import org.kyupi.circuit.CircuitTools;
import org.kyupi.circuit.LevelizedCircuit;
import org.kyupi.circuit.LevelizedCircuit.LevelizedCell;
import org.kyupi.circuit.MutableCircuit.MutableCell;
import org.kyupi.data.item.BVector;
import org.kyupi.data.source.BVSource;
import org.kyupi.data.source.QBSource;

import jp.ac.kyutech.ci.grouping.QBWeightedSwitchingActivitySim.WeightedNodeSet;

public class QBWeightedSwitchingActivitySimTest {

	@Test
	public void test() {
		LevelizedCircuit circuit = CircuitTools.parseBench("INPUT(a) INPUT(b) OUTPUT(z) z=AND(a,b)").levelized();
		//System.out.println(circuit);
		ArrayList<BVector> v = new ArrayList<>();
		v.add(new BVector("110")); // activity 0
		v.add(new BVector("100"));
		v.add(new BVector("100")); // activity 1
		v.add(new BVector("000"));
		v.add(new BVector("110")); // activity 2
		v.add(new BVector("010"));
		BVSource pat = BVSource.from(3, v);
		QBWeightedSwitchingActivitySim sim = new QBWeightedSwitchingActivitySim(circuit, QBSource.from(pat));

		LevelizedCell and = circuit.searchCellByName("z_");
		LevelizedCell a = circuit.searchCellByName("a");
		LevelizedCell b = circuit.searchCellByName("b");

		WeightedNodeSet wns1 = sim.new WeightedNodeSet();
		wns1.add(and, 2.2);

		WeightedNodeSet wns2 = sim.new WeightedNodeSet();
		wns2.add(and, 2.2);
		wns2.add(a, 1.5);

		sim.next();

		assertEquals(2.2, wns1.getActivity(0), 0.001);
		assertEquals(0.0, wns1.getActivity(1), 0.001);
		assertEquals(2.2, wns1.getActivity(2), 0.001);
		assertEquals(0.0, wns1.getActivity(3), 0.001);
		assertEquals(0.0, wns1.getActivity(4), 0.001);

		assertEquals(2.2, wns2.getActivity(0), 0.001);
		assertEquals(1.5, wns2.getActivity(1), 0.001);
		assertEquals(3.7, wns2.getActivity(2), 0.001);
		assertEquals(0.0, wns2.getActivity(3), 0.001);
		assertEquals(0.0, wns2.getActivity(4), 0.001);

	}

}
