public class BitGrid {
    private final int width;
    private final int height;
    private long[] binData;

    public BitGrid(int new_width, int new_height){
        if(new_width < 1){
            throw new IllegalArgumentException("Width must be at least 1. \"" + new_width + "\" was specified.");
        }
        width = new_width;
        if(new_height < 1){
            throw new IllegalArgumentException("Height must be at least 1. \"" + new_height + "\" was specified.");
        }
        height = new_height;
        long pixels = ((long) width) * ((long) height);
        long cells = pixels / 64;
        if(cells > Integer.MAX_VALUE){
            throw new IllegalArgumentException("Dimensions of " + width + "x" + height + " would require the storage of " + cells + " longs, which is beyond indexing maximum of " + Integer.MAX_VALUE + ".");
        }
        int cells_int = (int) cells;
        long extra = pixels % 64;
        if(extra == 0){
            binData = new long[cells_int];
        } else {
            binData = new long[cells_int + 1];
        }
    }

    //TODO: Refactor the indexing protocol

    public void setBit(int x, int y){
        if(x < 0){
            throw new IllegalArgumentException("X must be at least 0. \"" + x + "\" was specified.");
        }
        if(y < 0){
            throw new IllegalArgumentException("Y must be at least 0. \"" + y + "\" was specified.");
        }
        final int xMax = width - 1;
        final int yMax = height - 1;
        if(x > xMax){
            throw new IllegalArgumentException("X must be at most " + xMax + ". \"" + x + "\" was specified.");
        }
        if(y > yMax){
            throw new IllegalArgumentException("Y must be at most " + yMax + ". \"" + y + "\" was specified.");
        }
        //Wait just a second...
    }
}
