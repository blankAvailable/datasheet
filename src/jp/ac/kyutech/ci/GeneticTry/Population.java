package jp.ac.kyutech.ci.GeneticTry;

import java.util.Random;

public class Population {
    Random random = new Random();
    Individual[] individuals = new Individual[10];
    int fittest = 0;
    //Initialize population
    public void initializePopulation(int size) {
        for (int i = 0; i < individuals.length; i++) {
            individuals[i] = new Individual();
        }
    }

    public Individual[] naturalSelection() {
        int fitness[] = new int[individuals.length];
        int fitnessSum = 0;
        float possibility[] = new float[individuals.length];
        float roulette = 0;

        Individual[] selectedInd = new Individual[individuals.length];

        // prepare the list of fitness
        for (int i = 0; i < individuals.length; i++) {
            fitness[i] = individuals[i].fitness;
            fitnessSum += fitness[i];
        }
        // prepare the roulette
        for (int i = 0; i < individuals.length; i++) {
            if (i == 0) {
                possibility[i] = fitness[i] / fitnessSum;
            }else {
                possibility[i] = (fitness[i] / fitnessSum) + fitness[i - 1];
            }
        }
        // select individual
        for (int i = 0; i < selectedInd.length; i++){
            // roulette selection
            roulette = random.nextFloat();
            for (int j = 0; j < selectedInd.length; j++)
                if (roulette < possibility[i])
                    selectedInd[i] = individuals[j];
        }
        return selectedInd;
    }

    //Get the fittest individual
    public Individual getOneFittest() {
        int fitness[] = new int[individuals.length];
        int fitnessSum = 0;
        float possibility[] = new float[individuals.length];
        float roulette = 0;
        int oneFit = 0;

        // prepare the list of fitness
        for (int i = 0; i < individuals.length; i++) {
            fitness[i] = individuals[i].fitness;
            fitnessSum += fitness[i];
        }
        // prepare the roulette
        for (int i = 0; i < individuals.length; i++) {
            if (i == 0) {
                possibility[i] = fitness[i] / fitnessSum;
            }else {
                possibility[i] = (fitness[i] / fitnessSum) + fitness[i - 1];
            }
        }

        // roulette selection
        roulette = random.nextFloat();
        for (int i = 0; i < individuals.length; i++){
            if (roulette < possibility[i])
                oneFit = i;
        }
        fittest = individuals[oneFit].fitness;
        return individuals[oneFit];
    }
    //Get the second most fittest individual
    public Individual getAnotherFittest() {
        int maxFit1 = 0;
        int maxFit2 = 0;
        for (int i = 0; i < individuals.length; i++) {
            if (individuals[i].fitness > individuals[maxFit1].fitness) {
                maxFit2 = maxFit1;
                maxFit1 = i;
            } else if (individuals[i].fitness > individuals[maxFit2].fitness) {
                maxFit2 = i;
            }
        }
        return individuals[maxFit2];
    }
    //Get index of least fittest individual
    public int getLeastFittestIndex() {
        int minFit = 0;
        for (int i = 0; i < individuals.length; i++) {
            if (minFit >= individuals[i].fitness) {
                minFit = i;
            }
        }
        return minFit;
    }
    //Calculate fitness of each individual
    public void calculateFitness() {
        for (int i = 0; i < individuals.length; i++) {
            individuals[i].calcFitness();
        }
        getOneFittest();
    }
}
