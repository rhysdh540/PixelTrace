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
        global_x_min = x_min;
        global_y_min = y_min;
        pixels = pixels_input;

        if(!canHaveChildren) return;

        final int width = pixels.width;
        final int height = pixels.height;
        //Cache these values to enjoy a slight performance uplift and less verbose syntax.

        if(width<=2 || height<=2) return;

        // 8-neighbor union of background to find outside-connected background components
        UnionFind uf8 = new UnionFind(width, height);
        for(int y=0; y<height; y++){
            for(int x=0; x<width; x++){
                if(pixels.getBit(x, y)) continue; //Only background participates here
                // Left neighbor
                if(x > 0 && !pixels.getBit(x-1, y)) uf8.union(x-1, y, x, y);
                if(y > 0){
                    // Up
                    if(!pixels.getBit(x, y-1)) uf8.union(x, y-1, x, y);
                    // Up-left
                    if(x > 0 && !pixels.getBit(x-1, y-1)) uf8.union(x-1, y-1, x, y);
                    // Up-right
                    if(x < width-1 && !pixels.getBit(x+1, y-1)) uf8.union(x+1, y-1, x, y);
                }
            }
        }

        // Mark all outside-connected background roots by scanning the border
        final int size = width * height;
        BitSet outsideRoots = new BitSet(size);
        // Top and bottom rows
        for(int x=0; x<width; x++){
            if(!pixels.getBit(x, 0)) outsideRoots.set(uf8.find(x, 0));
            int by = height-1;
            if(!pixels.getBit(x, by)) outsideRoots.set(uf8.find(x, by));
        }
        // Left and right columns (skip corners already visited)
        for(int y=1; y<height-1; y++){
            if(!pixels.getBit(0, y)) outsideRoots.set(uf8.find(0, y));
            int rx = width-1;
            if(!pixels.getBit(rx, y)) outsideRoots.set(uf8.find(rx, y));
        }

        // the remaining background pixels not connected to outside are hole candidates
        boolean[] holeCandidate = new boolean[size];
        int candidateCount = 0;
        for(int y=0; y<height; y++){
            int base = y * width;
            for(int x=0; x<width; x++){
                if(pixels.getBit(x, y)) continue;
                int r8 = uf8.find(x, y);
                if(!outsideRoots.get(r8)){
                    holeCandidate[base + x] = true;
                    candidateCount++;
                }
            }
        }
        if(candidateCount == 0){
            children = new Island[0];
            return;
        }

        // group hole candidates with 4-neighborhood union (left/up only)
        UnionFind uf4 = new UnionFind(width, height);
        for(int y=0; y<height; y++){
            int base = y * width;
            for(int x=0; x<width; x++){
                if(!holeCandidate[base + x]) continue;
                if(x > 0 && holeCandidate[base + (x-1)]) uf4.union(x-1, y, x, y);
                if(y > 0 && holeCandidate[base - width + x]) uf4.union(x, y-1, x, y);
            }
        }

        // Map roots to hole indices
        int[] rootToIndex = new int[size];
        Arrays.fill(rootToIndex, -1);
        int holeCount = 0;
        for(int y=0; y<height; y++){
            int base = y * width;
            for(int x=0; x<width; x++){
                if(!holeCandidate[base + x]) continue;
                int r4 = uf4.find(x, y);
                if(rootToIndex[r4] == -1){
                    rootToIndex[r4] = holeCount++;
                }
            }
        }

        if(holeCount == 0){
            children = new Island[0];
            return;
        }

		// Determine bounding box for each hole
        int[] child_x_min = new int[holeCount];
        Arrays.fill(child_x_min, width);
        int[] child_x_max = new int[holeCount];
        Arrays.fill(child_x_max, -1);
        int[] child_y_min = new int[holeCount];
        Arrays.fill(child_y_min, height);
        int[] child_y_max = new int[holeCount];
        Arrays.fill(child_y_max, -1);
        for(int y=0; y<height; y++){
            int base = y * width;
            for(int x=0; x<width; x++){
                if(!holeCandidate[base + x]) continue;
                int idx = rootToIndex[uf4.find(x, y)];
                if(x < child_x_min[idx]) child_x_min[idx] = x;
                if(x > child_x_max[idx]) child_x_max[idx] = x;
                if(y < child_y_min[idx]) child_y_min[idx] = y;
                if(y > child_y_max[idx]) child_y_max[idx] = y;
            }
        }

        // build BitGrid for each hole
        children = new Island[holeCount];
        BitGrid[] childBits = new BitGrid[holeCount];
        for(int i=0; i<holeCount; i++){
            int child_width = (child_x_max[i] - child_x_min[i]) + 1;
            int child_height = (child_y_max[i] - child_y_min[i]) + 1;
            childBits[i] = new BitGrid(child_width, child_height);
        }

		// populate hole BitGrids
        for(int y=0; y<height; y++){
            int base = y * width;
            for(int x=0; x<width; x++){
                if(!holeCandidate[base + x]) continue;
                int idx = rootToIndex[uf4.find(x, y)];
                childBits[idx].setBit(x - child_x_min[idx], y - child_y_min[idx], true);
            }
        }

        for(int i=0; i<holeCount; i++){
            children[i] = new Island(child_x_min[i] + global_x_min, child_y_min[i] + global_y_min, childBits[i], false);
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