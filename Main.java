import java.util.*;
import java.util.Map.Entry;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

class Main{
    public static String leftPad(String original, char pad, int minLength){
        StringBuilder sb = new StringBuilder(original);
        while(sb.length() < minLength) sb.insert(0, pad);
        return sb.toString();
    }

    private static ColorLayer[] createLayers(BufferedImage bitmap){
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        HashMap<Integer, ArrayList<IntPoint>> detections = new HashMap<>();
        for(int y=0; y<height; y++){
            for(int x=0; x<width; x++){
                //TODO: Remove this alpha channel coercion when alpha support is ready.
                int color = bitmap.getRGB(x, y) | 0xFF000000;
                if(detections.containsKey(color)){
                    detections.get(color).add(new IntPoint(x, y));
                } else {
                    ArrayList<IntPoint> newList = new ArrayList<>();
                    newList.add(new IntPoint(x, y));
                    detections.put(color, newList);
                }
            }
        }
        ArrayList<ColorLayer> layers = new ArrayList<>();
        for(Entry<Integer, ArrayList<IntPoint>> item : detections.entrySet()){
            layers.add(new ColorLayer(item.getKey(), item.getValue()));
        }
        System.out.println(layers.size() + " ColorLayers created.");
        return layers.toArray(new ColorLayer[0]);
    }
    public static void main(String[] args) throws Exception{
        final long startTime = System.nanoTime();
        BufferedImage original = ImageIO.read(new File("TestBitmaps/Rings.png"));
        final int width = original.getWidth();
        final int height = original.getHeight();
        ColorLayer[] layers = createLayers(original);
        System.gc();
        Arrays.sort(layers);
        BitGrid stackedBits = new BitGrid(width, height);
        for(int i=0; i<layers.length; i++){
            if(i % 100 == 0){
                System.out.print(i + " ColorLayers chunked.\r");
                System.out.flush();
                if(i % 1000 == 0){
                    System.gc();
                }
            }
            //Index math necessary: ColorLayers sorted back-to-front, but must be traced front-to-back.
            layers[layers.length-1-i].generateChildren(stackedBits);
        }
        System.gc();
        System.out.println(layers.length + " ColorLayers chunked.");
        PrintSVG fileOut = new PrintSVG(new File("Testing.svg"));
        fileOut.println("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + width + "\" height=\"" + height + "\" viewBox=\"0 0 " + width + " " + height + "\" shape-rendering=\"crispEdges\">");
        fileOut.moreIndent();
        for(ColorLayer layer : layers){
            layer.printSVG(fileOut);
        }
        fileOut.lessIndent();
        fileOut.print("</svg>");
        fileOut.close();
        final long endTime = System.nanoTime();
        String seconds = leftPad(Long.toString(endTime-startTime), '0', 10);
        seconds = seconds.substring(0,seconds.length()-9) + "." + seconds.substring(seconds.length()-9);
        System.out.println("Finished in " + seconds + " seconds.");
    }
}