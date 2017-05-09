package jp.ac.kyutech.ci.grouping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ScanChainGrouperAlgZ1 extends ScanChainGrouper {
	/** save the status of all clk groups, if all groups are empty,
	 * curruntStatus - 1; if no group is empty, curruntStatus = 2;
	 * if not all groups are empty, curruntStatus = 3. */
	private int curruntStatus = 1;
	public int[] calculateClocking(int clockCount) {
		int clkGroupStatus[] = new int[clockCount];
		int clocking[] = new int[chains.size()];
		Util util = new Util();
		Matrix matrix = new Matrix(chains, cell2aggressorSet, chain2impactSet);

		/** initialize all available clk groups */
		List<List<Integer>> scGroup = new ArrayList<>();
		for (int i=0; i<clockCount; i++){
			List<Integer> oneGroup = new ArrayList<>();
			scGroup.add(oneGroup);
		}

		for (int i=0; i<chains.size(); i++){
			if (curruntStatus == 1) {
				Random random = new Random();
				int randomG = random.nextInt(clkGroupStatus.length);
				scGroup.get(randomG).add(i);
				clkGroupStatus[randomG] = 1;
				if (util.arraySum(clkGroupStatus) != clkGroupStatus.length && util.arraySum(clkGroupStatus) > 0) {
					curruntStatus = 3;
				}else
					curruntStatus = 2;
				continue;
			}

			if (curruntStatus == 2) {
				List<Integer> maxImpact = new ArrayList<>();
				for (int j=0; j<clkGroupStatus.length; j++){
					scGroup.get(j).add(i);
					maxImpact.add(matrix.groupEvaluate(scGroup));
					scGroup.get(j).remove(scGroup.get(j).size()-1);
				}
				System.out.println("All clk group numbers used up, group " + util.findMin(maxImpact) + " generate " +
						"smallest structure overlap");
				scGroup.get(util.findMin(maxImpact)).add(i);
				System.out.println(scGroup);
				maxImpact.clear();
				continue;
			}

			if (curruntStatus == 3) {
				int minUsedIdx = 0;
				int minEmptyIdx = 0;
				int minImpactUsed = 0;
				int minImpactEmpty = 0;

				//calculate max impacted weight for all used clk group
				for (int j=0; j<clkGroupStatus.length; j++){
					if (clkGroupStatus[j] == 1) {
						System.out.println("An used clk group: " + (j+1));
						scGroup.get(j).add(i);
						System.out.println(scGroup);
						if (minImpactUsed == 0) {
							minImpactUsed = matrix.groupEvaluate(scGroup);
							minUsedIdx = j;
						} else if (minImpactUsed > matrix.groupEvaluate(scGroup)) {
							minImpactUsed = matrix.groupEvaluate(scGroup);
							minUsedIdx = j;
						}
						scGroup.get(j).remove(scGroup.get(j).size()-1);
					}
				}

				//calculate max impacted weight for all empty clk group
				for (int j=0; j<clkGroupStatus.length; j++){
					if (clkGroupStatus[j] == 0) {
						System.out.println("An empty clk group: " + j);
						scGroup.get(j).add(i);
						if (minImpactEmpty == 0) {
							minImpactEmpty = matrix.groupEvaluate(scGroup);
							minEmptyIdx = j;
						} else if (minImpactEmpty > matrix.groupEvaluate(scGroup)) {
							minImpactEmpty = matrix.groupEvaluate(scGroup);
							minUsedIdx = j;
						}
						scGroup.get(j).remove(scGroup.get(j).size()-1);
					}
				}

				if (minImpactUsed < minImpactEmpty) {
					System.out.println("reuse an used clk group number: " + minUsedIdx);
					scGroup.get(minUsedIdx).add(i);
				}else{
					System.out.println("use a new clk group number: " + minEmptyIdx);
					scGroup.get(minEmptyIdx).add(i);
					clkGroupStatus[minEmptyIdx] = 1;
				}
				if (util.arraySum(clkGroupStatus) == clkGroupStatus.length)
					curruntStatus = 2;
				continue;

			}

		}

		for (int i=0; i<scGroup.size(); i++){
			for (int chainIdx=0; chainIdx<scGroup.get(i).size(); chainIdx++){
				clocking[chainIdx] = i;
			}
		}

		// FIXME implement algorithm

		return clocking;
	}
}