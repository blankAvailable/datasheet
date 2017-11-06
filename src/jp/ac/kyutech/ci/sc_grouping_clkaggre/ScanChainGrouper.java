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

    protected int chainSize = 0;

    public void setChainSize(int chainSize){
        this.chainSize = chainSize;
    }

    public abstract int[] calculateClocking(int groupCount, FastCostFunction cost);

}
