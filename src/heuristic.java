import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by ZhangYucong on 2017/3/29.
 */
public class heuristic {
    private int scNum;
    //save the status of a group that it's empty or not
    private int[] groupStatus;
    private String circuitName = null;
    /** save the status of all groups, if all groups are empty,
     * curruntStatus = 1; if not all but still some groups are empty,
     * curruntStatus = 2; if no group is empty, curruntStatus = 0. */
    private int curruntStatus = 1;
    public List<List<Integer>> scGroup = new ArrayList<>();

    public heuristic(int scNum, int gNum, String circuitName){
        this.scNum = scNum;
        groupStatus = new int[gNum];
        this.circuitName = circuitName;
        for (int i=0; i<gNum; i++){
            List<Integer> oneGroup = new ArrayList<>();
            scGroup.add(oneGroup);
        }
    }

    public void grouping() throws IOException {
        List<Integer> maxWsaList = new ArrayList<>();
        Util util = new Util();
        Matrix matrix = new Matrix(circuitName);
        for (int i=1; i<=scNum; i++) {
            if (curruntStatus == 1) {
                Random random = new Random();
                scGroup.get(random.nextInt(groupStatus.length)).add(i);
                groupStatus[i] = 1;
                if (util.arraySum(groupStatus) != groupStatus.length && util.arraySum(groupStatus) > 0){
                    curruntStatus = 3;
                }else if (util.arraySum(groupStatus) == groupStatus.length)
                    curruntStatus = 2;
            }
            if (curruntStatus == 2){
                for (int j=0; j<groupStatus.length; j++){
                    scGroup.get(j).add(i);
                    maxWsaList.add(matrix.groupEvaluate(scGroup));
                    scGroup.get(j).remove(scGroup.get(j).size()-1);
                }
                System.out.println("All group number used up, group " + util.findMin(maxWsaList) +
                        " generate smallest difference of max WSA");
                scGroup.get(util.findMin(maxWsaList)).add(i);
            }
            if (curruntStatus == 3){
                for (int j=0; j<groupStatus.length; j++){
                    if (groupStatus[j] == 1){

                    }
                }
                if (util.arraySum(groupStatus) == groupStatus.length)
                    curruntStatus = 2;
            }
        }
    }
}
