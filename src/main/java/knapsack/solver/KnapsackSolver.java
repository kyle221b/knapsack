package knapsack.solver;

import knapsack.domain.Item;
import knapsack.domain.KnapsackResult;

import java.util.List;

public interface KnapsackSolver {
    KnapsackResult solve(int maxWeight, List<Item> items);
}
