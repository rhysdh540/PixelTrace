import java.util.TreeSet;
import java.util.ArrayList;

public class ColorLayer implements Comparable<ColorLayer>{
    private final int color;
    private final int x_min;
    private final int x_max;
    private final int y_min;
    private final int y_max;
    private final long bounding_area;
    private final long pixel_count;
    private final BitGrid mask;
    private Island[] children;

    public ColorLayer(int new_color, BitGrid detections){
        color = new_color;
        int temp_x_min = Integer.MAX_VALUE;
        int temp_x_max = Integer.MIN_VALUE;
        int temp_y_min = Integer.MAX_VALUE;
        int temp_y_max = Integer.MIN_VALUE;
        long temp_count = 0;
        for(int y=0; y<detections.height; y++){
            for(int x=0; x<detections.width; x++){
                if(detections.getBit(x, y)){
                    temp_x_min = Math.min(temp_x_min, x);
                    temp_x_max = Math.max(temp_x_max, x);
                    temp_y_min = Math.min(temp_y_min, y);
                    temp_y_max = Math.max(temp_y_max, y);
                    temp_count++;
                }
            }
        }
        x_min = temp_x_min;
        x_max = temp_x_max;
        y_min = temp_y_min;
        y_max = temp_y_max;
        pixel_count = temp_count;
        long width = (x_max - x_min) + 1;
        long height = (y_max - y_min) + 1;
        bounding_area = width * height;
        mask = new BitGrid((int) width, (int) height);
        for(int y=y_min; y<=y_max; y++){
            for(int x=x_min; x<=x_max; x++){
                if(detections.getBit(x, y)){
                    mask.setBit(x-x_min, y-y_min, true);
                }
            }
        }
        children = new Island[0];
    }

    public String debugInfo(){
        StringBuilder sb = new StringBuilder("====ColorLayer ");
        sb.append(Main.leftPad(Integer.toHexString(color).toUpperCase(), '0', 8));
        sb.append("====");
        sb.append(System.lineSeparator());
        sb.append("X: ");
        sb.append(x_min);
        sb.append(" -> ");
        sb.append(x_max);
        sb.append(System.lineSeparator());
        sb.append("Y: ");
        sb.append(y_min);
        sb.append(" -> ");
        sb.append(y_max);
        sb.append(System.lineSeparator());
        sb.append("Area: ");
        sb.append(bounding_area);
        sb.append(" / ");
        sb.append("Count: ");
        sb.append(pixel_count);
        sb.append(System.lineSeparator());
        sb.append(children.length);
        if(children.length == 1){
            sb.append(" stored child.");
        } else {
            sb.append(" stored children.");
        }
        return sb.toString();
    }

    @Override
    public int compareTo(ColorLayer other) {
        int area_compare = Long.compare(other.bounding_area, bounding_area);
        if(area_compare == 0){
            int count_compare = Long.compare(other.pixel_count, pixel_count);
            if(count_compare == 0){
                return Integer.compareUnsigned(color, other.color);
            }
            return count_compare;
        }
        return area_compare;
    }

    private int[] determineValidIslands(int[][] grid){
        //"Accessible" islands either touch the outer border of this ColorLayer,
        //or else come into contact with some "-1" somewhere on the island's edge.
        //Islands that don't match either of those criteria must be entirely surrounded by "-2",
        //and are thus supposed to be saved for a later recursive pass.
        TreeSet<Integer> accessibleIslands = new TreeSet<>();
        for(int x=0; x<mask.width; x++){
            int check = grid[0][x];
            if(check >= 0) accessibleIslands.add(check);
        }
        for(int y=1; y<mask.height; y++){
            int check = grid[y][0];
            if(check >= 0) accessibleIslands.add(check);
            check = grid[y][mask.width-1];
            if(check >= 0) accessibleIslands.add(check);
        }
        for(int x=1; x<mask.width-1; x++){
            int check = grid[mask.height-1][x];
            if(check >= 0) accessibleIslands.add(check);
        }
        for(int y=1; y<mask.height-1; y++){
            for(int x=1; x<mask.width-1; x++){
                int check = grid[y][x];
                if(check >= 0){
                    int up = grid[y-1][x];
                    int down = grid[y+1][x];
                    int left = grid[y][x-1];
                    int right = grid[y][x+1];
                    if((up == -1) || (down == -1) || (left == -1) || (right == -1)) accessibleIslands.add(check);
                }
            }
        }
        //"Matched" islands are Accessible islands that actually do contain some of this ColorLayer's assigned color.
        TreeSet<Integer> matchedIslands = new TreeSet<>();
        for(int y=0; y<mask.height; y++){
            for(int x=0; x<mask.width; x++){
                int check = grid[y][x];
                if(accessibleIslands.contains(check) && mask.getBit(x, y)){
                    matchedIslands.add(check);
                }
            }
        }
        return matchedIslands.stream().mapToInt(i->i).toArray();
    }

