import java.io.IOException;
import java.util.*;

public class Island {
    static private final int RIGHT = 0;
    static private final int DOWN = 1;
    static private final int LEFT = 2;
    static private final int UP = 3;

    //For an explanation of the values in this table, see "Archive/Original Corner Table.png"
    static private final List<Map<Integer, Integer>> cornerTable = List.of(
        //Index 0 = Right
        Map.of(2,DOWN,8,UP,13,DOWN,9,UP,6,DOWN,7,UP),
        //Index 1 = Down
        Map.of(8,LEFT,4,RIGHT,9,LEFT,6,RIGHT,7,LEFT,11,RIGHT),
        //Index 2 = Left
        Map.of(1,DOWN,4,UP,14,DOWN,9,DOWN,6,UP,11,UP),
        //Index 3 = Up
        Map.of(1,RIGHT,2,LEFT,14,RIGHT,13,LEFT,9,RIGHT,6,LEFT)
    );

    private final int global_x_min;
    private final int global_y_min;
    private final BitGrid pixels;
    private Island[] children = new Island[0];

    public Island(int x_min, int y_min, BitGrid pixels_input, boolean canHaveChildren){
        record Run(int y, int x0, int x1) {}

        global_x_min = x_min;
        global_y_min = y_min;
        pixels = pixels_input;

        if(!canHaveChildren) return;

        final int width = pixels.width;
        final int height = pixels.height;
        //Cache these values to enjoy a slight performance uplift and less verbose syntax.

        if(width<=2 || height<=2) return;

        // collect runs of background pixels (false bits)
        final List<Run> runs = new ArrayList<>();
        final int[] rowStarts = new int[height];
        final int[] rowEnds = new int[height];

        for(int y=0; y<height; y++){
            rowStarts[y] = runs.size();
            int x = 0;
            while(x < width){
                // skip solids
                while(x < width && pixels.getBit(x, y)) x++;
                if(x >= width) break;

                // found start of a background run
                int x0 = x;
                do {
                    x++;
                } while(x < width && !pixels.getBit(x, y));
                int x1 = x - 1;

                Run r = new Run(y, x0, x1);
                runs.add(r);
            }
            rowEnds[y] = runs.size();
        }

        final int R = runs.size();
        if(R == 0){ // No background = no holes
            children = new Island[0];
            return;
        }

        // side+corner union to identify "outside" background
        // runs overlap if their x ranges intersect when expanded by 1 pixel on each side
        UnionFind uf = new UnionFind(R);

        for(int y=1; y<height; y++){
            int prevStart = rowStarts[y-1], prevEnd = rowEnds[y-1];
            int currStart = rowStarts[y], currEnd = rowEnds[y];

            while (prevStart < prevEnd && currStart < currEnd) {
                Run prev = runs.get(prevStart);
                Run curr = runs.get(currStart);

                if(Math.max(prev.x0 - 1, curr.x0) <= Math.min(prev.x1 + 1, curr.x1)){
                    uf.union(prevStart, currStart);
                    // advance the one that ends first
                    if (prev.x1 < curr.x1) {
                        prevStart++;
                    } else {
                        currStart++;
                    }
                } else {
                    if (prev.x1 < curr.x0) {
                        prevStart++;
                    } else {
                        currStart++;
                    }
                }
            }
        }

        // mark roots that touch the border as outside
        boolean[] outsideRoot = new boolean[R];
        for(int r = 0; r < R; r++){
            Run run = runs.get(r);
            if(run.y == 0 || run.y == height - 1 || run.x0 == 0 || run.x1 == width - 1){
                outsideRoot[uf.find(r)] = true;
            }
        }

        // propagate outside flags to all runs via roots
        boolean[] isOutside = new boolean[R];
        for(int r = 0; r < R; r++){
            isOutside[r] = outsideRoot[uf.find(r)];
        }

        // if all background is outside, there are no holes
        int anyInside = 0;
        for(int r = 0; r < R; r++){
            if(!isOutside[r]){
                anyInside = 1;
                break;
            }
        }

        if(anyInside == 0){
            children = new Island[0];
            return;
        }

        // side union of the remaining (non-outside) background runs
        uf = new UnionFind(R);

        for(int y = 1; y < height; y++){
            int prevStart = rowStarts[y - 1], prevEnd = rowEnds[y - 1];
            int currStart = rowStarts[y], currEnd = rowEnds[y];

            while(prevStart < prevEnd && currStart < currEnd){
                Run prev = runs.get(prevStart);
                Run curr = runs.get(currStart);

                // skip runs that are outside (cannot form holes)
                if(isOutside[prevStart]){
                    prevStart++;
                    continue;
                }
                if(isOutside[currStart]){
                    currStart++;
                    continue;
                }

                if(Math.max(prev.x0, curr.x0) <= Math.min(prev.x1, curr.x1)){
                    uf.union(prevStart, currStart);
                    // advance the one that ends first
                    if(prev.x1 < curr.x1){
                        prevStart++;
                    } else {
                        currStart++;
                    }
                } else {
                    if(prev.x1 < curr.x0){
                        prevStart++;
                    } else {
                        currStart++;
                    }
                }
            }
        }

        // assign hole ids to each root
        int[] rootToHole = new int[R];
        Arrays.fill(rootToHole, -1);
        int holeCount = 0;

        for(int y = 0; y < height; y++){
            for(int idx = rowStarts[y]; idx < rowEnds[y]; idx++){
                if(isOutside[idx]) continue;
                int root = uf.find(idx);
                if(rootToHole[root] == -1){
                    rootToHole[root] = holeCount++;
                }
            }
        }

        if(holeCount == 0){
            children = new Island[0];
            return;
        }

        // determine bounding boxes for each hole
        int[] minX = new int[holeCount], maxX = new int[holeCount];
        int[] minY = new int[holeCount], maxY = new int[holeCount];
        Arrays.fill(minX, width);
        Arrays.fill(maxX, -1);
        Arrays.fill(minY, height);
        Arrays.fill(maxY, -1);

        for(int r = 0; r < R; r++){
            if(isOutside[r]) continue;
            int h = rootToHole[uf.find(r)];
            if(h < 0) continue;
            Run run = runs.get(r);
            if(run.x0 < minX[h]) minX[h] = run.x0;
            if(run.x1 > maxX[h]) maxX[h] = run.x1;
            if(run.y < minY[h]) minY[h] = run.y;
            if(run.y > maxY[h]) maxY[h] = run.y;
        }

        // create BitGrids for each hole and populate them
        children = new Island[holeCount];
        BitGrid[] grids = new BitGrid[holeCount];

        for(int h = 0; h < holeCount; h++){
            int w = (maxX[h] - minX[h]) + 1;
            int hgt = (maxY[h] - minY[h]) + 1;
            grids[h] = new BitGrid(w, hgt);
        }

        for(int r = 0; r < R; r++){
            if(isOutside[r]) continue;
            int hId = rootToHole[uf.find(r)];
            if(hId < 0) continue;
            Run run = runs.get(r);
            int yLocal = run.y - minY[hId];
            int xLocal = run.x0 - minX[hId];
            int len = run.x1 - run.x0 + 1;
            grids[hId].setSpan(xLocal, yLocal, len, true);
        }

        for(int h = 0; h < holeCount; h++){
            children[h] = new Island(
                    minX[h] + global_x_min,
                    minY[h] + global_y_min,
                    grids[h],
                    false
            );
        }
    }

