package jp.ac.kyutech.ci.grouping;

import java.util.Iterator;

import org.apache.log4j.Logger;

public abstract class PartitionGenerator implements Iterable<int[]>, Iterator<int[]> {

	protected static Logger log = Logger.getLogger(PartitionGenerator.class);
}
