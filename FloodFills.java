public class FloodFills {
    public static void fourDirectionFill(int[][] grid, IntPoint start, int target, int newVal){
        IntQueue points = new IntQueue(grid.length * grid[0].length * 4);
        points.add(start.x, start.y);
        while(!points.isEmpty()){
            int x = points.poll();
            int y = points.poll();
            if(grid[y][x] != target) continue;
            grid[y][x] = newVal;

            if(x > 0) points.add(x-1, y);
            if(x < grid[0].length - 1) points.add(x+1, y);
            if(y > 0) points.add(x, y-1);
            if(y < grid.length - 1) points.add(x, y+1);
        }
    }

    public static void eightDirectionFill(int[][] grid, IntPoint start, int target, int newVal){
        IntQueue points = new IntQueue(grid.length * grid[0].length * 8);
        points.add(start.x, start.y);
        while(!points.isEmpty()){
            int x = points.poll();
            int y = points.poll();
            if(grid[y][x] != target) continue;
            grid[y][x] = newVal;

            if(x > 0){
                points.add(x-1, y);
                if(y > 0) points.add(x-1, y-1);
                if(y < grid.length - 1) points.add(x-1, y+1);
            }
            if(x < grid[0].length - 1){
                points.add(x+1, y);
                if(y > 0) points.add(x+1, y-1);
                if(y < grid.length - 1) points.add(x+1, y+1);
            }
            if(y > 0) points.add(x, y-1);
            if(y < grid.length - 1) points.add(x, y+1);
        }
    }
}