    private boolean safeLookup(int x, int y){
        return (x>=0) && (x<pixels.width) && (y>=0) && (y<pixels.height) && pixels.getBit(x, y);
    }

    private int fourSquareVal(int x, int y){
        int topLeft = safeLookup(x-1, y-1) ? 8 : 0;
        int top = safeLookup(x, y-1) ? 4 : 0;
        int left = safeLookup(x-1, y) ? 2 : 0;
        int center = safeLookup(x, y) ? 1 : 0;
        return topLeft | top | left | center;
    }

    private long findUpperLeftCorner(){
        for(int y=0; y<pixels.height; y++){
            for(int x=0; x<pixels.width; x++){
                if(fourSquareVal(x, y) == 1){
                    return (((long) x) << 32) | Integer.toUnsignedLong(y);
                }
            }
        }
        throw new AssertionError("Every island has at least one upper-left corner. The only way for this exception to trip is some sort of memory corruption or other catastrophic error has occurred.");
    }

    public void traceSVG(ObscurePrint out) throws IOException{
        long start = findUpperLeftCorner();
        int start_x = (int) (start >>> 32);
        int start_y = (int) start;
        int prev_x = start_x;
        int prev_y = start_y;
        int cur_x = start_x+1;
        int cur_y = start_y;
        int direction = RIGHT;
        out.print("M " + (global_x_min + start_x) + " " + (global_y_min + start_y));
        while((cur_x != start_x) || (cur_y != start_y)){
            int turn = cornerTable.get(direction).getOrDefault(fourSquareVal(cur_x, cur_y), -1);
            if(turn >= 0){
                if((direction == RIGHT) || (direction == LEFT)){ //Horizontal Line
                    out.print(" h ");
                    out.print(cur_x - prev_x);
                    prev_x = cur_x;
                } else { //Vertical Line
                    out.print(" v ");
                    out.print(cur_y - prev_y);
                    prev_y = cur_y;
                }
                direction = turn;
            }
            switch(direction) {
                case RIGHT -> cur_x++;
                case DOWN -> cur_y++;
                case LEFT -> cur_x--;
                case UP -> cur_y--;
                default -> throw new IllegalStateException("Unexpected value: " + direction);
            }
        }
        out.print(" z");
        for(Island child : children){
            out.print(" ");
            child.traceSVG(out);
        }
    }

    public void traceTikZ(ObscurePrint out, final int globalHeight) throws IOException {
        long start = findUpperLeftCorner();
        int start_x = (int) (start >>> 32);
        int start_y = (int) start;
        int cur_x = start_x+1;
        int cur_y = start_y;
        int direction = RIGHT;
        out.print(" (");
        out.print(global_x_min + start_x);
        out.print(",");
        out.print(globalHeight - (global_y_min + start_y));
        out.print(")");
        while((cur_x != start_x) || (cur_y != start_y)){
            int turn = cornerTable.get(direction).getOrDefault(fourSquareVal(cur_x, cur_y), -1);
            if(turn >= 0){
                if((direction == DOWN) || (direction == UP)){
                    out.print(" -| (");
                    out.print(global_x_min + cur_x);
                    out.print(",");
                    out.print(globalHeight - (global_y_min + cur_y));
                    out.print(")");
                }
                direction = turn;
            }
            switch(direction) {
                case RIGHT -> cur_x++;
                case DOWN -> cur_y++;
                case LEFT -> cur_x--;
                case UP -> cur_y--;
                default -> throw new IllegalStateException("Unexpected value: " + direction);
            }
        }
        out.print(" -| cycle");
        for(Island child : children) child.traceTikZ(out, globalHeight);
    }
}