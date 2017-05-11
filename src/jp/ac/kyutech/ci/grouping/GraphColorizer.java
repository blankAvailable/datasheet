package jp.ac.kyutech.ci.grouping;

import org.apache.log4j.Logger;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

public class GraphColorizer {

	protected static Logger log = Logger.getLogger(GraphColorizer.class);

	// private BitSet adjacency = new BitSet();
	private final int vertexCount;
	private final int colorCount;
	private int edgeCount;

	private ISolver solver;

	public GraphColorizer(int vertexCount, int colorCount) {
		this.vertexCount = vertexCount;
		this.colorCount = colorCount;
		solver = SolverFactory.newDefault();
		solver.setTimeout(3600); // 1h
		solver.newVar(colorCount * vertexCount);
		// solver.setExpectedNumberOfClauses(NBCLAUSES);

		try {
			// each vertex has to have at least one color
			int[] clause = new int[colorCount];
			for (int v1 = 0; v1 < vertexCount; v1++) {
				for (int c = 0; c < colorCount; c++) {
					clause[c] = v(v1, c);
				}
				solver.addClause(new VecInt(clause));
			}

			// each vertex cannot have more than one color
			clause = new int[2];
			for (int v1 = 0; v1 < vertexCount; v1++) {
				for (int c1 = 0; c1 < colorCount; c1++) {
					for (int c2 = c1 + 1; c2 < colorCount; c2++) {
						clause[0] = -v(v1, c1);
						clause[1] = -v(v1, c2);
						solver.addClause(new VecInt(clause));
					}
				}
			}

		} catch (ContradictionException e) {
			log.error("Contradiction in SAT instance generation during GraphColorizer init!");
			e.printStackTrace();
			System.exit(1);
		}
	}

	private int[] clause = new int[2];

	public void addEdge(int v1, int v2) {
		try {
			// connected vertices cannot have the same color
			for (int c = 0; c < colorCount; c++) {
				clause[0] = -v(v1, c);
				clause[1] = -v(v2, c);
				solver.addClause(new VecInt(clause));
			}
			edgeCount++;
		} catch (ContradictionException e) {
			log.warn("Contradiction in SAT instance generation during addEdge.");
			// e.printStackTrace();
		}
	}

	public void addEdge(int[] vs, int edgeSize) {
		if (vs.length < edgeSize)
			throw new IllegalArgumentException("vs too short");

		try {
			// connected vertices cannot have the same color
			int[] cls = new int[edgeSize];
			for (int c = 0; c < colorCount; c++) {
				for (int vidx = 0; vidx < edgeSize; vidx++) {
					int vertex = vs[vidx];
					cls[vidx] = -v(vertex, c);
				}
				solver.addClause(new VecInt(cls));
			}
			edgeCount++;
		} catch (ContradictionException e) {
			log.warn("Contradiction in SAT instance generation during addEdge (hyper).");
			// e.printStackTrace();
		}
	}

	// public boolean hasEdge(int v1, int v2) {
	// if (v1 < v2)
	// return hasEdge(v2, v1);
	// // from here on: v1 >= v2
	// if (v1 >= vertexCount || v2 < 0)
	// throw new IllegalArgumentException("vertex index out of bounds");
	// return adjacency.get(v2 * vertexCount + v1);
	// }

	private int v(int vertex, int col) {
		if (vertex < 0 || vertex >= vertexCount)
			throw new IllegalArgumentException("vertex out of bounds");
		return col * vertexCount + vertex + 1;
	}

	public int[] colorize() {
		int[] colors = new int[vertexCount];
		if (colorCount < 2) {
			if (edgeCount > 0)
				return null;
			else
				return colors;
		}

		try {
			if (solver.isSatisfiable()) {
				for (int v1 = 0; v1 < vertexCount; v1++) {
					for (int c = 0; c < colorCount; c++) {
						if (solver.model(v(v1, c))) {
							colors[v1] = c;
						}
					}
				}
				return colors;
			} else {
				log.info("UnSAT.");
				return null;
			}
		} catch (TimeoutException e) {
			log.warn("Timeout in SAT solving.");
			// e.printStackTrace();
			return null;
		}
	}

	public int countEdges() {
		return edgeCount;
	}

	public int size() {
		return vertexCount;
	}
}
