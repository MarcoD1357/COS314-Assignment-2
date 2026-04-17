import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class KnapsackGA {

    // GA Parameters
    private static final int POPULATION_SIZE = 100;
    private static final double CROSSOVER_RATE = 0.85;
    private static final double MUTATION_RATE = 0.01;   
    private static final int TOURNAMENT_SIZE = 3;
    private static final int ELITISM_COUNT = 2;        

    // Fitness 
    // Returns [totalValue, totalWeight].
    // Infeasible solutions (overweight) get fitness = 0.
    private static double[] calculateFitness(int[] solution, double[] values, double[] weights, double capacity) {
        double totalValue = 0;
        double totalWeight = 0;
        for (int i = 0; i < solution.length; i++) {
            if (solution[i] == 1) {
                totalValue += values[i];
                totalWeight += weights[i];
            }
        }
        if (totalWeight > capacity) {
            return new double[]{0, totalWeight};
        }
        return new double[]{totalValue, totalWeight};
    }

    // Repair 
    // If a solution exceeds capacity, drop items with the worst
    // value-to-weight ratio until feasible.
    private static void repair(int[] solution, double[] values, double[] weights, double capacity) {
        double totalWeight = 0;
        for (int i = 0; i < solution.length; i++) {
            if (solution[i] == 1) totalWeight += weights[i];
        }
        if (totalWeight <= capacity) return;

        // Collect indices of selected items
        List<Integer> selected = new ArrayList<>();
        for (int i = 0; i < solution.length; i++) {
            if (solution[i] == 1) selected.add(i);
        }

        // Sort ascending by value/weight ratio → remove worst first
        selected.sort((a, b) -> {
            double ratioA = values[a] / weights[a];
            double ratioB = values[b] / weights[b];
            return Double.compare(ratioA, ratioB);
        });

        for (int idx : selected) {
            solution[idx] = 0;
            totalWeight -= weights[idx];
            if (totalWeight <= capacity) break;
        }
    }

    // Initialisation
    // Each individual is a random binary string, repaired to be feasible.
    private static int[][] initialisePopulation(int numItems, double[] values, double[] weights, double capacity, Random rng) {
        int[][] population = new int[POPULATION_SIZE][numItems];
        for (int p = 0; p < POPULATION_SIZE; p++) {
            for (int i = 0; i < numItems; i++) {
                population[p][i] = rng.nextBoolean() ? 1 : 0;
            }
            repair(population[p], values, weights, capacity);
        }
        return population;
    }

    // Tournament Selection
    // Pick TOURNAMENT_SIZE individuals at random; return the fittest.
    private static int[] tournamentSelect(int[][] population, double[] fitnesses, Random rng) {
        int bestIdx = rng.nextInt(POPULATION_SIZE);
        for (int t = 1; t < TOURNAMENT_SIZE; t++) {
            int candidate = rng.nextInt(POPULATION_SIZE);
            if (fitnesses[candidate] > fitnesses[bestIdx]) {
                bestIdx = candidate;
            }
        }
        return population[bestIdx].clone();
    }

    // Single-Point Crossover
    private static int[][] crossover(int[] parent1, int[] parent2, Random rng) {
        int len = parent1.length;
        int[] child1 = parent1.clone();
        int[] child2 = parent2.clone();

        if (rng.nextDouble() < CROSSOVER_RATE) {
            int point = rng.nextInt(len - 1) + 1; // crossover point in [1, len-1]
            for (int i = point; i < len; i++) {
                child1[i] = parent2[i];
                child2[i] = parent1[i];
            }
        }
        return new int[][]{child1, child2};
    }

    // Bit-Flip Mutation 
    private static void mutate(int[] individual, Random rng) {
        for (int i = 0; i < individual.length; i++) {
            if (rng.nextDouble() < MUTATION_RATE) {
                individual[i] = 1 - individual[i];
            }
        }
    }

    // Main GA Loop 
    public static Result geneticAlgorithm(double[] values, double[] weights, double capacity, long seed, double maxTimeSeconds) {
        Random rng = new Random(seed);
        long startTime = System.currentTimeMillis();
        long maxTimeMillis = (long) (maxTimeSeconds * 1000);

        int numItems = values.length;

        // 1. Initialise population
        int[][] population = initialisePopulation(numItems, values, weights, capacity, rng);

        // Evaluate initial fitness
        double[] fitnesses = new double[POPULATION_SIZE];
        double bestValue = Double.NEGATIVE_INFINITY;
        int[] bestSolution = null;

        for (int p = 0; p < POPULATION_SIZE; p++) {
            fitnesses[p] = calculateFitness(population[p], values, weights, capacity)[0];
            if (fitnesses[p] > bestValue) {
                bestValue = fitnesses[p];
                bestSolution = population[p].clone();
            }
        }

        // 2. Generational loop (time-limited)
        while ((System.currentTimeMillis() - startTime) < maxTimeMillis) {
            int[][] newPopulation = new int[POPULATION_SIZE][numItems];

            // ── Elitism: carry over the best individuals unchanged ──
            // Find indices of the top ELITISM_COUNT individuals
            Integer[] sortedIndices = new Integer[POPULATION_SIZE];
            for (int i = 0; i < POPULATION_SIZE; i++) sortedIndices[i] = i;
            java.util.Arrays.sort(sortedIndices, (a, b) -> Double.compare(fitnesses[b], fitnesses[a]));

            for (int e = 0; e < ELITISM_COUNT; e++) {
                newPopulation[e] = population[sortedIndices[e]].clone();
            }

            // Fill the rest via selection → crossover → mutation 
            int idx = ELITISM_COUNT;
            while (idx < POPULATION_SIZE) {
                int[] parent1 = tournamentSelect(population, fitnesses, rng);
                int[] parent2 = tournamentSelect(population, fitnesses, rng);

                int[][] children = crossover(parent1, parent2, rng);

                for (int c = 0; c < 2 && idx < POPULATION_SIZE; c++) {
                    mutate(children[c], rng);
                    repair(children[c], values, weights, capacity);
                    newPopulation[idx] = children[c];
                    idx++;
                }
            }

            // Replace population and re-evaluate 
            population = newPopulation;
            for (int p = 0; p < POPULATION_SIZE; p++) {
                fitnesses[p] = calculateFitness(population[p], values, weights, capacity)[0];
                if (fitnesses[p] > bestValue) {
                    bestValue = fitnesses[p];
                    bestSolution = population[p].clone();
                }
            }
        }

        double runtimeSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
        return new Result(bestValue, runtimeSeconds);
    }

    // Result Container
    public static class Result {
        public double bestValue;
        public double runtime;

        public Result(double bestValue, double runtime) {
            this.bestValue = bestValue;
            this.runtime = runtime;
        }
    }

    // Example Usage
    public static void main(String[] args) {
        double[] exampleValues  = {60, 100, 120};
        double[] exampleWeights = {10, 20, 30};
        double exampleCapacity = 50;
        long seed = 42;
        double maxTimeInSeconds = 2.0;

        Result result = geneticAlgorithm(exampleValues, exampleWeights, exampleCapacity, seed, maxTimeInSeconds);

        System.out.printf("Algorithm: GA | Seed: %d | Best Solution (Value): %.2f | Runtime: %.4f seconds%n",
                seed, result.bestValue, result.runtime);
    }
}