import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ZhangYucong on 2017/3/22.
 */
public class Matrix {
    private int scNam = 0;
    private int gNam = 0;
    private String circuitName = null;

    public Matrix(int scNam, int gNam, String citcuitName){
        this.scNam = scNam;
        this.gNam = gNam;
        this.circuitName = citcuitName;
    }

    /** calculate the biggest WSA difference between every two neighboring flip flips in one grouping */
    public int groupEvaluate(List<List<Integer>> scGroup) throws IOException {
        int maxWsaDiff = 0;
        List<Integer> reachableAggre = new ArrayList<>();
        ScffAggre scffAggre = new ScffAggre(circuitName);
        ScAggre scAggre = new ScAggre(circuitName);

        for (int i=0; i<scGroup.size(); i++){
            for (int j=0; j<scGroup.get(i).size(); j++){
                reachableAggre.removeAll(scAggre.scAggreId.get(Integer.parseInt(scGroup.get(i).get(j).toString())));
                reachableAggre.addAll(scAggre.scAggreId.get(Integer.parseInt(scGroup.get(i).get(j).toString())));
            }
            reachableAggre.stream().forEach(System.out::println);
            System.out.println();
            reachableAggre.clear();
        }

        return maxWsaDiff;
    }
}