    public void generateChildren(BitGrid prevMask){
        for(int y=y_min; y<=y_max; y++){
            for(int x=x_min; x<=x_max; x++){
                if(mask.getBit(x-x_min, y-y_min)){
                    prevMask.setBit(x, y, true);
                }
            }
        }
        int[][] grid = new int[mask.height][mask.width];
        for(int y=y_min; y<=y_max; y++){
            for(int x=x_min; x<=x_max; x++){
                if(prevMask.getBit(x, y)){
                    grid[y-y_min][x-x_min] = -1;
                } else {
                    grid[y-y_min][x-x_min] = -2;
                }
            }
        }
        int islandCount = 0;
        for(int y=0; y<mask.height; y++){
            for(int x=0; x<mask.width; x++){
                if(grid[y][x] == -1){
                    FloodFills.fourDirectionFill(grid, new IntPoint(x, y), -1, islandCount);
                    islandCount++;
                }
            }
        }
        //Scan edges to change outer ocean from -2 to -1
        for(int x=0; x<mask.width; x++){
            if(grid[0][x] == -2) FloodFills.eightDirectionFill(grid, new IntPoint(x, 0), -2, -1);
        }
        for(int y=1; y<mask.height; y++){
            if(grid[y][0] == -2) FloodFills.eightDirectionFill(grid, new IntPoint(0, y), -2, -1);
            if(grid[y][mask.width-1] == -2) FloodFills.eightDirectionFill(grid, new IntPoint(mask.width-1, y), -2, -1);
        }
        for(int x=1; x<mask.width-1; x++){
            if(grid[mask.height-1][x] == -2) FloodFills.eightDirectionFill(grid, new IntPoint(x, mask.height-1), -2, -1);
        }
        //At this point, the only cells with -2 in them must be inner trapped pools inside islands.
        //(Specifically pools that cannot be accessed from the outer edge by eight-directional travel.)
        //Now it's time to validate which islands are top-level.
        int[] validIslands = determineValidIslands(grid);
        children = new Island[validIslands.length];
        for(int i=0; i<validIslands.length; i++){
            int index = validIslands[i];
            int local_x_min = mask.width;
            int local_x_max = -1;
            int local_y_min = mask.height;
            int local_y_max = -1;
            for(int y=0; y<mask.height; y++){
                for(int x=0; x<mask.width; x++){
                    if(grid[y][x] == index){
                        local_x_min = Math.min(local_x_min, x);
                        local_x_max = Math.max(local_x_max, x);
                        local_y_min = Math.min(local_y_min, y);
                        local_y_max = Math.max(local_y_max, y);
                    }
                }
            }
            int island_width = (local_x_max - local_x_min) + 1;
            int island_height = (local_y_max - local_y_min) + 1;
            BitGrid islandBits = new BitGrid(island_width, island_height);
            for(int y=local_y_min; y<=local_y_max; y++){
                for(int x=local_x_min; x<=local_x_max; x++){
                    if(grid[y][x] == index){
                        islandBits.setBit(x-local_x_min, y-local_y_min, true);
                    }
                }
            }
            children[i] = new Island(local_x_min + x_min, local_y_min + y_min, islandBits);
        }
    }

    public void printSVG(ArrayList<String> lines){
        String colorStr = "#" + Main.leftPad(Integer.toHexString(color & 0xFFFFFF).toUpperCase(), '0', 6);
        if(children.length == 1){
            lines.add("<path fill=\"" + colorStr + "\" d=\"" + children[0].pathTrace() + "\" />");
        } else {
            lines.add("<g fill=\"" + colorStr + "\">");
            lines.add("+");
            for(Island child : children){
                lines.add("<path d=\"" + child.pathTrace() + "\" />");
            }
            lines.add("-");
            lines.add("</g>");
        }
    }
}