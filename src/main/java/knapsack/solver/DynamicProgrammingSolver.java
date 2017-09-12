package knapsack.solver;

import knapsack.domain.Item;
import knapsack.domain.KnapsackResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DynamicProgrammingSolver implements KnapsackSolver {
    @Override
    public KnapsackResult solve(int maxKnapsackWeight, List<Item> items) {
        int numItems = items.size();
        int[][] solutions = new int[numItems][maxKnapsackWeight];
        for (int i = 0; i < numItems; i++) {
            solutions = fillColumn(maxKnapsackWeight, items.get(i), i, solutions);
        }
        int optimalValue = solutions[numItems - 1][maxKnapsackWeight - 1];
        List<Item> optimalItems = determineItems(maxKnapsackWeight, items, solutions);
        return new KnapsackResult(optimalValue, optimalItems, true);
    }

    private int[][] fillColumn(int maxWeight, Item currentItem, int currentItemIndex,  int[][] dpResults) {
        if (currentItemIndex == 0) {
            int tippingPoint = Math.min(currentItem.getWeight(), maxWeight);
            Arrays.fill(dpResults[0], 0, tippingPoint, 0);
            Arrays.fill(dpResults[0], tippingPoint, maxWeight, currentItem.getValue());
        } else {
            for (int knapsackWeight = 1; knapsackWeight <= maxWeight; knapsackWeight++) {
                dpResults[currentItemIndex][knapsackWeight - 1] = getOptimalValue(knapsackWeight, currentItem, currentItemIndex, dpResults);
            }
        }
        return dpResults;
    }

    private int getOptimalValue(int knapsackWeight, Item item, int itemIndex, int[][] dpResults) {
        int weightIndex = knapsackWeight - 1;
        int optimalValueWithoutCurrentItem = dpResults[itemIndex - 1][weightIndex];
        if (item.getWeight() > knapsackWeight) {
            return optimalValueWithoutCurrentItem;
        } else if (item.getWeight() == knapsackWeight) {
            return Math.max(optimalValueWithoutCurrentItem, item.getValue());
        } else {
            int optimalValueWithCurrentItem = item.getValue() + dpResults[itemIndex - 1][weightIndex - item.getWeight()];
            return Math.max(optimalValueWithoutCurrentItem, optimalValueWithCurrentItem);
        }
    }

    private List<Item> determineItems(int maxKnapsackWeight, List<Item> items, int[][] results) {
        int weight = maxKnapsackWeight;
        List<Item> solution = new ArrayList<>();
        for (int i = items.size() - 1; i > 0 && weight > 0; i--) {
            if (results[i][weight - 1] != results[i - 1][weight - 1]) {
                Item item = items.get(i);
                solution.add(item);
                weight -= item.getWeight();
            }
        }
        if (weight > 0) {
            solution.add(items.get(0));
        }
        return solution;
    }
}
