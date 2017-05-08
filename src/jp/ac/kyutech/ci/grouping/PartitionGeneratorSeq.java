package jp.ac.kyutech.ci.grouping;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class PartitionGeneratorSeq extends PartitionGenerator {

	private int set_size;
	private int partition_count;

	private int[] k;
	private int[] m;

	public PartitionGeneratorSeq(int set_size, int partition_count) {
		this.set_size = set_size;
		this.partition_count = partition_count;
		initFirstPartition();
	}
	

	private void initFirstPartition() {
		int n = set_size;
		int p = partition_count;

		k = new int[set_size];
		m = new int[set_size];
		
		for (int i = 0; i <= n-p; i++) {
			k[i] = 0;
			m[i] = 0;
		}
		for (int i = n-p+1; i < n; i++) {
			k[i] = i-(n-p);
			m[i] = k[i];
		}
	}

	@Override
	public boolean hasNext() {
		return k != null;
	}

	@Override
	public int[] next() {
		int [] ret = Arrays.copyOf(k, set_size);
		if (ret == null)
			throw new NoSuchElementException();
		
		int p = partition_count;
		int n = set_size;
		
		boolean have_more = false;
		for (int i=n-1; i >= 1; i--) {
			if (k[i] < (p-1) && k[i] <= m[i-1]) {
				k[i] = k[i]+1;
				m[i] = Math.max(m[i], k[i]);
				for (int j = i+1; j <= n-(p-m[i]); j++) {
					k[j] = 0;
					m[j] = m[i];
				}
				for (int j = n-(p-m[i])+1; j <= n-1; j++) {
					k[j] = p-(n-j);
					m[j] = k[j];
				}
				have_more = true;
				break;
			}
		}
		if (!have_more)
			k = null;
		
		return ret;
	}

	@Override
	public Iterator<int[]> iterator() {
		initFirstPartition();
		return this;
	}

}