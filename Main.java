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
    public static void main(String[] args){
        BufferedImage original = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        try{
            original = ImageIO.read(new File("TestBitmaps/Lakitu.png"));
        } catch (IOException ex){
            System.err.println("Failed to open testing bitmap.");
            System.err.println("Exiting early.");
            System.exit(1);
        }
        final int width = original.getWidth();
        final int height = original.getHeight();
        ArrayList<ColorLayer> layers = new ArrayList<>();
        {
            HashSet<Integer> colors = new HashSet<>();
            for(int y=0; y<height; y++){
                for(int x=0; x<width; x++){
                    colors.add(original.getRGB(x, y));
                }
            }
            for(int c : colors){
                layers.add(new ColorLayer(c, original));
            }
        } // Getting the "colors" object out of scope
        System.gc();
        Collections.sort(layers);
        BitGrid stackedBits = new BitGrid(width, height);
        for(ColorLayer layer : layers.reversed()){
            layer.generateChildren(stackedBits, original);
        }
        //The structure of the following code is totally subject to change.
        //I'm likely to implement a new class to automate more of the XML output process.
        //For now I just wanted to hack someting together to start inspecing some visual output.
        try{
            ArrayList<String> svgLines = new ArrayList<>();
            svgLines.add("<svg viewBox=\"0 0 " + width + " " + height + "\" xmlns=\"http://www.w3.org/2000/svg\">");
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
        } catch (IOException ex){
            System.err.println("Failed to write final file.");
            System.err.println("Exiting early.");
            System.exit(1);
        }
    }
}