package jp.ac.kyutech.ci.grouping;

import org.apache.log4j.Logger;
import org.kyupi.circuit.Cell;
import org.kyupi.circuit.Placement;
import org.kyupi.circuit.ScanChains;
import org.kyupi.circuit.ScanChains.ScanCell;
import org.kyupi.circuit.ScanChains.ScanChain;

import java.util.HashMap;
import java.util.HashSet;

public abstract class ScanChainGrouper {

	protected static Logger log = Logger.getLogger(ScanChainGrouper.class);

	protected int row_height;
	protected Placement placement;
	protected ScanChains chains;
	protected HashMap<ScanCell, HashSet<Cell>> cell2aggressorSet;
	protected HashMap<ScanChain, HashSet<Cell>> chain2impactSet;

	public void setRowHeight(int row_height){ this.row_height = row_height; }

	public void setPlacement(Placement placement){ this.placement = placement; }

	public void setChains(ScanChains chains) {
		this.chains = chains;
	}

	public void setCell2aggressorSet(HashMap<ScanCell, HashSet<Cell>> cell2aggressorSet) {
		this.cell2aggressorSet = cell2aggressorSet;
	}

	public void setChain2impactSet(HashMap<ScanChain, HashSet<Cell>> chain2impactSet) {
		this.chain2impactSet = chain2impactSet;
	}

	public abstract int[] calculateClocking(int clockCount);
}
