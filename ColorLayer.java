import java.awt.image.BufferedImage;

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
}