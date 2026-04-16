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
}