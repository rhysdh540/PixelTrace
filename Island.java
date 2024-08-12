import java.util.*;

public class Island {
    static private final int RIGHT = 0;
    static private final int DOWN = 1;
    static private final int LEFT = 2;
    static private final int UP = 3;

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
        int direction = RIGHT;
        StringBuilder buf = new StringBuilder("M " + (global_x_min + start.x) + " " + (global_y_min + start.y));
        while((cur_x != start.x) || (cur_y != start.y)){
            int turn = cornerTable.get(direction).getOrDefault(fourSquareVal(cur_x, cur_y), -1);
            if(turn >= 0){
                if((direction == RIGHT) || (direction == LEFT)){ //Horizontal Line
                    buf.append(" h ");
                    buf.append(cur_x - prev_x);
                    prev_x = cur_x;
                } else { //Vertical Line
                    buf.append(" v ");
                    buf.append(cur_y - prev_y);
                    prev_y = cur_y;
                }
                direction = turn;
            }
            switch(direction){
                case RIGHT:
                    cur_x++;
                    break;
                case DOWN:
                    cur_y++;
                    break;
                case LEFT:
                    cur_x--;
                    break;
                default: //case UP
                    cur_y--;
                    break;
            }
        }
        buf.append(" z");
        return buf.toString();
    }
}