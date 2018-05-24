package jp.ac.kyutech.ci.grouping;

import java.util.*;

import org.kyupi.circuit.MutableCircuit;

import static java.lang.Math.abs;
import static java.lang.Math.max;

/**
 * Created by ZhangYucong on 2017/3/20.
 */
public class Util {
    /** deep clone the source list */
    public List<Integer> clone(List<Integer> source) {
        List<Integer> destination = new ArrayList<>();
        for (int i = 0; i < source.size(); i++) {
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
            if (needToSearch.get(i) <= tempMin){
                tempMin = needToSearch.get(i);
                index = i;
            }
        }
        return index;
    }

    /** find the max value in the needToSearch[] array */
    public int findMax(int[] needToSearch){
        int max = 0;
        for (int i=0; i<needToSearch.length; i++){
            if (needToSearch[i]>max) {
                max = needToSearch[i];
            }
        }
        return max;
    }

    /** calculate the standard deviation of the given list of scan chain group */
    public double coefficientOfDispersion(List<Integer> oneGroup){
        double coeff = 0.0;
        int[] groupArray = new int[oneGroup.size()];
        for (int i=0; i<oneGroup.size(); i++){
            groupArray[i] = oneGroup.get(i) + 1;
        }
        coeff = Math.sqrt(Math.abs(variance(groupArray)))/average(groupArray);
        return coeff;
    }

    /** calculate the average value of the given array */
    public double average(int[] needToCalculate){
        int sum = 0;
        double avg;
        for (int i=0; i<needToCalculate.length; i++)
            sum = sum + needToCalculate[i];
        return sum/needToCalculate.length;
    }

    /** calculate the square sum of the given array */
    public double squareSum(int[] needToCalculate){
        double squareSum = 0;
        for (int i=0; i<needToCalculate.length; i++){
            squareSum = squareSum + Math.pow(needToCalculate[i], 2);
        }
        return squareSum;
    }

    /** calculate the variance of the given array */
    public double variance(int[] needToCalculate){
        double variance = 0.0;

        double sqrSum = squareSum(needToCalculate);
        double average = average(needToCalculate);

        variance = (sqrSum - needToCalculate.length * Math.pow(average, 2))/needToCalculate.length;
        return variance;
    }

    public List<List<Integer>> arrayToList (int[] clocking, int clockCount){
        List<List<Integer>> scGrouping = new ArrayList<>();
        for (int clkIdx=0; clkIdx<clockCount; clkIdx++){
            List<Integer> oneGroup = new ArrayList<>();
            scGrouping.add(oneGroup);
        }

        for (int chainIdx=0; chainIdx<clocking.length; chainIdx++)
            scGrouping.get(clocking[chainIdx]).add(chainIdx);
        return scGrouping;
    }
}
