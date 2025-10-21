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

        // collect runs of background pixels (false bits)
        ConnectedComponentLabeling.RunSet set = ConnectedComponentLabeling.collectRuns(pixels, 0, 0, width, height, false);
        final int R = set.size();
        if(R == 0){ // No background = no holes
            children = new Island[0];
            return;
        }

        // side+corner union to identify "outside" background (expand by 1 both sides)
        UnionFind ufOutside = ConnectedComponentLabeling.unionRows(set, 1, 1, null);

        int R1 = set.size();
        boolean[] outsideRoot = new boolean[R1];
        for(int r = 0; r<R1; r++){
            ConnectedComponentLabeling.Run run = set.runs().get(r);
            if(run.y() == 0 || run.y() == set.height() - 1 || run.x0() == 0 || run.x1() == set.width() - 1){
                outsideRoot[ufOutside.find(r)] = true;
            }
        }
        boolean[] isOutside = new boolean[R1];
        for(int r = 0; r<R1; r++){
            isOutside[r] = outsideRoot[ufOutside.find(r)];
        }

        // if all background is outside, there are no holes
        boolean anyInside = false;
        for(int i=0; i<R; i++){
            if(!isOutside[i]){
                anyInside = true;
                break;
            }
        }
        if(!anyInside){
            children = new Island[0];
            return;
        }

        // side-only union of the remaining (non-outside) background runs
        UnionFind ufInside = ConnectedComponentLabeling.unionRows(set, 0, 0, isOutside);

        // keep only inside components
        boolean[] inside = new boolean[R];
        for(int i=0; i<R; i++){
            inside[i] = !isOutside[i];
        }
        BitSet keepRoots = ConnectedComponentLabeling.rootsWithFlags(ufInside, inside);
        int holeCount = keepRoots.cardinality();
        if(holeCount == 0){
            children = new Island[0];
            return;
        }

        int[] rootToHole = ConnectedComponentLabeling.mapRoots(ufInside, R, keepRoots);

        // determine bounding boxes for each hole
        int[] minX = new int[holeCount], maxX = new int[holeCount];
        int[] minY = new int[holeCount], maxY = new int[holeCount];
        ConnectedComponentLabeling.computeBoundingBoxes(set, ufInside, rootToHole, minX, maxX, minY, maxY);

        BitGrid[] grids = ConnectedComponentLabeling.buildComponentGrids(set, ufInside, rootToHole, minX, maxX, minY, maxY);

        children = new Island[holeCount];

        for(int h=0; h<holeCount; h++){
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