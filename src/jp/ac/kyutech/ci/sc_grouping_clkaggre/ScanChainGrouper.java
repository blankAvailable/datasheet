package jp.ac.kyutech.ci.sc_grouping_clkaggre;

import org.apache.log4j.Logger;

public abstract class ScanChainGrouper {

    protected static Logger log = Logger.getLogger(ScanChainGrouper.class);

    protected int chainSize = 0;

    public void setChainSize(int chainSize){
        this.chainSize = chainSize;
    }

    public abstract int[] calculateClocking(int groupCount, FastCostFunction cost);

}
