import java.io.File;
import java.util.LinkedHashSet;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

class Main{
    public static String leftPad(String original, char pad, int minLength){
        StringBuilder sb = new StringBuilder(original);
        while(sb.length() < minLength) sb.insert(0, pad);
        return sb.toString();
    }
    public static void main(String[] args) throws Exception{
        BufferedImage original = ImageIO.read(new File("TestBitmaps/Lakitu.png"));
        LinkedHashSet<Integer> colors = new LinkedHashSet<>();
        final int width = original.getWidth();
        final int height = original.getHeight();
        for(int y=0; y<height; y++){
            for(int x=0; x<width; x++){
                colors.add(original.getRGB(x, y));
            }
        }
        for(int c : colors){
            System.out.println(leftPad(Integer.toHexString(c), '0', 8));
        }
    }
}