package jp.ac.kyutech.ci.sc_grouping_clkaggre;

import java.util.HashMap;
import java.util.HashSet;

import org.kyupi.graph.Graph.Node;
import org.apache.log4j.Logger;
import org.kyupi.graph.ScanChains;
import org.kyupi.graph.ScanChains.ScanCell;
import org.kyupi.graph.ScanChains.ScanChain;

public abstract class ScanChainGrouper {

    protected static Logger log = Logger.getLogger(ScanChainGrouper.class);

    protected ScanChains chains;
    protected HashMap<ScanCell, HashSet<Node>> cell2aggressorSet;
    protected HashMap<ScanChain, HashSet<Node>> chain2impactSet;

    public void setChains(ScanChains chains) {
        this.chains = chains;
    }

    public void setCell2aggressorSet(HashMap<ScanCell, HashSet<Node>> cell2aggressorSet) {
        this.cell2aggressorSet = cell2aggressorSet;
    }

    public void setChain2impactSet(HashMap<ScanChain, HashSet<Node>> chain2impactSet) {
        this.chain2impactSet = chain2impactSet;
    }

    public abstract int[] calculateClocking(int groupCount);
}
