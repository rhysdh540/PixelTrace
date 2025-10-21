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

    private int[] getMatchedIslands(int[][] grid){
        BitSet matchedIslands = new BitSet();
        for(int y=0; y<mask.height; y++){
            for(int x=0; x<mask.width; x++){
                if(mask.getBit(x, y)){
                    matchedIslands.set(grid[y][x]);
                }
            }
        }
        return matchedIslands.stream().toArray();
    }

    public void generateChildren(BitGrid prevMask) {
        // a horizontal run of solid pixels in prevMask
        record Run(int y, int x0, int x1, boolean touchesMask, int parentIndex) {}

        final int W = mask.width;
        final int H = mask.height;

        // merge this layer into previous (for next layer's processing)
        for (int y = 0; y < H; y++) {
            final int gy = y + y_min;
            for (int x = 0; x < W; x++) {
                if (mask.getBit(x, y)) prevMask.setBit(x + x_min, gy, true);
            }
        }

        List<Run> runs = new ArrayList<>();

        // indices of the first/last runs in each row
        int[] rowStarts = new int[H];
        int[] rowEnds = new int[H];

        // populate runs from prevMask
        for (int y = 0; y < H; y++) {
            rowStarts[y] = runs.size();
            final int gy = y + y_min;

            int x = 0;
            while (x < W) {
                // find start of a solid run in prevMask
                while (x < W && !prevMask.getBit(x + x_min, gy)) x++;
                if (x >= W) break;
                int x0 = x;
                boolean touched = false;
                // extend run; track if it touches current color's mask
                do {
                    if (!touched && mask.getBit(x, y)) touched = true;
                    x++;
                } while (x < W && prevMask.getBit(x + x_min, gy));

                int x1 = x - 1;

                Run r = new Run(y, x0, x1, touched, runs.size());
                runs.add(r);
            }
            rowEnds[y] = runs.size();
        }

        int totalRuns = runs.size();
        if (totalRuns == 0) {
            children = EMPTY_ISLANDS;
            return;
        }

        UnionFind uf = new UnionFind(totalRuns);

        // connect runs between adjacent rows
        for (int y = 1; y < H; y++) {
            int prevStart = rowStarts[y-1], prevEnd = rowEnds[y-1];
            int currStart = rowStarts[y], currEnd = rowEnds[y];

            while (prevStart < prevEnd && currStart < currEnd) {
                Run prev = runs.get(prevStart);
                Run curr = runs.get(currStart);

                // they overlap if their x-ranges intersect
				if (Math.max(prev.x0, curr.x0) <= Math.min(prev.x1, curr.x1)) {
                    uf.union(prevStart, currStart);
                    // advance the one that ends first
                    if (prev.x1 < curr.x1) {
                        prevStart++;
                    } else {
                        currStart++;
                    }
                } else {
                    if (prev.x1 < curr.x0) {
                        prevStart++;
                    } else {
                        currStart++;
                    }
                }
            }
        }

        // filter the connected components to only those touching the mask
        // any not touching the mask aren't part of this color layer
        int[] rootToIdx = new int[totalRuns];
        Arrays.fill(rootToIdx, -1);
        BitSet keepRoots = new BitSet();

        for (int r = 0; r < totalRuns; r++) {
            if (runs.get(r).touchesMask) {
                keepRoots.set(uf.find(r));
            }
        }

        int kept = 0;
        for (int r = 0; r < totalRuns; r++) {
            int rr = uf.find(r);
            if (!keepRoots.get(rr)) continue;
            if (rootToIdx[rr] == -1) {
                rootToIdx[rr] = kept++;
            }
        }

        if (kept == 0) {
            children = EMPTY_ISLANDS;
            return;
        }

        int[] minX = new int[kept], maxX = new int[kept];
        int[] minY = new int[kept], maxY = new int[kept];
        Arrays.fill(minX, W);
        Arrays.fill(minY, H);
        Arrays.fill(maxX, -1);
        Arrays.fill(maxY, -1);

        for (int r = 0; r < totalRuns; r++) {
            int idx = rootToIdx[uf.find(r)];
            if (idx == -1) continue;
			Run run = runs.get(r);
            if (run.x0 < minX[idx]) minX[idx] = run.x0;
            if (run.x1 > maxX[idx]) maxX[idx] = run.x1;
            if (run.y < minY[idx]) minY[idx] = run.y;
            if (run.y > maxY[idx]) maxY[idx] = run.y;
        }

        children = new Island[kept];
        BitGrid[] bits = new BitGrid[kept];
        for (int i = 0; i < kept; i++) {
            int w = (maxX[i] - minX[i]) + 1;
            int h = (maxY[i] - minY[i]) + 1;
            bits[i] = new BitGrid(w, h);
        }

        for (int r = 0; r < totalRuns; r++) {
            int idx = rootToIdx[uf.find(r)];
            if (idx == -1) continue; // not kept
            Run run = runs.get(r);
            int localY = run.y - minY[idx];
            int startX = run.x0 - minX[idx];
            int len = run.x1 - run.x0 + 1;
            bits[idx].setSpan(startX, localY, len, true);
        }

        for (int i = 0; i < kept; i++) {
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