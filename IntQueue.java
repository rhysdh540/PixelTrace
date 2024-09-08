public class IntQueue {
	private final int[] arr;
	private int head = 0;
	private int tail = 0;

	public final int capacity;

	public IntQueue(int capacity){
		arr = new int[this.capacity = capacity + 1];
	}

	private int wrapIncrement(int i){
		if(++i == capacity) i = 0;
		return i;
	}

	public void add(int i){
		arr[tail] = i;
		if((tail = wrapIncrement(tail)) == head){
			throw new IllegalStateException("Queue is full.");
		}
	}

	public int poll(){
		if(isEmpty()){
			throw new IllegalStateException("Queue is empty.");
		}
		int result = arr[head];
		head = wrapIncrement(head);
		return result;
	}

	public void add(int a, int b){
		add(a);
		add(b);
	}

	public boolean isEmpty(){
		return head == tail;
	}

	public int size(){
		return (tail - head + arr.length) % arr.length;
	}
}
