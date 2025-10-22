import java.util.*;

public class RunCache {
    private final BitGrid grid;
    private final int globalWidth;
    private final int globalHeight;

    private final List<ConnectedComponentLabeling.Run>[] rowRuns;

    private final BitSet dirtyRows;

    @SuppressWarnings("unchecked")
    public RunCache(BitGrid grid) {
        this.grid = grid;
        this.globalWidth = grid.width;
        this.globalHeight = grid.height;
        this.rowRuns = (List<ConnectedComponentLabeling.Run>[]) new ArrayList[globalHeight];
        this.dirtyRows = new BitSet(globalHeight);

        Arrays.setAll(rowRuns, _ -> new ArrayList<>());
    }

    public void markRowDirty(int globalY) {
        if (globalY >= 0 && globalY < globalHeight) {
            dirtyRows.set(globalY);
        }
    }

    public void updateDirtyRows() {
        for (int y = dirtyRows.nextSetBit(0); y >= 0; y = dirtyRows.nextSetBit(y + 1)) {
            updateRow(y);
        }
        dirtyRows.clear();
    }

    private void updateRow(int y) {
        List<ConnectedComponentLabeling.Run> runs = rowRuns[y];
        runs.clear();

        int x = 0;
        while (x < globalWidth) {
            // Find start of a run
            while (x < globalWidth && !grid.getBit(x, y)) x++;
            if (x >= globalWidth) break;

            int x0 = x;
            do {
                x++;
            } while (x < globalWidth && grid.getBit(x, y));
            int x1 = x - 1;

            runs.add(new ConnectedComponentLabeling.Run(y, x0, x1));
        }
    }

    public ConnectedComponentLabeling.RunSet extractRuns(int xOff, int yOff, int w, int h) {
        // First, ensure all dirty rows are updated
        updateDirtyRows();

        ArrayList<ConnectedComponentLabeling.Run> runs = new ArrayList<>();
        int[] rowStarts = new int[h];
        int[] rowEnds = new int[h];

        for (int localY = 0; localY < h; localY++) {
            int globalY = yOff + localY;
            rowStarts[localY] = runs.size();

            if (globalY >= 0 && globalY < globalHeight) {
                List<ConnectedComponentLabeling.Run> globalRuns = rowRuns[globalY];

                // Extract runs that overlap with [xOff, xOff + w)
                for (ConnectedComponentLabeling.Run run : globalRuns) {
                    int runX0 = run.x0();
                    int runX1 = run.x1();

                    // Check if run overlaps with our x-range
                    if (runX1 >= xOff && runX0 < xOff + w) {
                        // Clip to our bounds and convert to local coordinates
                        int localX0 = Math.max(0, runX0 - xOff);
                        int localX1 = Math.min(w - 1, runX1 - xOff);

                        if (localX0 <= localX1) {
                            runs.add(new ConnectedComponentLabeling.Run(localY, localX0, localX1));
                        }
                    }
                }
            }

            rowEnds[localY] = runs.size();
        }

        return new ConnectedComponentLabeling.RunSet(runs, rowStarts, rowEnds, w, h);
    }
}

