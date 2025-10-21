public class UnionFind {
	private final int[] parent;
	private final int[] rank;
	private final int width;

	public UnionFind(int width, int height){
		this.width = width;
		int size = width * height;
		parent = new int[size];
		rank = new int[size];
		for(int i=0; i<size; i++) {
			parent[i] = i;
			rank[i] = 0;
		}
	}

	private int findIndex(int x){
		int p = x;
		while(parent[p] != p){
			int gp = parent[parent[p]];
			parent[p] = gp;
			p = gp;
		}
		return p;
	}

	public int find(int x, int y){
		return findIndex(y * width + x);
	}

	public void union(int x1, int y1, int x2, int y2){
		int idx1 = y1 * width + x1;
		int idx2 = y2 * width + x2;
		int root1 = findIndex(idx1);
		int root2 = findIndex(idx2);
		if(root1 != root2){
			if(rank[root1] < rank[root2]){
				parent[root1] = root2;
			} else if(rank[root1] > rank[root2]){
				parent[root2] = root1;
			} else {
				parent[root2] = root1;
				rank[root1]++;
			}
		}
	}
}