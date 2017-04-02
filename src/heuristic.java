import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by ZhangYucong on 2017/3/29.
 */
public class heuristic {
    private int scNum = 0;
    private int thr = 0;
    //save the status of a group that it's empty or not
    private int[] groupStatus;
    private String circuitName = null;
    /** save the status of all groups, if all groups are empty,
     * curruntStatus = 1; if no group is empty, curruntStatus = 2;
     * if not all group is empty, curruntStatus = 3. */
    private int curruntStatus = 1;
    public List<List<Integer>> scGroup = new ArrayList<>();

    public heuristic(int scNum, int gNum, String circuitName, int thr){
        this.scNum = scNum;
        this.thr = thr;
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
                maxWsaList.clear();
            }
            if (curruntStatus == 3){
                int minUsedIndex = 0;
                int minEmptyIndex = 0;
                int minUsedDiff = 0;
                int minEmptyDiff = 0;
                for (int j=0; j<groupStatus.length; j++){
                    if (groupStatus[j] == 1){
                        scGroup.get(j).add(i);
                        maxWsaList.add(matrix.groupEvaluate(scGroup));
                        scGroup.get(j).remove(scGroup.get(j).size()-1);
                    }
                }
                minUsedIndex = util.findMin(maxWsaList);
                minUsedDiff = maxWsaList.get(minUsedIndex);
                maxWsaList.clear();
                for (int j=0; j<groupStatus.length; j++){
                    if (groupStatus[j] == 0){
                        scGroup.get(j).add(i);
                        maxWsaList.add(matrix.groupEvaluate(scGroup));
                        scGroup.get(j).remove(scGroup.get(j).size()-1);
                    }
                }
                minEmptyIndex = util.findMin(maxWsaList);
                minEmptyDiff = maxWsaList.get(minEmptyIndex);
                maxWsaList.clear();
                if (minUsedDiff<minEmptyDiff){
                    System.out.println("reuse an used group: " + minUsedIndex);
                    scGroup.get(minUsedIndex).add(i);
                }else {
                    System.out.println("use a new group: " + minEmptyIndex);
                    scGroup.get(minEmptyIndex).add(i);
                    groupStatus[minEmptyIndex] = 1;
                }
                if (util.arraySum(groupStatus) == groupStatus.length)
                    curruntStatus = 2;
            }
        }
    }
}
