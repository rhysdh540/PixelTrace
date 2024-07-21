import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

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

    private IndexAndMask indexBit(int x, int y){
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
        long pixel = ((long) y) * width + x;
        int index = (int)(pixel / 64);
        long mask = Long.MIN_VALUE >>> (pixel % 64);
        return new IndexAndMask(index, mask);
    }

    public void setBit(int x, int y, boolean value){
        IndexAndMask spot = indexBit(x, y);
        if(value){
            //Writing a 1 into position
            binData[spot.index] = binData[spot.index] | spot.mask;
        } else {
            //Writing a 0 into position
            binData[spot.index] = (binData[spot.index] | spot.mask) ^ spot.mask;
        }
    }

    public void toggleBit(int x, int y){
        IndexAndMask spot = indexBit(x, y);
        binData[spot.index] = binData[spot.index] ^ spot.mask;
    }

    public boolean getBit(int x, int y){
        IndexAndMask spot = indexBit(x, y);
        return (binData[spot.index] & spot.mask) != 0;
    }

    public void debugFile(File location) throws IOException{
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for(int y=0; y<height; y++){
            for(int x=0; x<width; x++){
                if(getBit(x, y)){
                    canvas.setRGB(x, y, -1);
                } else {
                    canvas.setRGB(x, y, 0);
                }
            }
        }
        ImageIO.write(canvas, "PNG", new FileOutputStream(location, false));
    }
}
