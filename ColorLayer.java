import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.TreeSet;

public class ColorLayer implements Comparable<ColorLayer>{
    private final int color;
    private final int x_min;
    private final int x_max;
    private final int y_min;
    private final int y_max;
    private final long bounding_area;
    private final long pixel_count;

    public ColorLayer(int new_color, BufferedImage bitmap){
        color = new_color;
        int temp_x_min = Integer.MAX_VALUE;
        int temp_x_max = Integer.MIN_VALUE;
        int temp_y_min = Integer.MAX_VALUE;
        int temp_y_max = Integer.MIN_VALUE;
        long temp_count = 0;
        for(int y=0; y<bitmap.getHeight(); y++){
            for(int x=0; x<bitmap.getWidth(); x++){
                if(bitmap.getRGB(x, y) == color){
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

    public void trace(BitGrid prevMask, BufferedImage original){
        for(int y=0; y<original.getHeight(); y++){
            for(int x=0; x<original.getWidth(); x++){
                if(original.getRGB(x, y) == color){
                    prevMask.setBit(x, y, true);
                }
            }
        }
        int local_width = (x_max - x_min) + 1;
        int local_height = (y_max - y_min) + 1;
        int[][] grid = new int[local_height][local_width];
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
        for(int y=0; y<local_height; y++){
            for(int x=0; x<local_width; x++){
                if(grid[y][x] == -1){
                    FloodFills.fourDirectionFill(grid, new IntPoint(x, y), -1, islandCount);
                    islandCount++;
                }
            }
        }
        //Scan edges to change outer ocean from -2 to -1
        for(int x=0; x<local_width; x++){
            if(grid[0][x] == -2) FloodFills.eightDirectionFill(grid, new IntPoint(x, 0), -2, -1);
        }
        for(int y=1; y<local_height; y++){
            if(grid[y][0] == -2) FloodFills.eightDirectionFill(grid, new IntPoint(0, y), -2, -1);
            if(grid[y][local_width-1] == -2) FloodFills.eightDirectionFill(grid, new IntPoint(local_width-1, y), -2, -1);
        }
        for(int x=1; x<local_width-1; x++){
            if(grid[local_height-1][x] == -2) FloodFills.eightDirectionFill(grid, new IntPoint(x, local_height-1), -2, -1);
        }
        //At this point, the only cells with -2 in them must be inner trapped pools inside islands.
        //(Specifically pools that cannot be accessed from the outer edge by eight-directional travel.)
        //Now it's time to validate which islands are top-level.
        int[] validIslands = new int[0];
        {
            //"Accessible" islands either touch the outer border of this ColorLayer,
            //or else come into contact with some "-1" somewhere on the island's edge.
            //Islands that don't match either of those criteria must be entirely surrounded by "-2",
            //and are thus supposed to be saved for a later recursive pass.
            TreeSet<Integer> accessibleIslands = new TreeSet<>();
            for(int x=0; x<local_width; x++){
                int check = grid[0][x];
                if(check >= 0) accessibleIslands.add(check);
            }
            for(int y=1; y<local_height; y++){
                int check = grid[y][0];
                if(check >= 0) accessibleIslands.add(check);
                check = grid[y][local_width-1];
                if(check >= 0) accessibleIslands.add(check);
            }
            for(int x=1; x<local_width-1; x++){
                int check = grid[local_height-1][x];
                if(check >= 0) accessibleIslands.add(check);
            }
            for(int y=1; y<local_height-1; y++){
                for(int x=1; x<local_width-1; x++){
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
            for(int y=0; y<local_height; y++){
                for(int x=0; x<local_width; x++){
                    int check = grid[y][x];
                    if(accessibleIslands.contains(check) && (original.getRGB(x+x_min, y+y_min) == color)){
                        matchedIslands.add(check);
                    }
                }
            }
            validIslands = matchedIslands.stream().mapToInt(i->i).toArray();
        }//Getting the "accessibleIslands" and "matchedIslands" objects out of scope.
        System.gc();
        try{
            PrintStream ps = new PrintStream(new FileOutputStream("Debug-" + Main.leftPad(Integer.toHexString(color).toUpperCase(), '0', 8) + ".csv", false), true);
            for(int[] row : grid){
                ps.print(row[0]);
                for(int i=1; i<row.length; i++){
                    ps.print(", " + row[i]);
                }
                ps.println();
            }
            ps.print("V:, " + validIslands[0]);
            for(int i=1; i<validIslands.length; i++){
                ps.print(", " + validIslands[i]);
            }
            ps.println();
            ps.close();
        } catch (Exception ex){}
    }
}