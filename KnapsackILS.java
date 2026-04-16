import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class KnapsackILS {

    // Helper method to calculate fitness. Returns an array: [totalValue, totalWeight]
    private static int[] calculateFitness(int[] solution, int[] values, int[] weights, int capacity) {
        int totalValue = 0;
        int totalWeight = 0;
        for (int i = 0; i < solution.length; i++) {
            if (solution[i] == 1) {
                totalValue += values[i];
                totalWeight += weights[i];
            }
        }
        if (totalWeight > capacity) {
            return new int[]{0, totalWeight};
        }
        return new int[]{totalValue, totalWeight};
    }

    // Generates a feasible initial solution by randomly packing items
    private static int[] generateInitialSolution(int numItems, int[] values, int[] weights, int capacity, Random random) {
        int[] solution = new int[numItems];
        List<Integer> items = new ArrayList<>();
        for (int i = 0; i < numItems; i++) {
            items.add(i);
        }
        Collections.shuffle(items, random);

        int currentWeight = 0;
        for (int i : items) {
            if (currentWeight + weights[i] <= capacity) {
                solution[i] = 1;
                currentWeight += weights[i];
            }
        }
        return solution;
    }

    // First-improvement local search (1-flip neighborhood)
    private static int[] localSearch(int[] solution, int[] values, int[] weights, int capacity) {
        int numItems = solution.length;
        int[] bestSolution = solution.clone();
        int bestValue = calculateFitness(bestSolution, values, weights, capacity)[0];

        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 0; i < numItems; i++) {
                int[] neighbor = bestSolution.clone();
                // Flip the bit
                neighbor[i] = 1 - neighbor[i];

                int[] fitness = calculateFitness(neighbor, values, weights, capacity);
                int neighborVal = fitness[0];
                int neighborWeight = fitness[1];

                // Accept first improvement
                if (neighborWeight <= capacity && neighborVal > bestValue) {
                    bestSolution = neighbor;
                    bestValue = neighborVal;
                    improved = true;
                    break; // Restart search from the new better solution
                }
            }
        }
        return bestSolution;
    }

    // Perturbs the solution to escape local optima and repairs it if over capacity
    private static int[] perturb(int[] solution, int[] weights, int[] values, int capacity, double perturbationStrength, Random random) {
        int numItems = solution.length;
        int[] perturbed = solution.clone();

        // Calculate how many bits to flip
        int numFlips = Math.max(1, (int) (numItems * perturbationStrength));
        
        // Pick random unique indices to flip
        List<Integer> allIndices = new ArrayList<>();
        for (int i = 0; i < numItems; i++) allIndices.add(i);
        Collections.shuffle(allIndices, random);
        
        for (int i = 0; i < numFlips; i++) {
            int idx = allIndices.get(i);
            perturbed[idx] = 1 - perturbed[idx];
        }

        // Repair mechanism if capacity is exceeded
        int currentWeight = calculateFitness(perturbed, values, weights, capacity)[1];
        if (currentWeight > capacity) {
            // Find items currently in the knapsack
            List<Integer> inKnapsack = new ArrayList<>();
            for (int i = 0; i < numItems; i++) {
                if (perturbed[i] == 1) {
                    inKnapsack.add(i);
                }
            }

            // Sort by value/weight ratio (ascending) so we remove the worst items first
            inKnapsack.sort((a, b) -> {
                double ratioA = (double) values[a] / weights[a];
                double ratioB = (double) values[b] / weights[b];
                return Double.compare(ratioA, ratioB);
            });

            // Remove items until we fit within the capacity
            for (int i : inKnapsack) {
                perturbed[i] = 0;
                currentWeight -= weights[i];
                if (currentWeight <= capacity) {
                    break;
                }
            }
        }
        return perturbed;
    }

    // Main ILS Algorithm
    public static Result iteratedLocalSearch(int[] values, int[] weights, int capacity, long seed, double maxTimeSeconds) {
        Random random = new Random(seed);
        long startTimeMillis = System.currentTimeMillis();
        long maxTimeMillis = (long) (maxTimeSeconds * 1000);
        
        int numItems = values.length;
        double perturbationStrength = 0.1; // 10% of items

        // 1. Generate Initial Solution
        int[] currentSolution = generateInitialSolution(numItems, values, weights, capacity, random);
        currentSolution = localSearch(currentSolution, values, weights, capacity);
        
        int[] bestSolution = currentSolution.clone();
        int bestValue = calculateFitness(bestSolution, values, weights, capacity)[0];

        // 2. Main ILS Loop
        while ((System.currentTimeMillis() - startTimeMillis) < maxTimeMillis) {
            // Perturbation
            int[] perturbedSolution = perturb(currentSolution, weights, values, capacity, perturbationStrength, random);

            // Local Search on perturbed solution
            int[] newSolution = localSearch(perturbedSolution, values, weights, capacity);
            int newValue = calculateFitness(newSolution, values, weights, capacity)[0];

            // Acceptance Criterion (accept if better or equal)
            if (newValue >= bestValue) {
                currentSolution = newSolution.clone();
                if (newValue > bestValue) {
                    bestSolution = newSolution.clone();
                    bestValue = newValue;
                }
            }
        }

        double runtimeSeconds = (System.currentTimeMillis() - startTimeMillis) / 1000.0;
        return new Result(bestValue, runtimeSeconds);
    }
}