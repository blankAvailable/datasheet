import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.acos;

/**
 * Created by ZhangYucong on 2017/3/20.
 */
public class Util {
    /** deep clone the source list */
    public List<Integer> clone(List<Integer> source){
        List<Integer> destination = new ArrayList<>();
        for (int i=0; i<source.size(); i++){
            destination.add(source.get(i));
        }
        return destination;
    }

    /** find the farthest group number from the currunt group num*/
    public int findFarthest(int currunt, List<Integer> available){
        int farthestGroup = 0;
        int tempDistance = 0;
        for (int i=0; i<available.size(); i++){
            if (abs(available.get(i)-currunt)>tempDistance){
                tempDistance = abs(available.get(i)-currunt);
                farthestGroup = available.get(i);
            }
        }
        return farthestGroup;
    }

    /** get the sum of an array */
    public int arraySum(int[] needToSum){
        int sum = 0;
        for (int i=0; i<needToSum.length; i++){
            sum = sum + needToSum[i];
        }
        return sum;
    }

    /** find the index of a list that has smallest value */
    public int findMin(List<Integer> needToSearch){
        int tempMin = needToSearch.get(0);
        int index = 0;
        for (int i=0; i<needToSearch.size(); i++){
            if (needToSearch.get(i)<tempMin){
                tempMin = needToSearch.get(i);
                index = i;
            }
        }
        return index;
    }
}
