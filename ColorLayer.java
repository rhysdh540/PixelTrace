import java.io.IOException;
import java.util.*;

public class ColorLayer implements Comparable<ColorLayer>{
    public final int color;
    private final int x_min;
    private final int x_max;
    private final int y_min;
    private final int y_max;
    private final long bounding_area;
    private final int pixel_count;
    private final BitGrid mask;
    private Island[] children;

    private static final Island[] EMPTY_ISLANDS = new Island[0];

    public ColorLayer(int new_color, IntPointQueueBounded detections){
        color = new_color;
        x_min = detections.x_min();
        x_max = detections.x_max();
        y_min = detections.y_min();
        y_max = detections.y_max();
        pixel_count = detections.size();
        long width = (x_max - x_min) + 1;
        long height = (y_max - y_min) + 1;
        bounding_area = width * height;
        mask = new BitGrid((int) width, (int) height);
        while(!detections.isEmpty()){
            long packed_point = detections.poll();
            int x = (int) (packed_point >>> 32);
            int y = (int) packed_point;
            mask.setBit(x-x_min, y-y_min, true);
        }
        children = EMPTY_ISLANDS;
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
        sb.append(System.lineSeparator());
        sb.append(children.length);
        if(children.length == 1){
            sb.append(" stored child.");
        } else {
            sb.append(" stored children.");
        }
        return sb.toString();
    }

    @Override
    public int compareTo(ColorLayer other) {
        int alpha_compare = Integer.compare(color >>> 24, other.color >>> 24); //Lower alpha is further towards the back
        if(alpha_compare == 0){
            int area_compare = Long.compare(other.bounding_area, bounding_area); //Greater area is further towards the back
            if(area_compare == 0){
                int count_compare = Integer.compare(other.pixel_count, pixel_count); //Greater pixel count is further towards the back
                if(count_compare == 0){
                    return Integer.compareUnsigned(color & 0xFFFFFF, other.color & 0xFFFFFF); //Darker color is further towards the back
                }
                return count_compare;
            }
            return area_compare;
        }
        return alpha_compare;
    }

    public void generateChildren(BitGrid prevMask){
        final int W = mask.width;
        final int H = mask.height;

        // merge this layer into previous (for next layer's processing)
        for(int y=0; y<H; y++){
            final int gy = y + y_min;
            for(int x=0; x<W; x++){
                if(mask.getBit(x, y)) prevMask.setBit(x + x_min, gy, true);
            }
        }

        // collect runs of true pixels in prevMask over this layer's bounding box
        ConnectedComponentLabeling.RunSet set = ConnectedComponentLabeling.collectRuns(prevMask, x_min, y_min, W, H, true);
        int totalRuns = set.size();
        if(totalRuns == 0){
            children = EMPTY_ISLANDS;
            return;
        }

        // determine which runs touch this layer's color mask
        boolean[] touches = new boolean[totalRuns];
        for(int idx=0; idx<totalRuns; idx++){
            ConnectedComponentLabeling.Run r = set.runs().get(idx);
            int y = r.y();
            // short-circuit scan: any bit of current color mask within run range
            for(int x=r.x0(); x<=r.x1(); x++){
                if(mask.getBit(x, y)){
                    touches[idx] = true;
                    break;
                }
            }
        }

        UnionFind uf = ConnectedComponentLabeling.unionRows(set, 0, 0, null);
        BitSet keepRoots = ConnectedComponentLabeling.rootsWithFlags(uf, touches);
        if(keepRoots.isEmpty()){
            children = EMPTY_ISLANDS;
            return;
        }

        int[] rootToIdx = ConnectedComponentLabeling.mapRoots(uf, totalRuns, keepRoots);
        int kept = keepRoots.cardinality();
        if(kept == 0){
            children = EMPTY_ISLANDS;
            return;
        }

        int[] minX = new int[kept], maxX = new int[kept];
        int[] minY = new int[kept], maxY = new int[kept];
        ConnectedComponentLabeling.computeBoundingBoxes(set, uf, rootToIdx, minX, maxX, minY, maxY);
        BitGrid[] bits = ConnectedComponentLabeling.buildComponentGrids(set, uf, rootToIdx, minX, maxX, minY, maxY);

        children = new Island[kept];

        for(int i=0; i<kept; i++){
            children[i] = new Island(minX[i] + x_min, minY[i] + y_min, bits[i], true);
        }
    }

    public void printSVG(ObscurePrint out) throws IOException {
        String colorSpec = "fill=\"#" + Main.leftPad(Integer.toHexString(color & 0xFFFFFF).toUpperCase(), '0', 6) + "\"";
        int alpha = color >>> 24;
        if(alpha != 0xFF){
            float opacity = ((float) alpha) / 255.0f;
            colorSpec += " fill-opacity=\"" + opacity + "\"";
        }
        out.print("<path " + colorSpec + " d=\"");
        children[0].traceSVG(out);
        for(int i=1; i<children.length; i++){
            out.print(" ");
            children[i].traceSVG(out);
        }
        out.println("\" />");
    }

    public void printTikZ(ObscurePrint out, final int globalHeight) throws IOException {
        final String colorName = "pt-" + Main.leftPad(Integer.toHexString(color & 0xFFFFFF).toUpperCase(), '0', 6);
        out.print("\\definecolor{");
        out.print(colorName);
        out.print("}{RGB}{");
        out.print((color >>> 16) & 0xFF);
        out.print(", ");
        out.print((color >>> 8) & 0xFF);
        out.print(", ");
        out.print(color & 0xFF);
        out.println("}");
        out.print("\\fill[");
        out.print(colorName);
        final int alpha = color >>> 24;
        if(alpha != 0xFF){
            float opacity = ((float) alpha) / 255.0f;
            out.print(",opacity=" + opacity);
        }
        out.print("]");
        children[0].traceTikZ(out, globalHeight);
        for(int i=1; i<children.length; i++){
            out.print(" ");
            children[i].traceTikZ(out, globalHeight);
        }
        out.println(";");
    }
}