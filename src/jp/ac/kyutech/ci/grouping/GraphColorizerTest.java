package jp.ac.kyutech.ci.grouping;

import static org.junit.Assert.*;

import org.junit.Test;

public class GraphColorizerTest {

	@Test
	public void test() {
		GraphColorizer g = new GraphColorizer(4, 2);
		g.addEdge(0, 1);
		g.addEdge(1, 2);
		g.addEdge(2, 3);
		int[] colors = g.colorize();
		assertNotNull(colors);
		assertTrue(colors[0] != colors[1]);
		g.addEdge(0, 2);
		colors = g.colorize();
		assertNull(colors);
		
		g = new GraphColorizer(4, 3);
		g.addEdge(0, 1);
		g.addEdge(1, 2);
		g.addEdge(2, 3);
		g.addEdge(0, 2);
		colors = g.colorize();
		assertNotNull(colors);
		assertTrue(colors[0] != colors[1]);
		assertTrue(colors[1] != colors[2]);
		assertTrue(colors[2] != colors[1]);
	}

}
