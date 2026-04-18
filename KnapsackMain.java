import java.io.*;
import java.util.*;

public class KnapsackMain {

    private static final long   SEED         = 42;
    private static final double MAX_TIME_SEC = 60.0;

    // ---------------------------------------------------------------
    // Holds the parsed contents of one problem instance file
    // ---------------------------------------------------------------
    static class Instance {
        String   name;
        int[]    values;
        int[]    weights;
        int      capacity;

        Instance(String name, int[] values, int[] weights, int capacity) {
            this.name     = name;
            this.values   = values;
            this.weights  = weights;
            this.capacity = capacity;
        }
    }

    // ---------------------------------------------------------------
    // Parses a knapsack file.
    //
    // Supported formats:
    //   Format A – first line is "n capacity", then n lines of "value weight"
    //   Format B – first line is just "n", second line is "capacity",
    //              then n lines of "value weight"
    //   Both formats ignore blank lines and lines that start with '#'.
    // ---------------------------------------------------------------
    static Instance parseFile(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    lines.add(line);
                }
            }
        }

        if (lines.isEmpty()) throw new IOException("Empty file: " + file.getName());

        String[] firstTokens = lines.get(0).split("\\s+");
        int n, capacity;
        int dataStart;

        if (firstTokens.length >= 2) {
            // Format A: "n capacity" on the first line
            n        = Integer.parseInt(firstTokens[0]);
            capacity = Integer.parseInt(firstTokens[1]);
            dataStart = 1;
        } else {
            // Format B: n on line 0, capacity on line 1
            n        = (int) Math.round(Double.parseDouble(firstTokens[0]));
            capacity = (int) Math.round(Double.parseDouble(firstTokens[1]));
            dataStart = 2;
        }

        int[] values  = new int[n];
        int[] weights = new int[n];

        for (int i = 0; i < n; i++) {
            String[] tokens = lines.get(dataStart + i).split("\\s+");
            values[i]  = (int) Math.round(Double.parseDouble(tokens[0]));
            weights[i] = (int) Math.round(Double.parseDouble(tokens[1]));
}

        // Strip path and extension for a clean display name
        String name = file.getName().replaceAll("\\.[^.]+$", "");
        return new Instance(name, values, weights, capacity);
    }

    // ---------------------------------------------------------------
    // Collects every instance file from a directory (or a single file)
    // ---------------------------------------------------------------
    static List<Instance> loadInstances(String pathStr) throws IOException {
        File path = new File(pathStr);
        List<Instance> instances = new ArrayList<>();

        if (path.isDirectory()) {
            File[] files = path.listFiles();
            if (files == null) throw new IOException("Cannot read directory: " + pathStr);
            Arrays.sort(files, Comparator.comparing(File::getName));
            for (File f : files) {
                if (f.isFile()) {
                    try {
                        instances.add(parseFile(f));
                    } catch (Exception e) {
                        System.err.println("Skipping " + f.getName() + ": " + e.getMessage());
                    }
                }
            }
        } else if (path.isFile()) {
            instances.add(parseFile(path));
        } else {
            throw new IOException("Path not found: " + pathStr);
        }

        return instances;
    }

    // ---------------------------------------------------------------
    // Prints a divider line of given width
    // ---------------------------------------------------------------
    static void divider(int width) {
        System.out.println("-".repeat(width));
    }

    // ---------------------------------------------------------------
    // Main
    // ---------------------------------------------------------------
    public static void main(String[] args) throws IOException {

        // --- Locate instance files -----------------------------------
        // Default: look for a folder called "instances" next to this class.
        // Override by passing the folder path as the first argument.
        String instancePath = (args.length > 0) ? args[0] : "instances";

        System.out.println("Loading instances from: " + new File(instancePath).getAbsolutePath());
        List<Instance> instances = loadInstances(instancePath);

        if (instances.isEmpty()) {
            System.err.println("No instance files found. " +
                "Pass the folder path as an argument: java KnapsackMain <path>");
            return;
        }

        System.out.printf("Found %d instance(s). Running with seed=%d, time=%.0f s%n%n",
                instances.size(), SEED, MAX_TIME_SEC);

        // --- Table header --------------------------------------------
        int W = 90;
        divider(W);
        System.out.printf("%-30s %-6s %4s %12s %12s %10s%n",
                "Problem Instance", "Algo", "Seed", "Best Value", "Known Opt*", "Time (s)");
        divider(W);

        // --- Run both algorithms on every instance -------------------
        for (Instance inst : instances) {

            // Cast to double arrays once — GA uses double[], ILS uses int[]
            double[] dValues  = new double[inst.values.length];
            double[] dWeights = new double[inst.weights.length];
            for (int i = 0; i < inst.values.length; i++) {
                dValues[i]  = inst.values[i];
                dWeights[i] = inst.weights[i];
            }

            // ILS
            KnapsackILS.Result ilsResult = KnapsackILS.iteratedLocalSearch(
                    inst.values, inst.weights, inst.capacity, SEED, MAX_TIME_SEC);

            // GA
            KnapsackGA.Result gaResult = KnapsackGA.geneticAlgorithm(
                    dValues, dWeights, inst.capacity, SEED, MAX_TIME_SEC);

            // Print ILS row
            System.out.printf("%-30s %-6s %4d %12d %12s %10.4f%n",
                    inst.name, "ILS", SEED, ilsResult.bestValue, "see notes", ilsResult.runtime);

            // Print GA row
            System.out.printf("%-30s %-6s %4d %12.0f %12s %10.4f%n",
                    inst.name, "GA",  SEED, gaResult.bestValue,  "see notes", gaResult.runtime);

            divider(W);
        }

        System.out.println();
        System.out.println("* Known optimums are provided separately in your assignment zip.");
        System.out.println("  Record the 'Best Value' column in the report table.");
    }
}