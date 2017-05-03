package jp.ac.kyutech.ci.grouping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.Test;

public class PartitionGeneratorTest {

	public static long sterling(int n, int k) {
		if (n == k)
			return 1;
		if (k == 0 || n == 0)
			return 0;
		return k * sterling(n - 1, k) + sterling(n - 1, k - 1);
	}


	public void runTestWith(int set_size, int partition_count) {
		PartitionGenerator gen = new PartitionGenerator(set_size, partition_count);

		HashSet<int[]> unique = new HashSet<>();
		for (int[] partition : gen) {
			assertEquals(set_size, partition.length);
			for (int p : partition) {
				assertTrue("partition number out of range: " + p + " (should: 0 <= p < " + partition_count + ")",
						p >= 0 && p < partition_count);
			}
			assertFalse("duplicate partition " + partition.toString(), unique.contains(partition));
			unique.add(partition);
		}
		assertEquals("wrong number of partitions.", sterling(set_size, partition_count), unique.size());
	}
	
	@Test
	public void testTrivials() {
		runTestWith(1,1);
		runTestWith(2,2);
		runTestWith(3,3);
		runTestWith(2,1);
		runTestWith(3,1);
	}
	
	@Test
	public void testBigger() {
		runTestWith(3,2);
		runTestWith(5,2);
		runTestWith(10,4);
	}

	@Test
	public void testSterling() {
		assertEquals(1, sterling(0, 0));
		assertEquals(0, sterling(1, 0));
		assertEquals(1, sterling(1, 1));
		assertEquals(1, sterling(2, 2));
		assertEquals(3, sterling(3, 2));
		assertEquals(350, sterling(7, 4));
	}

}
