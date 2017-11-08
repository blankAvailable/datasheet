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
            demo.selection();

            //Do crossover
            demo.crossover();

            //Do mutation under a random probability
            if (rn.nextInt()%7 < 5) {
                demo.mutation();
            }

            //Add fittest offspring to population
            demo.addFittestOffspring();

            //Calculate new fitness value
            demo.population.calculateFitness();

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
    //Selection
    private void selection() {

        //Select the most fittest individual
        fittest = population.getOneFittest();

        //Select the second most fittest individual
        secondFittest = population.getAnotherFittest();

    }

    // new random crossover
    private void randCrossover(){
        Random r = new Random();
        int crossOverPoint0 = 0;
        int crossOverPoint1 = 0;
        int parent0 = 0;
        int parent1 = 0;
        int checkList[] = new int[population.individuals.length];

        // two random crossover points
        while (crossOverPoint0 == crossOverPoint1){
            crossOverPoint0 = r.nextInt(population.individuals[0].geneLength);
            crossOverPoint1 = r.nextInt(population.individuals[0].geneLength);
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

    //Crossover
    void crossover() {
        Random rn = new Random();

        //Select a random crossover point
        int crossOverPoint = rn.nextInt(population.individuals[0].geneLength);
        //Swap values among parents
        for (int i = 0; i < crossOverPoint; i++) {
            int temp = fittest.genes[i];
            fittest.genes[i] = secondFittest.genes[i];
            secondFittest.genes[i] = temp;
        }
    }
    //Mutation
    private void mutation() {
        Random rn = new Random();

        //Select a random mutation point
        int mutationPoint = rn.nextInt(population.individuals[0].geneLength);
        //Flip values at the mutation point
        if (fittest.genes[mutationPoint] == 0) {
            fittest.genes[mutationPoint] = 1;
        } else {
            fittest.genes[mutationPoint] = 0;
        }
        mutationPoint = rn.nextInt(population.individuals[0].geneLength);
        if (secondFittest.genes[mutationPoint] == 0) {
            secondFittest.genes[mutationPoint] = 1;
        } else {
            secondFittest.genes[mutationPoint] = 0;
        }
    }
    //Get fittest offspring
    Individual getFittestOffspring() {
        if (fittest.fitness > secondFittest.fitness) {
            return fittest;
        }
        return secondFittest;
    }
    //Replace least fittest individual from most fittest offspring
    void addFittestOffspring() {

        //Update fitness values of offspring
        fittest.calcFitness();
        secondFittest.calcFitness();

        //Get index of least fit individual
        int leastFittestIndex = population.getLeastFittestIndex();

        //Replace least fittest individual from most fittest offspring
        population.individuals[leastFittestIndex] = getFittestOffspring();
    }
}
