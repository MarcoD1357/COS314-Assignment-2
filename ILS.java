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
}