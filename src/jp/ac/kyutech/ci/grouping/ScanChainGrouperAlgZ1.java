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

		List<Integer> maxImpact = new ArrayList<>();

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
				for (int j=0; j<clkGroupStatus.length; j++){
					scGroup.get(j).add(i);
					maxImpact.add(matrix.groupEvaluate(scGroup));
					scGroup.get(j).remove(scGroup.get(j).size()-1);
				}
				System.out.println("All clk group numbers used up, group " + util.findMin(maxImpact) + " generate " +
						"smallest structure overlap");
				scGroup.get(util.findMin(maxImpact)).add(i);
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
						scGroup.get(j).add(i);
						System.out.println(scGroup);
						maxImpact.add(matrix.groupEvaluate(scGroup));
						scGroup.get(j).remove(scGroup.get(j).size()-1);
					}
				}
				minUsedIdx = util.findMin(maxImpact);
				minImpactUsed = maxImpact.get(minUsedIdx);
				maxImpact.clear();

				//calculate max impacted weight for all empty clk group
				for (int j=0; j<clkGroupStatus.length; j++){
					if (clkGroupStatus[j] == 0) {
						scGroup.get(j).add(i);
						maxImpact.add(matrix.groupEvaluate(scGroup));
						scGroup.get(j).remove(scGroup.get(j).size()-1);
					}
				}
				minEmptyIdx = util.findMin(maxImpact);
				minImpactEmpty = maxImpact.get(minEmptyIdx);
				maxImpact.clear();
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