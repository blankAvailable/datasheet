package jp.ac.kyutech.ci.sc_grouping_clkaggre;

import java.util.Random;

public class ScanChainGrouperZ2 extends ScanChainGrouper {

    private static final int INITIAL_POPULATION = 64;

    private int[][] candClkings;

    // get initial population 64 individuals
    private void initialPopulation(int groupCount){
        ScanChainGrouping randGrouping = new RandomGrouping(chainSize, groupCount, 4);
        candClkings = new int[INITIAL_POPULATION][chainSize];

        for (int indsNum = 0; indsNum < INITIAL_POPULATION; indsNum++){
            candClkings[indsNum] = randGrouping.next();
        }
    }

    // natural selection
    private void naturalSelection(int groupCount, FastCostFunction cost){
        int[][] tempClkings = new int[INITIAL_POPULATION][chainSize];
        int fitness[] = new int[INITIAL_POPULATION];
        int fitnessSum = 1;
        float possibility[] = new float[INITIAL_POPULATION];
        float roulette = 0;
        Random r = new Random();

        for (int i = 0; i < INITIAL_POPULATION; i++){
            fitness[i] = 10000 - cost.evaluate(candClkings[i], groupCount);
            fitnessSum += fitness[i];
        }

        // perpare the roulette
        for (int i = 0; i < INITIAL_POPULATION; i++){
            if (i == 0){
                possibility[i] = fitness[i] / fitnessSum;
            }else {
                possibility[i] = (fitness[i] / fitnessSum) + fitness[i-1];
            }
        }

        // roulette individual selection
        for (int i = 0; i < INITIAL_POPULATION; i++){
            roulette = r.nextFloat();
            for (int j = 0; j < INITIAL_POPULATION; j++){
                if (roulette < possibility[j])
                    System.arraycopy(candClkings[j], 0, tempClkings[i], 0, candClkings[j].length);
            }
        }

        //apply new population
        for (int i = 0; i < INITIAL_POPULATION; i++){
            System.arraycopy(tempClkings[i], 0, candClkings[i], 0, tempClkings[i].length);
        }
    }

    @Override
    public int[] calculateClocking(int groupCount, FastCostFunction cost) {
        initialPopulation(groupCount);

        return new int[0];
    }
}
