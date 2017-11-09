package jp.ac.kyutech.ci.GeneticTry;

import java.util.Random;
/**
 *
 * @author Vijini
 */
//Main class
public class SimpleDemoGA {
    Population population = new Population();
    Individual fittest;
    Individual secondFittest;
    int generationCount = 0;
    public static void main(String[] args) {
        Random rn = new Random();

        SimpleDemoGA demo = new SimpleDemoGA();

        //Initialize population
        demo.population.initializePopulation(10);

        //Calculate fitness of each individual
        demo.population.calculateFitness();

        System.out.println("Generation: " + demo.generationCount + " Fittest: " + demo.population.fittest);
        //While population gets an individual with maximum fitness
        while (demo.population.fittest < 5) {
            ++demo.generationCount;

            //Do selection
            demo.population = demo.naturalSelection(demo.population.individuals.length, demo.population);

            //Do crossover
            //demo.crossover();
            //demo.randCrossover();

            //Do mutation under a random probability
            //if (rn.nextInt()%7 < 5) {
            //    demo.mutation();
            //}
            //demo.randMutation(1);

            //Add fittest offspring to population
            //demo.addFittestOffspring();

            //Calculate new fitness value
           //demo.population.calculateFitness();

            System.out.println("Generation: " + demo.generationCount + " Fittest: " + demo.population.fittest);
        }
        System.out.println("\nSolution found in generation " + demo.generationCount);
        System.out.println("Fitness: "+demo.population.getOneFittest().fitness);
        System.out.print("Genes: ");
        for (int i = 0; i < 5; i++) {
            System.out.print(demo.population.getOneFittest().genes[i]);
        }
        System.out.println("");
    }

    // New roulette natural selection
    private Population naturalSelection(int indsNum, Population population){
        int fitness[] = new int[indsNum];
        int fitnessSum = 0;
        float possibility[] = new float[indsNum];
        float roulette = 0;
        Random random = new Random();

        Population newPopulation = new Population();

        for (int i = 0; i < indsNum; i++){
            fitness[i] = population.individuals[i].fitness;
            fitnessSum += fitness[i];
        }
        System.out.println("check fitness SUM " + fitnessSum);

        // prepare the roulette
        for (int i = 0; i < indsNum; i++) {
            if (i == 0) {
                possibility[i] = fitness[i] / fitnessSum;
            }else {
                possibility[i] = (fitness[i] / fitnessSum) + fitness[i - 1];
            }
        }
        // select individual
        for (int i = 0; i < indsNum; i++) {
            // roulette selection
            roulette = random.nextFloat();
            for (int j = 0; j < indsNum; j++)
                if (roulette < possibility[i])
                    newPopulation.individuals[i] = population.individuals[j];
        }
        return newPopulation;
    }

    // New random crossover
    private void randCrossover(){
        Random r = new Random();
        int crossOverPoint0 = 0;
        int crossOverPoint1 = 0;
        int parent0 = 0;
        int parent1 = 0;
        int checkList[] = new int[population.individuals.length];

        // two random crossover points
        while (crossOverPoint0 == crossOverPoint1){
            crossOverPoint0 = r.nextInt(population.individuals[1].geneLength);
            crossOverPoint1 = r.nextInt(population.individuals[1].geneLength);
        }

        // keep crossOverPoint0 smaller than crossOverPoint1
        if (crossOverPoint0 > crossOverPoint1){
            int temp = 0;
            temp = crossOverPoint0;
            crossOverPoint0 = crossOverPoint1;
            crossOverPoint1 = temp;
        }

        // choose parents randomly and do the cross over
        for (int i = 0; i < (population.individuals.length) / 2; i ++){
            while (parent0 == parent1 || checkList[parent0] == 1 || checkList[parent1] == 1){
                parent0 = r.nextInt(population.individuals.length);
                parent1 = r.nextInt(population.individuals.length);
            }
            checkList[parent0] = 1;
            checkList[parent1] = 1;
            // swap values among parents
            for (int j = 0; j <= crossOverPoint1; j++){
                if (j >= crossOverPoint0){
                    int temp = population.individuals[parent0].genes[j];
                    population.individuals[parent0].genes[j] = population.individuals[parent1].genes[j];
                    population.individuals[parent1].genes[j] = temp;
                }
            }

        }
    }

    // New random mutation
    private void randMutation(int groupCount){
        Random r = new Random();

        int mutationPoint0 = r.nextInt(population.individuals[0].geneLength);
        int mutationPoint1 = r.nextInt(population.individuals[0].geneLength);

        for (int i = 0; i < population.individuals.length; i ++){
            if (r.nextInt()%7 < 5)
                population.individuals[i].genes[mutationPoint0] = r.nextInt()%groupCount;
            if (r.nextInt()%8 < 5)
                population.individuals[i].genes[mutationPoint1] = r.nextInt()%groupCount;
        }

    }


}
