package jp.ac.kyutech.ci.sc_grouping_clkaggre;

import org.apache.log4j.Logger;

import java.util.Iterator;

public abstract class ScanChainGrouping implements Iterable<int[]>, Iterator<int[]>{
        protected static Logger log = Logger.getLogger(ScanChainGrouping.class);
}
