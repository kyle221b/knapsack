package knapsack.solver;

import knapsack.domain.Item;
import knapsack.domain.KnapsackResult;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class KnapsackSolverTest {
    @Test
    public void singleSolutionForSmallKnapsack() {
        KnapsackSolver solver = new DynamicProgrammingSolver();
        List<Item> items = Arrays.asList(
                Item.of(4, 8),
                Item.of(5, 10),
                Item.of(8, 15),
                Item.of(3, 4));
        KnapsackResult result = solver.solve(11, items);
        System.out.println(result);
    }
}