import java.util.*;

public class RunCache {
    private final BitGrid grid;
    private final int globalWidth;
    private final int globalHeight;

    private final List<ConnectedComponentLabeling.Run>[] rowRuns;

    private final BitSet dirtyRows;
    private final BitSet computedRows;

    @SuppressWarnings("unchecked")
    public RunCache(BitGrid grid) {
        this.grid = grid;
        this.globalWidth = grid.width;
        this.globalHeight = grid.height;
        this.rowRuns = (List<ConnectedComponentLabeling.Run>[]) new ArrayList[globalHeight];
        this.dirtyRows = new BitSet(globalHeight);
        this.computedRows = new BitSet(globalHeight);

        Arrays.setAll(rowRuns, _ -> new ArrayList<>());
    }

    public void markRowDirty(int globalY) {
        if (globalY >= 0 && globalY < globalHeight) {
            dirtyRows.set(globalY);
        }
    }

    private void updateRow(int y) {
        List<ConnectedComponentLabeling.Run> runs = rowRuns[y];
        runs.clear();

        grid.scanRowRuns(y, (x0, x1) -> runs.add(new ConnectedComponentLabeling.Run(y, x0, x1)));
    }

    public ConnectedComponentLabeling.RunSet extractRuns(int xOff, int yOff, int w, int h) {
        ArrayList<ConnectedComponentLabeling.Run> runs = new ArrayList<>();
        int[] rowStarts = new int[h];
        int[] rowEnds = new int[h];

        for (int localY = 0; localY < h; localY++) {
            int globalY = yOff + localY;
            rowStarts[localY] = runs.size();

            if (globalY >= 0 && globalY < globalHeight) {
                if (dirtyRows.get(globalY) || !computedRows.get(globalY)) {
                    updateRow(globalY);
                    computedRows.set(globalY);
                    dirtyRows.clear(globalY);
                }
                List<ConnectedComponentLabeling.Run> globalRuns = rowRuns[globalY];

                // Extract runs that overlap with [xOff, xOff + w)
                for(int i = 0; i < globalRuns.size(); i++){
                    ConnectedComponentLabeling.Run run = globalRuns.get(i);
                    int runX0 = run.x0();
                    int runX1 = run.x1();

                    // Check if run overlaps with our x-range
                    if(runX1 >= xOff && runX0 < xOff + w){
                        // Clip to our bounds and convert to local coordinates
                        int localX0 = Math.max(0, runX0 - xOff);
                        int localX1 = Math.min(w - 1, runX1 - xOff);

                        if(localX0 <= localX1){
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
