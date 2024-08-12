import java.util.*;

public class Island {
    static private final List<TreeMap<Integer, Integer>> cornerTable = List.of(
        //Index 0 = Right
        new TreeMap<Integer, Integer>(Map.of(2,1,8,3,13,1,9,3,6,1,7,3)),
        //Index 1 = Down
        new TreeMap<Integer, Integer>(Map.of(8,2,4,0,9,2,6,0,7,2,11,0)),
        //Index 2 = Left
        new TreeMap<Integer, Integer>(Map.of(1,1,4,3,14,1,9,1,6,3,11,3)),
        //Index 3 = Up
        new TreeMap<Integer, Integer>(Map.of(1,0,2,2,14,0,13,2,9,0,6,2))
    );

    private int global_x_min;
    private int global_y_min;
    private BitGrid pixels;

    public Island(int x_min, int y_min, BitGrid pixels_input){
        global_x_min = x_min;
        global_y_min = y_min;
        pixels = pixels_input;
    }

    private boolean safeLookup(int x, int y){
        if(x < 0) return false;
        if(x >= pixels.width) return false;
        if(y < 0) return false;
        if(y >= pixels.height) return false;
        return pixels.getBit(x, y);
    }

    private int fourSquareVal(int x, int y){
        int eight = safeLookup(x-1, y-1) ? 8 : 0;
        int four = safeLookup(x, y-1) ? 4 : 0;
        int two = safeLookup(x-1, y) ? 2 : 0;
        int one = safeLookup(x, y) ? 1 : 0;
        return eight | four | two | one;
    }

    private IntPoint findUpperLeftCorner(){
        for(int y=0; y<pixels.height; y++){
            for(int x=0; x<pixels.width; x++){
                if(fourSquareVal(x, y) == 1){
                    return new IntPoint(x, y);
                }
            }
        }
        return new IntPoint(0, 0); //Just in case...
    }

    public String pathTrace(){
        IntPoint start = findUpperLeftCorner();
        int prev_x = start.x;
        int prev_y = start.y;
        int cur_x = start.x+1;
        int cur_y = start.y;
        int direction = 0;
        StringBuilder buf = new StringBuilder("M " + (global_x_min + start.x) + " " + (global_y_min + start.y));
        while(true){
            if((cur_x == start.x) && (cur_y == start.y)){
                buf.append(" z");
                break;
            }
            int check = fourSquareVal(cur_x, cur_y);
            int turn = cornerTable.get(direction).getOrDefault(check, -1);
            if(turn >= 0){
                if((direction == 0) || (direction == 2)){
                    //Horizontal Line
                    buf.append(" h ");
                    buf.append(cur_x - prev_x);
                    prev_x = cur_x;
                } else {
                    //Vertical Line
                    buf.append(" v ");
                    buf.append(cur_y - prev_y);
                    prev_y = cur_y;
                }
                direction = turn;
            }
            switch(direction){
                case 0:
                    cur_x++;
                    break;
                case 1:
                    cur_y++;
                    break;
                case 2:
                    cur_x--;
                    break;
                default: //case 3
                    cur_y--;
                    break;
            }
        }
        return buf.toString();
    }
}