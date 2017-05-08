package jp.ac.kyutech.ci.grouping;

import java.math.BigDecimal;
import java.math.BigInteger;
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
		BigInteger cnt = sterling(BigInteger.valueOf(set_size), BigInteger.valueOf(partition_count));
		log.info("PartitionCount " + (new BigDecimal(cnt)).toString());
		initFirstPartition();
	}
	
	public static BigInteger sterling(BigInteger n, BigInteger k) {
		if (n.equals(k))
			return BigInteger.ONE;
		if (k.compareTo(BigInteger.ZERO) == 0 || n.compareTo(BigInteger.ZERO) == 0)
			return BigInteger.ZERO;
		return k.multiply(sterling(n.subtract(BigInteger.ONE), k)
				.add(sterling(n.subtract(BigInteger.ONE), k.subtract(BigInteger.ONE))));
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