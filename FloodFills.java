import java.util.ArrayDeque;

public class FloodFills {
    public static void fourDirectionFill(int[][] grid, IntPoint start, int target, int newVal){
        ArrayDeque<IntPoint> points = new ArrayDeque<>();
        points.addLast(start);
        while(!points.isEmpty()){
            IntPoint cur = points.pollFirst();
            if(cur.x < 0) continue;
            if(cur.x > (grid[0].length - 1)) continue;
            if(cur.y < 0) continue;
            if(cur.y > (grid.length - 1)) continue;
            if(grid[cur.y][cur.x] != target) continue;
            grid[cur.y][cur.x] = newVal;
            points.addLast(new IntPoint(cur.x-1, cur.y));
            points.addLast(new IntPoint(cur.x+1, cur.y));
            points.addLast(new IntPoint(cur.x, cur.y-1));
            points.addLast(new IntPoint(cur.x, cur.y+1));
        }
    }

    public static void eightDirectionFill(int[][] grid, IntPoint start, int target, int newVal){
        ArrayDeque<IntPoint> points = new ArrayDeque<>();
        points.addLast(start);
        while(!points.isEmpty()){
            IntPoint cur = points.pollFirst();
            if(cur.x < 0) continue;
            if(cur.x > (grid[0].length - 1)) continue;
            if(cur.y < 0) continue;
            if(cur.y > (grid.length - 1)) continue;
            if(grid[cur.y][cur.x] != target) continue;
            grid[cur.y][cur.x] = newVal;
            points.addLast(new IntPoint(cur.x-1, cur.y-1));
            points.addLast(new IntPoint(cur.x-1, cur.y));
            points.addLast(new IntPoint(cur.x-1, cur.y+1));
            points.addLast(new IntPoint(cur.x, cur.y-1));
            points.addLast(new IntPoint(cur.x, cur.y+1));
            points.addLast(new IntPoint(cur.x+1, cur.y-1));
            points.addLast(new IntPoint(cur.x+1, cur.y));
            points.addLast(new IntPoint(cur.x+1, cur.y+1));
        }
    }
}