import java.io.IOException;
import java.util.List;

/**
 * Created by ZhangYucong on 2017/3/22.
 */
public class matrix {
    private int scNam = 0;
    private int gNam = 0;
    private String circuitName = null;

    public matrix(int scNam, int gNam, String citcuitName){
        this.scNam = scNam;
        this.gNam = gNam;
        this.circuitName = citcuitName;
    }

    public int groupEvaluate(List<Integer> scGroup) throws IOException {
        int maxWeightDiff = 0;
        ScffAggre scffAggre = new ScffAggre(circuitName);
        ScAggre scAggre = new ScAggre(circuitName);

        scffAggre.ffidAggre.get(scffAggre.scFFid.get(scGroup.get(0)).get(0));

        return maxWeightDiff;
    }
}
