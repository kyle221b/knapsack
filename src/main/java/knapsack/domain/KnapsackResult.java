package knapsack.domain;

import lombok.Value;

import java.util.List;
import java.util.Set;

@Value
public class KnapsackResult {
    private int value;
    private List<Item> solution;
    private boolean isOptimal;
}
