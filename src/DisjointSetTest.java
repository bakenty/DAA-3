/**
 * Checks of Mission 0. Run:  java -cp out DisjointSetTest
 * Add your own checks below: a large chain, a union of a set with itself, count after many unions.
 */
public class DisjointSetTest {

    static int failed = 0;

    static void check(boolean ok, String what) {
        System.out.println((ok ? "ok    " : "FAIL  ") + what);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        DisjointSet d = new DisjointSet(6);
        check(d.count() == 6, "6 sets at the start");
        check(!d.connected(0, 1), "0 and 1 are apart at the start");
        check(d.union(0, 1), "union(0, 1) joins two sets");
        check(d.connected(0, 1), "0 and 1 are together");
        check(!d.union(1, 0), "union(1, 0) again returns false");
        check(d.count() == 5, "5 sets after one union");
        d.union(2, 3); d.union(1, 3);
        check(d.connected(0, 2), "0 and 2 are together through 1 and 3");
        check(d.count() == 3, "3 sets: {0,1,2,3} {4} {5}");
        check(d.find(4) == 4 && d.find(5) == 5, "a single cell is its own root");

        check(!d.union(0, 0), "union with itself returns false");
        check(d.count() == 3, "self-union does not change count");
        d.union(4, 5);
        int largeRoot = d.find(0);
        check(d.union(4, 0), "join the smaller set to the larger set");
        check(d.find(4) == largeRoot, "union by size keeps the larger root");
        check(d.count() == 1, "one set after joining all six cells");

        DisjointSet empty = new DisjointSet(0);
        check(empty.count() == 0, "an empty DSU has zero sets");
        DisjointSet single = new DisjointSet(1);
        check(single.connected(0, 0) && !single.union(0, 0), "a single cell stays in its own set");
        check(single.count() == 1, "singleton count stays one");

        int n = 1_000_000;
        DisjointSet chain = new DisjointSet(n);
        boolean joined = true;
        for (int i = 1; i < n; i++) {
            if (!chain.union(i - 1, i)) joined = false;
        }
        check(joined, "every union along a million-cell chain joins two sets");
        check(chain.count() == 1, "a million-cell chain has one set");
        boolean connected = true;
        int root = chain.find(0);
        for (int i = 0; i < n; i++) {
            if (chain.find(i) != root) connected = false;
        }
        check(connected, "all million cells have the same root without recursion");
        check(!chain.union(n - 1, 0) && chain.count() == 1, "closing the chain changes nothing");

        System.out.println(failed == 0 ? "All checks passed." : failed + " checks failed.");
        if (failed != 0) throw new AssertionError(failed + " DSU checks failed");
    }
}
