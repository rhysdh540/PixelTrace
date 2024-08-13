import java.util.*;
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
                int color = bitmap.getRGB(x, y);
                if(detections.containsKey(color)){
                    detections.get(color).add(new IntPoint(x, y));
                } else {
                    ArrayList<IntPoint> newList = new ArrayList<>();
                    newList.add(new IntPoint(x, y));
                    detections.put(color, newList);
                }
            }
        }
        ColorLayer[] layers = new ColorLayer[detections.size()];
        int counter = 0;
        for(int color : detections.keySet()){
            if(counter % 100 == 0){
                System.out.print(counter + " ColorLayers created.\r");
                System.out.flush();
            }
            layers[counter] = new ColorLayer(color, detections.get(color));
            counter++;
        }
        System.out.println(counter + " ColorLayers created.");
        return layers;
    }
    public static void main(String[] args) throws Exception{
        final long startTime = System.currentTimeMillis();
        BufferedImage original = ImageIO.read(new File("TestBitmaps/WeezerSmall.png"));
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
        //The structure of the following code is totally subject to change.
        //I'm likely to implement a new class to automate more of the XML output process.
        //For now I just wanted to hack someting together to start inspecing some visual output.
        //(And also start profiling some of the code I've already written.)
        ArrayList<String> svgLines = new ArrayList<>();
        svgLines.add("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + width + " " + height + "\" shape-rendering=\"crispEdges\">");
        svgLines.add("+");
        for(ColorLayer layer : layers){
            layer.printSVG(svgLines);
        }
        svgLines.add("-");
        svgLines.add("</svg>");
        int indent = 0;
        PrintStream ps = new PrintStream(new FileOutputStream("Testing.svg", false), true);
        for(String line : svgLines){
            switch(line){
                case "+":
                    indent++;
                    break;
                case "-":
                    indent--;
                    break;
                default:
                    ps.print("    ".repeat(indent));
                    ps.print(line);
                    ps.print("\n");
                    break;
            }
        }
        ps.close();
        final long endTime = System.currentTimeMillis();
        String seconds = leftPad(Long.toString(endTime-startTime), '0', 4);
        seconds = seconds.substring(0,seconds.length()-3) + "." + seconds.substring(seconds.length()-3);
        System.out.println("Finished in " + seconds + " seconds.");
    }
}