package jp.ac.kyutech.ci.grouping;

import java.util.Iterator;
import java.util.Random;

public class PartitionGeneratorRandom extends PartitionGenerator {

	private int partition_count;
	private long seed;
	private long init_seed;

	private int[] prt;

	public PartitionGeneratorRandom(int set_size, int partition_count, long init_seed) {
		this.partition_count = partition_count;
		this.init_seed = init_seed;
		this.prt = new int[set_size];
		seed = init_seed;
	}

	@Override
	public boolean hasNext() {
		return true;
	}

	@Override
	public int[] next() {
		Random rnd = new Random(seed);
		for (int i = 0; i < prt.length; i++) {
			prt[i] = rnd.nextInt(partition_count);
		}
		seed++;
		return prt;
	}

	@Override
	public Iterator<int[]> iterator() {
		seed = init_seed;
		return this;
	}

}