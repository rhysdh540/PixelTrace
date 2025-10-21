import java.util.*;

public final class ConnectedComponentLabeling{
    private ConnectedComponentLabeling(){}

    public record Run(int y, int x0, int x1){}

    public record RunSet(List<Run> runs, int[] rowStarts, int[] rowEnds, int width, int height){
        public int size(){
            return runs.size();
        }
    }

    // Collect horizontal runs of targetValue within a sub-rectangle of grid starting at (xOff,yOff) of size w x h
    public static RunSet collectRuns(BitGrid grid, int xOff, int yOff, int w, int h, boolean targetValue){
        final ArrayList<Run> runs = new ArrayList<>();
        final int[] rowStarts = new int[h];
        final int[] rowEnds = new int[h];
        for(int y=0; y<h; y++){
            rowStarts[y] = runs.size();
            int x = 0;
            while(x < w){
                // find start of a run
                while(x < w && (grid.getBit(xOff + x, yOff + y) != targetValue)) x++;
                if(x >= w) break;
                int x0 = x;
                do {
                    x++;
                } while(x < w && (grid.getBit(xOff + x, yOff + y) == targetValue));
                int x1 = x - 1;
                runs.add(new Run(y, x0, x1));
            }
            rowEnds[y] = runs.size();
        }
        return new RunSet(runs, rowStarts, rowEnds, w, h);
    }

    // Union runs between adjacent rows when their x-intervals overlap,
    // with optional expansion on the previous row's run
    // If skip != null, indices with skip[idx] == true will be ignored (not unioned + and advanced over)
    public static UnionFind unionRows(RunSet set, int expandLeft, int expandRight, boolean[] skip){
        int total = set.size();
        UnionFind uf = new UnionFind(total);
        final List<Run> runs = set.runs;
        for(int y=1; y<set.height; y++){
            int prevStart = set.rowStarts[y - 1], prevEnd = set.rowEnds[y - 1];
            int currStart = set.rowStarts[y], currEnd = set.rowEnds[y];

            while(prevStart < prevEnd && currStart < currEnd){
                if(skip != null && skip[prevStart]){
                    prevStart++;
                    continue;
                }
                if(skip != null && skip[currStart]){
                    currStart++;
                    continue;
                }
                Run pr = runs.get(prevStart);
                Run cr = runs.get(currStart);
                int prL = pr.x0 - expandLeft;
                int prR = pr.x1 + expandRight;
                if(Math.max(prL, cr.x0) <= Math.min(prR, cr.x1)){
                    uf.union(prevStart, currStart);
                    if(pr.x1 < cr.x1){
                        prevStart++;
                    } else {
                        currStart++;
                    }
                } else {
                    if(pr.x1 < cr.x0){
                        prevStart++;
                    } else {
                        currStart++;
                    }
                }
            }
        }
        return uf;
    }

    public static BitSet rootsWithFlags(UnionFind uf, boolean[] flags){
        BitSet keep = new BitSet();
        for(int i=0; i<flags.length; i++){
            if(flags[i]) keep.set(uf.find(i));
        }
        return keep;
    }

    // Map each root to a compact index only for kept roots
    public static int[] mapRoots(UnionFind uf, int totalRuns, BitSet keepRoots){
        int[] rootToIdx = new int[totalRuns];
        Arrays.fill(rootToIdx, -1);
        int next = 0;
        for(int r=0; r<totalRuns; r++){
            int rr = uf.find(r);
            if(!keepRoots.get(rr)) continue;
            if(rootToIdx[rr] == -1) rootToIdx[rr] = next++;
        }
        return rootToIdx;
    }

    public static void computeBoundingBoxes(RunSet set, UnionFind uf, int[] rootToIdx,
                                            int[] minX, int[] maxX, int[] minY, int[] maxY){
        Arrays.fill(minX, set.width);
        Arrays.fill(minY, set.height);
        Arrays.fill(maxX, -1);
        Arrays.fill(maxY, -1);
        final List<Run> runs = set.runs;
        for(int r=0; r<set.size(); r++){
            int idx = rootToIdx[uf.find(r)];
            if(idx == -1) continue;
            Run run = runs.get(r);
            if(run.x0 < minX[idx]) minX[idx] = run.x0;
            if(run.x1 > maxX[idx]) maxX[idx] = run.x1;
            if(run.y < minY[idx]) minY[idx] = run.y;
            if(run.y > maxY[idx]) maxY[idx] = run.y;
        }
    }

    // Build and populate BitGrids for each kept component using bounding boxes and runs
    public static BitGrid[] buildComponentGrids(RunSet set, UnionFind uf, int[] rootToIdx,
                                                int[] minX, int[] maxX, int[] minY, int[] maxY){
        int kept = minX.length;
        BitGrid[] grids = new BitGrid[kept];
        for(int i=0; i<kept; i++){
            int w = (maxX[i] - minX[i]) + 1;
            int h = (maxY[i] - minY[i]) + 1;
            grids[i] = new BitGrid(w, h);
        }

        final List<Run> runs = set.runs;
        for(int r=0; r<set.size(); r++){
            int idx = rootToIdx[uf.find(r)];
            if(idx == -1) continue;
            Run run = runs.get(r);
            int localY = run.y - minY[idx];
            int startX = run.x0 - minX[idx];
            int len = run.x1 - run.x0 + 1;
            grids[idx].setSpan(startX, localY, len, true);
        }
        return grids;
    }

}
