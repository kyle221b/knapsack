package knapsack;

import knapsack.domain.Item;
import knapsack.domain.KnapsackResult;
import knapsack.solver.*;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Integer.parseInt;

/**
 * The class <code>knapsack.Solver</code> is an implementation of a greedy algorithm to solve the knapsack problem.
 *
 */
public class Solver {
    
    /**
     * The main class
     */
    public static void main(String[] args) {
        try {
            solve(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Read the instance, solve it, and print the solution in the standard output
     */
    public static void solve(String[] args) throws IOException {
        String fileName = null;
        
        // get the temp file name
        for(String arg : args){
            if(arg.startsWith("-file=")){
                fileName = arg.substring(6);
            } 
        }
        if(fileName == null)
            return;
        
        // read the lines out of the file
        List<String> lines = new ArrayList<String>();

        BufferedReader input =  new BufferedReader(new FileReader(fileName));
        try {
            String line = null;
            while (( line = input.readLine()) != null){
                lines.add(line);
            }
        }
        finally {
            input.close();
        }
        
        
        // parse the data in the file
        String[] firstLine = lines.get(0).split("\\s+");
        int numItems = parseInt(firstLine[0]);
        int capacity = parseInt(firstLine[1]);

        List<Item> items = IntStream.rangeClosed(1, numItems).boxed()
                .map(lines::get)
                .map(line -> line.split("\\s+"))
                .map(parts -> Item.of(parseInt(parts[1]), parseInt(parts[0])))
                .collect(Collectors.toList());
        KnapsackSolver bestFirstSolver = new BfsBranchAndBoundSolver(LinearRelaxationEstimator$.MODULE$);
        KnapsackSolver depthFirstSolver = new DfsBranchAndBoundSolver(LinearRelaxationEstimator$.MODULE$);
        KnapsackSolver leastDiscrepancySolver = new LeastDiscrepancySolver();
        solveWithBenchmark(bestFirstSolver, capacity, items);
        solveWithBenchmark(depthFirstSolver, capacity, items);
        solveWithBenchmark(leastDiscrepancySolver, capacity, items);
        // prepare the solution in the specified output format
    //    System.out.println(value+" 0");
    //    for(int i=0; i < items; i++){
    //        System.out.print(taken[i]+" ");
    //    }
    //    System.out.println("");
    }

    private static void solveWithBenchmark(KnapsackSolver solver, int capacity, List<Item> items) {
        Instant startTime = Instant.now();
        KnapsackResult result = solver.solve(capacity, items);
        Duration totalTime = Duration.between(startTime, Instant.now());
        System.out.println(String.format("%s took %s seconds and obtained result: %s",
                solver.getClass().getSimpleName(), totalTime.toString(), result));
    }
}