import java.util.*;

public class Island {
    static private List<TreeMap<Integer, Integer>> cornerTable = List.of(
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
}