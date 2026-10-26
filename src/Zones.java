import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Missions 4 and 5. The slides: a directed graph. Cells are 0 .. n-1, slide i goes from s.from()[i] to s.to()[i]
 * and takes s.time()[i]. There are no slides from a cell to itself, and the exit can always be reached from the start.
 *
 * Ready for you: Graphs.slidesFrom(s)[u] is the list of the slides that leave cell u, and Graphs.slidesInto(s)[v] is the list
 * of the slides that enter cell v (reversed graph). The lists hold slide numbers i; the cell at the other end is s.to()[i]
 * or s.from()[i].
 */
public class Zones {

    /**
     * Mission 4. Kosaraju. Returns zoneOf: zoneOf[cell] = number of the zone, numbers 0 .. zones-1.
     * Number the zones in the order in which the second pass finds them. That order is a topological order
     * of the zone graph: every slide between two zones goes from a smaller number to a larger number.
     *
     * Plan: 1) DFS on the slides, write the cells in the order in which they are finished (use a stack, not recursion);
     * 2) go through the cells from the last finished to the first; if a cell has no zone yet, DFS from it on the reversed
     * slides (slidesInto) and give every cell it reaches the next zone number.
     * Is DAG in Main is 1 when the number of zones equals the number of cells (no cycle), so you do not need a separate check.
     */
    public static int[] kosaraju(Input.Slides s) {
        int n = s.n();
        int[][] outgoing = Graphs.slidesFrom(s);
        int[][] incoming = Graphs.slidesInto(s);
        boolean[] seen = new boolean[n];
        int[] stack = new int[n];
        int[] nextEdge = new int[n];
        int[] finished = new int[n];
        int finishCount = 0;

        for (int root = 0; root < n; root++) {
            if (seen[root]) continue;
            int depth = 1;
            stack[0] = root;
            seen[root] = true;
            while (depth > 0) {
                int cell = stack[depth - 1];
                if (nextEdge[cell] < outgoing[cell].length) {
                    int edge = outgoing[cell][nextEdge[cell]++];
                    int to = s.to()[edge];
                    if (!seen[to]) {
                        seen[to] = true;
                        stack[depth++] = to;
                    }
                } else {
                    // Append on return from DFS, not when the cell is first seen.
                    finished[finishCount++] = cell;
                    depth--;
                }
            }
        }

        int[] zoneOf = new int[n];
        Arrays.fill(zoneOf, -1);
        int zone = 0;
        for (int i = finishCount - 1; i >= 0; i--) {
            int root = finished[i];
            if (zoneOf[root] != -1) continue;
            int depth = 1;
            stack[0] = root;
            zoneOf[root] = zone;
            while (depth > 0) {
                int cell = stack[--depth];
                for (int edge : incoming[cell]) {
                    int from = s.from()[edge];
                    if (zoneOf[from] == -1) {
                        zoneOf[from] = zone;
                        stack[depth++] = from;
                    }
                }
            }
            zone++;
        }
        return zoneOf;
    }

    /**
     * Mission 5. The graph of zones: the edge X to Y costs the cheapest slide from X to Y, walking inside a zone is free.
     * Go through the zones in the order of their numbers (no priority queue). Returns two numbers:
     * [0] flow stages: the number of zones on the longest chain of zones;
     * [1] the cost of the cheapest route from the zone of cell 0 to the zone of the last cell.
     */
    public static long[] routes(Input.Slides s, int[] zoneOf, int zoneCount) {
        if (zoneCount == 0) return new long[]{0, 0};

        // One map per non-sink zone: retain only the cheapest parallel slide.
        List<Map<Integer, Integer>> graph = new ArrayList<>(zoneCount);
        for (int z = 0; z < zoneCount; z++) graph.add(null);
        for (int edge = 0; edge < s.count(); edge++) {
            int from = zoneOf[s.from()[edge]], to = zoneOf[s.to()[edge]];
            if (from == to) continue;
            Map<Integer, Integer> neighbours = graph.get(from);
            if (neighbours == null) {
                neighbours = new HashMap<>();
                graph.set(from, neighbours);
            }
            neighbours.merge(to, s.time()[edge], Math::min);
        }

        int[] stages = new int[zoneCount];
        Arrays.fill(stages, 1);
        long[] distance = new long[zoneCount];
        Arrays.fill(distance, Long.MAX_VALUE);
        distance[zoneOf[0]] = 0;
        long longest = 1;
        for (int zone = 0; zone < zoneCount; zone++) {
            longest = Math.max(longest, stages[zone]);
            Map<Integer, Integer> neighbours = graph.get(zone);
            if (neighbours == null) continue;
            for (Map.Entry<Integer, Integer> edge : neighbours.entrySet()) {
                int to = edge.getKey();
                stages[to] = Math.max(stages[to], stages[zone] + 1);
                if (distance[zone] != Long.MAX_VALUE) {
                    long candidate = distance[zone] + edge.getValue();
                    distance[to] = Math.min(distance[to], candidate);
                }
            }
        }
        long fastest = distance[zoneOf[s.n() - 1]];
        return new long[]{longest, fastest == Long.MAX_VALUE ? -1 : fastest};
    }
}
