public final class UnionFind{
    private final int[] parent;
    private final byte[] rank;

    public UnionFind(int n){
        parent = new int[n];
        rank = new byte[n];
        for(int i=0; i<n; i++){
            parent[i] = i;
            rank[i] = 0;
        }
    }

    public int find(int x){
        int root = x;
        while(parent[root] != root) root = parent[root];

        // compress path back up the chain
        for(int p; (p = parent[x]) != x; x = p){
            parent[x] = root;
        }
        return root;
    }

    public void union(int a, int b){
        int ra = find(a), rb = find(b);
        if(ra == rb) return;
        byte raRank = rank[ra], rbRank = rank[rb];
        if(raRank < rbRank){
            parent[ra] = rb;
        } else if(raRank > rbRank){
            parent[rb] = ra;
        } else {
            parent[rb] = ra;
            rank[ra]++;
        }
    }
}
