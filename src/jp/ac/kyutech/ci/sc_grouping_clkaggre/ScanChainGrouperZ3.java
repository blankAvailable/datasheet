package jp.ac.kyutech.ci.sc_grouping_clkaggre;

import java.util.Arrays;
import java.util.Random;

public class ScanChainGrouperZ3 extends ScanChainGrouper {
    @Override
    public int[] calculateClocking(int groupCount, FastCostFunction cost) {
        Random r = new Random();

        int[] clocking = new int[chainSize];
        for (int i = 0; i < chainSize; i++)
            clocking[i] = -1;
        clocking[0] = r.nextInt(groupCount);

        for (int i = 1; i < chainSize; i++){
            int tempMinCost = Integer.MAX_VALUE;

            int tempBestGroup = -1;
            for (int j = 0; j < groupCount; j++){
                clocking[i] = j;
                System.out.println("Clocking " + Arrays.toString(clocking).replaceAll("\\[", "").replaceAll("\\]", "")
                        .replaceAll(",", ""));
                if (tempMinCost > cost.evaluate(clocking, groupCount)){
                    tempMinCost = cost.evaluate(clocking, groupCount);
                    tempBestGroup = j;
                }
            }
            clocking[i] = tempBestGroup;
            System.out.println("for chain " + i + " best group is " + tempBestGroup + "\ngrouping cost is " + cost.evaluate(clocking, groupCount));
            System.out.println("GroupCost " + Arrays.toString(cost.groupCost).replaceAll("\\[", "").replaceAll("\\]", "")
                    .replaceAll(",", ""));
        }

        return clocking;
    }
}
