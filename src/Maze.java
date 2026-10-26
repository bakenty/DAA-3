import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Missions 1 and 2. Building the maze out of the walls.
 * A wall k joins the cells w.a()[k] and w.b()[k] and costs w.cost()[k]. Indexes start from 0,
 * the line number in walls.txt is the index + 1. The cells are connected by DisjointSet.
 */
public class Maze {

    /**
     * Mission 1. Take the walls in the order of the file. Break a wall when its two cells are in different sets.
     * Returns the indexes of the broken walls, in the order of breaking.
     */
    public static int[] breakWalls(Input.Walls w) {
        DisjointSet sets = new DisjointSet(w.cells());
        int[] broken = new int[Math.max(0, w.cells() - 1)];
        int count = 0;
        for (int k = 0; k < w.count() && count < broken.length; k++) {
            if (sets.union(w.a()[k], w.b()[k])) broken[count++] = k;
        }
        if (count != broken.length) throw new IllegalArgumentException("Walls do not connect all cells");
        return broken;
    }

    /**
     * Mission 2. Kruskal: the same, but take the walls from the cheapest to the most expensive.
     * Returns the indexes of the broken walls.
     */
    public static int[] cheapestMaze(Input.Walls w) {
        Integer[] order = new Integer[w.count()];
        for (int k = 0; k < order.length; k++) order[k] = k;
        Arrays.sort(order, byCost(w));

        DisjointSet sets = new DisjointSet(w.cells());
        int[] broken = new int[Math.max(0, w.cells() - 1)];
        int count = 0;
        for (int k : order) {
            if (count == broken.length) break;
            if (sets.union(w.a()[k], w.b()[k])) broken[count++] = k;
        }
        if (count != broken.length) throw new IllegalArgumentException("Walls do not connect all cells");
        return broken;
    }

    /**
     * Mission 2. Prim with a priority queue. Returns the total cost of the cheapest maze.
     * It must be equal to the total cost of cheapestMaze.
     */
    public static long primCost(Input.Walls w) {
        int n = w.cells();
        if (n <= 1) return 0;

        // Store incident wall indexes so the queue can compare their costs.
        int[] degree = new int[n];
        for (int k = 0; k < w.count(); k++) {
            degree[w.a()[k]]++;
            degree[w.b()[k]]++;
        }
        int[][] incident = new int[n][];
        for (int cell = 0; cell < n; cell++) incident[cell] = new int[degree[cell]];
        for (int k = 0; k < w.count(); k++) {
            incident[w.a()[k]][--degree[w.a()[k]]] = k;
            incident[w.b()[k]][--degree[w.b()[k]]] = k;
        }

        boolean[] visited = new boolean[n];
        PriorityQueue<Integer> queue = new PriorityQueue<>(byCost(w));
        visited[0] = true;
        int reached = 1;
        for (int k : incident[0]) queue.add(k);
        long total = 0;
        while (!queue.isEmpty() && reached < n) {
            int k = queue.remove();
            int a = w.a()[k], b = w.b()[k];
            if (visited[a] && visited[b]) continue;
            int cell = visited[a] ? b : a;
            visited[cell] = true;
            reached++;
            total += w.cost()[k];
            for (int edge : incident[cell]) {
                int other = w.a()[edge] == cell ? w.b()[edge] : w.a()[edge];
                if (!visited[other]) queue.add(edge);
            }
        }
        if (reached != n) throw new IllegalArgumentException("Walls do not connect all cells");
        return total;
    }

    private static Comparator<Integer> byCost(Input.Walls w) {
        return Comparator.comparingInt((Integer k) -> w.cost()[k]).thenComparingInt(k -> k);
    }
}
