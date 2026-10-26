import java.util.Arrays;

/**
 * Mission 3. Escape: DFS from cell 0 to the last cell (rows * cols - 1) in the maze of Mission 1.
 * Two neighbouring cells are connected when the wall between them is broken.
 * The neighbours are tried in this order: up, right, down, left (cell - cols, cell + 1, cell + cols, cell - 1).
 * The ready method Graphs.open(w, broken) gives you the open neighbours of every cell, in no particular order:
 * to visit them in the required order, go through the four cells above one by one and take those that are in the list.
 * The program must not overflow the stack on a maze of 1000 by 1000 cells.
 */
public class Escape {

    /**
     * path: the cells of the way out, from 0 to the last cell.
     * visited: how many cells are marked as visited at the moment the exit is marked (the start and the exit are counted).
     * deadEnds: how many cells of the maze have exactly one open neighbour.
     */
    public record Result(int[] path, int visited, int deadEnds) {}

    public static Result escape(Input.Walls w, int[] broken) {
        int n = w.cells();
        if (n == 0) return new Result(new int[0], 0, 0);
        int[][] open = Graphs.open(w, broken);
        int deadEnds = 0;
        for (int[] neighbours : open) {
            if (neighbours.length == 1) deadEnds++;
        }

        boolean[] seen = new boolean[n];
        int[] stack = new int[n];
        int[] nextDirection = new int[n];
        int depth = 1, visited = 1;
        stack[0] = 0;
        seen[0] = true;
        while (depth > 0) {
            int cell = stack[depth - 1];
            if (cell == n - 1) {
                // The stack is exactly the path currently explored by DFS.
                return new Result(Arrays.copyOf(stack, depth), visited, deadEnds);
            }
            if (nextDirection[cell] == 4) {
                depth--;
                continue;
            }
            int neighbour = switch (nextDirection[cell]++) {
                case 0 -> cell - w.cols();
                case 1 -> cell % w.cols() + 1 < w.cols() ? cell + 1 : -1;
                case 2 -> cell + w.cols();
                default -> cell % w.cols() > 0 ? cell - 1 : -1;
            };
            if (neighbour < 0 || neighbour >= n || seen[neighbour]) continue;
            for (int candidate : open[cell]) {
                if (candidate == neighbour) {
                    seen[neighbour] = true;
                    visited++;
                    stack[depth++] = neighbour;
                    break;
                }
            }
        }
        return new Result(new int[0], visited, deadEnds);
    }
}
