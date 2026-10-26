import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Extra checks: java -cp out tools/AlgorithmChecks.java [--stress] */
public class AlgorithmChecks {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        checkSingleton();
        checkMazes();
        checkDfsOrder();
        checkZones();
        if (Arrays.asList(args).contains("--stress")) checkLargeGraphs();
        System.out.println("All " + checks + " additional checks passed.");
    }

    private static void checkSingleton() {
        Input.Walls w = new Input.Walls(1, 1, new int[0], new int[0], new int[0]);
        check(Maze.breakWalls(w).length == 0, "singleton has no broken walls");
        check(Maze.cheapestMaze(w).length == 0 && Maze.primCost(w) == 0, "singleton MST costs zero");
        Escape.Result escape = Escape.escape(w, new int[0]);
        check(Arrays.equals(escape.path(), new int[]{0}), "singleton path contains the start");
        check(escape.visited() == 1 && escape.deadEnds() == 0, "singleton DFS statistics");
        Input.Slides s = new Input.Slides(1, new int[0], new int[0], new int[0]);
        int[] zones = Zones.kosaraju(s);
        check(Arrays.equals(zones, new int[]{0}), "singleton is one zone");
        check(Arrays.equals(Zones.routes(s, zones, 1), new long[]{1, 0}), "singleton route is free");
    }

    private static void checkMazes() {
        for (int rows = 1; rows <= 3; rows++) {
            for (int cols = 1; cols <= 3; cols++) {
                for (int seed = 0; seed < 5; seed++) {
                    Input.Walls w = Generator.generate(rows, cols, seed);
                    int[] broken = Maze.breakWalls(w);
                    check(broken.length == w.cells() - 1, "maze has n-1 passages");
                    int mask = 0;
                    for (int i = 0; i < broken.length; i++) {
                        check(i == 0 || broken[i - 1] < broken[i], "walls are broken in file order");
                        mask |= 1 << broken[i];
                    }
                    check(isConnected(w, mask), "broken walls connect every cell");

                    long best = Long.MAX_VALUE;
                    for (int subset = 0; subset < (1 << w.count()); subset++) {
                        if (Integer.bitCount(subset) == w.cells() - 1 && isConnected(w, subset)) {
                            long cost = 0;
                            for (int k = 0; k < w.count(); k++) {
                                if ((subset & (1 << k)) != 0) cost += w.cost()[k];
                            }
                            best = Math.min(best, cost);
                        }
                    }
                    long kruskal = 0;
                    int mstMask = 0;
                    int[] cheapest = Maze.cheapestMaze(w);
                    for (int k : cheapest) {
                        kruskal += w.cost()[k];
                        mstMask |= 1 << k;
                    }
                    check(cheapest.length == w.cells() - 1 && isConnected(w, mstMask), "Kruskal returns a tree");
                    check(kruskal == best, "Kruskal matches exhaustive enumeration");
                    check(Maze.primCost(w) == best, "Prim matches exhaustive enumeration");
                }
            }
        }
        Input.Walls largeCost = new Input.Walls(1, 3, new int[]{0, 1}, new int[]{1, 2},
                new int[]{2_000_000_000, 1_999_999_999});
        check(Maze.primCost(largeCost) == 3_999_999_999L, "MST cost uses long arithmetic");
    }

    // Independent breadth-first connectivity check for the small exhaustive tests.
    private static boolean isConnected(Input.Walls w, int mask) {
        boolean[] seen = new boolean[w.cells()];
        int[] queue = new int[w.cells()];
        int head = 0, tail = 1;
        seen[0] = true;
        while (head < tail) {
            int cell = queue[head++];
            for (int k = 0; k < w.count(); k++) {
                if ((mask & (1 << k)) == 0) continue;
                int other = w.a()[k] == cell ? w.b()[k] : w.b()[k] == cell ? w.a()[k] : -1;
                if (other >= 0 && !seen[other]) {
                    seen[other] = true;
                    queue[tail++] = other;
                }
            }
        }
        return tail == w.cells();
    }

    private static void checkDfsOrder() {
        Input.Walls w = new Input.Walls(3, 3,
                new int[]{0, 1, 1, 4, 3, 4, 5, 4},
                new int[]{1, 2, 4, 3, 6, 5, 8, 7}, new int[8]);
        Escape.Result escape = Escape.escape(w, new int[]{7, 6, 5, 4, 3, 2, 1, 0});
        check(Arrays.equals(escape.path(), new int[]{0, 1, 4, 5, 8}), "DFS path after backtracking");
        check(escape.visited() == 6, "DFS visits the right branch before the downward branch");
        check(escape.deadEnds() == 5, "dead ends include unvisited branches and endpoints");
    }

    private static void checkZones() {
        Input.Slides parallel = new Input.Slides(9,
                new int[]{0, 1, 2, 3, 0, 1, 3, 2, 4, 0, 6},
                new int[]{1, 0, 3, 2, 2, 3, 4, 8, 8, 8, 7},
                new int[]{1000, 2000, 500, 500, 20, 3, 4, 20, 5, 50, 1});
        int[] zones = Zones.kosaraju(parallel);
        int count = Arrays.stream(zones).max().orElse(-1) + 1;
        check(count == 7, "cycles, disconnected components and an isolated cell");
        check(zones[0] > 0, "the start zone need not be the first zone");
        check(Arrays.equals(Zones.routes(parallel, zones, count), new long[]{4, 12}),
                "minimum parallel slide and free movement inside a zone");

        checkRoute(new Input.Slides(3, new int[]{0, 1, 2}, new int[]{1, 2, 0},
                new int[]{100, 200, 300}), 1, 0);
        checkRoute(new Input.Slides(7, new int[]{0, 1, 2, 3, 4}, new int[]{6, 2, 3, 4, 5},
                new int[]{7, 1, 1, 1, 1}), 5, 7);
        checkRoute(new Input.Slides(3, new int[]{0, 1}, new int[]{1, 2},
                new int[]{2_000_000_000, 2_000_000_000}), 3, 4_000_000_000L);

        Random random = new Random(2026);
        for (int trial = 0; trial < 100; trial++) {
            int n = 1 + random.nextInt(8);
            List<Integer> from = new ArrayList<>(), to = new ArrayList<>(), time = new ArrayList<>();
            boolean[][] reach = new boolean[n][n];
            for (int u = 0; u < n; u++) {
                reach[u][u] = true;
                for (int v = 0; v < n; v++) {
                    if (u != v && (v == u + 1 || random.nextInt(5) == 0)) {
                        from.add(u); to.add(v); time.add(1 + random.nextInt(100));
                        reach[u][v] = true;
                    }
                }
            }
            Input.Slides s = new Input.Slides(n, toArray(from), toArray(to), toArray(time));
            for (int k = 0; k < n; k++) {
                for (int u = 0; u < n; u++) {
                    for (int v = 0; v < n; v++) reach[u][v] |= reach[u][k] && reach[k][v];
                }
            }
            int[] zoneOf = Zones.kosaraju(s);
            for (int u = 0; u < n; u++) {
                for (int v = 0; v < n; v++) {
                    check((zoneOf[u] == zoneOf[v]) == (reach[u][v] && reach[v][u]),
                            "Kosaraju agrees with transitive closure");
                }
            }
            for (int k = 0; k < s.count(); k++) {
                check(zoneOf[s.from()[k]] <= zoneOf[s.to()[k]], "zone numbers are topological");
            }
            // Bellman-Ford on the original graph, making internal slides free.
            long[] distance = new long[n];
            Arrays.fill(distance, Long.MAX_VALUE);
            distance[0] = 0;
            for (int pass = 0; pass < n - 1; pass++) {
                for (int k = 0; k < s.count(); k++) {
                    int u = s.from()[k], v = s.to()[k];
                    long cost = zoneOf[u] == zoneOf[v] ? 0 : s.time()[k];
                    if (distance[u] != Long.MAX_VALUE) distance[v] = Math.min(distance[v], distance[u] + cost);
                }
            }
            int zoneCount = Arrays.stream(zoneOf).max().orElse(-1) + 1;
            check(Zones.routes(s, zoneOf, zoneCount)[1] == distance[n - 1],
                    "zone route agrees with independent Bellman-Ford");
        }
    }

    private static int[] toArray(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void checkRoute(Input.Slides s, long stages, long cost) {
        int[] zoneOf = Zones.kosaraju(s);
        int zoneCount = Arrays.stream(zoneOf).max().orElse(-1) + 1;
        check(Arrays.equals(Zones.routes(s, zoneOf, zoneCount), new long[]{stages, cost}),
                "route stages and total cost");
    }

    private static void checkLargeGraphs() {
        int rows = 1000, cols = 1000, n = rows * cols;
        int[] a = new int[n - 1], b = new int[n - 1], cost = new int[n - 1], broken = new int[n - 1];
        Arrays.fill(cost, 1);
        for (int i = 0; i < n - 1; i++) {
            a[i] = snakeCell(i, cols);
            b[i] = snakeCell(i + 1, cols);
            broken[i] = i;
        }
        Escape.Result escape = Escape.escape(new Input.Walls(rows, cols, a, b, cost), broken);
        check(escape.path().length == n - cols + 1, "1000x1000 snake has a very deep DFS path");
        check(escape.visited() == n - cols + 1 && escape.deadEnds() == 2, "large maze DFS statistics");
        check(escape.path()[0] == 0 && escape.path()[escape.path().length - 1] == n - 1, "large path endpoints");
        System.out.println("1000x1000 maze passed: DFS path contains " + escape.path().length + " cells.");

        for (int i = 0; i < n - 1; i++) { a[i] = i; b[i] = i + 1; }
        Input.Slides chain = new Input.Slides(n, a, b, cost);
        int[] zoneOf = Zones.kosaraju(chain);
        boolean ordered = true;
        for (int i = 0; i < n; i++) {
            if (zoneOf[i] != i) ordered = false;
        }
        check(ordered, "million-cell slide chain has topologically numbered singleton zones");
        check(Arrays.equals(Zones.routes(chain, zoneOf, n), new long[]{n, n - 1}), "million-zone route");
        System.out.println("1,000,000-cell slide chain passed: Kosaraju and routes.");
    }

    private static int snakeCell(int position, int cols) {
        int row = position / cols, col = position % cols;
        return row * cols + (row % 2 == 0 ? col : cols - 1 - col);
    }
}
