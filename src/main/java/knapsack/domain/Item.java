package knapsack.domain;

import lombok.Value;

@Value
public class Item {
    private int weight;
    private int value;

    public static Item of(int weight, int value) {
        return new Item(weight, value);
    }
}
