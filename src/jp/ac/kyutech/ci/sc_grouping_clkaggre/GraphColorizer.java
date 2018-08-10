package jp.ac.kyutech.ci.sc_grouping_clkaggre;

import org.apache.log4j.Logger;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

public class GraphColorizer {

    protected static Logger log = Logger.getLogger(GraphColorizer.class);

    private final int vertexCount;
    private final int colorCount;
    private int edgeCount;

    private ISolver graphsolver;

    public GraphColorizer(int vertexCount, int colorCount) {
        this.vertexCount = vertexCount;
        this.colorCount = colorCount;
        graphsolver = SolverFactory.newDefault();
        graphsolver.setTimeout(1800); //30min
        graphsolver.newVar(colorCount * vertexCount);

        try{
            //each vertex has to have at least one color
            int[] clause = new int[colorCount];
            for (int verIdx = 0; verIdx < vertexCount; verIdx++){
                for (int colIdx = 0; colIdx < colorCount; colIdx++){
                    clause[colIdx] = v(verIdx, colIdx);
                }
                graphsolver.addClause(new VecInt(clause));
            }

            //each vertex cannot have more than one color
            clause = new int[2];
            for (int verIdx = 0; verIdx < vertexCount; verIdx++){
                for (int colIdx1 = 0; colIdx1 < colorCount; colIdx1++){
                    for (int colIdx2 = colIdx1 + 1; colIdx2 < colorCount; colIdx2++){
                        clause[0] = -v(verIdx, colIdx1);
                        clause[1] = -v(verIdx, colIdx2);
                        graphsolver.addClause(new VecInt(clause));
                    }
                }
            }
        } catch (ContradictionException e) {
            log.error("Graph init error as one vertex do not have a color or have two or more color");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private int[] clause = new int[2];

    //add normal edge
    public void addEdge(int ver1, int ver2){
        try{
            //connected vertices cannot have the same color
            for (int colIdx = 0; colIdx < colorCount; colIdx++){
                clause[0] = -v(ver1, colIdx);
                clause[1] = -v(ver2, colIdx);
                graphsolver.addClause(new VecInt(clause));
            }
        } catch (ContradictionException e) {
            log.warn("current vertex pair already has an edge");
        }
    }

    //add hyper-edge
    public void addEdge(int[] vers, int edgeSize){
        if (vers.length < edgeSize)
            throw new IllegalArgumentException("# of vertex and edgesize do not match");
        try{
            //connected vertices cannot have the same color
            int[] cols = new int[edgeSize];
            for (int colIdx = 0; colIdx < colorCount; colIdx++){
                for (int verIdx = 0; verIdx < edgeSize; verIdx++){
                    int vertex = vers[verIdx];
                    cols[verIdx] = -v(vertex, colIdx);
                }
                graphsolver.addClause(new VecInt(cols));
            }
            edgeCount++;
        } catch (ContradictionException e) {
            log.warn("current vertex set already has a hyper-edge");
        }
    }

    private int v(int vertex, int color){
        if(vertex < 0 || vertex >= vertexCount)
            throw new IllegalArgumentException("vertex out of bounds");
        return color * vertexCount + vertex + 1;
    }

    public int[] colorize(){
        int[] colors = new int[vertexCount];
        if (colorCount < 2){
            if (edgeCount > 0)
                return null;
            else
                return colors;
        }

        try{
            if (graphsolver.isSatisfiable()){
                for (int verIdx1 = 0; verIdx1 < vertexCount; verIdx1++){
                    for (int colIdx = 0; colIdx < colorCount; colIdx++){
                        if (graphsolver.model(v(verIdx1, colIdx))){
                            colors[verIdx1] = colIdx;
                        }
                    }
                }
                return colors;
            }
        } catch (TimeoutException e) {
            log.warn("Timeout in SAT solving");
        }
        return null;
    }

    public int countEdges(){ return edgeCount; }

    public int size(){ return vertexCount; }
}
