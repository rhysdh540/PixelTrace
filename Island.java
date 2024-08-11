public class Island {
    private int global_x_min;
    private int global_y_min;
    private BitGrid pixels;

    public Island(int x_min, int y_min, BitGrid pixels_input){
        global_x_min = x_min;
        global_y_min = y_min;
        pixels = pixels_input;
    }
}