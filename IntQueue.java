public class IntQueue {
	private final int[] arr;
	private int head = 0;
	private int tail = 0;
	private int size = 0;

	private final int arraySize;

	public IntQueue(int capacity){
		arr = new int[this.arraySize = capacity + 1];
	}

	private int wrapIncrement(int i){
		if(++i == arraySize) i = 0;
		return i;
	}

	public void add(int i){
		arr[tail] = i;
		if((tail = wrapIncrement(tail)) == head){
			throw new IllegalStateException("Queue is full.");
		}
		size++;
	}

	public int poll(){
		if(isEmpty()){
			throw new IllegalStateException("Queue is empty.");
		}
		int result = arr[head];
		head = wrapIncrement(head);
		size--;
		return result;
	}

	public void add(int a, int b){
		if(size() + 2 > arraySize - 1) {
			throw new IllegalStateException("Not enough space to add two elements.");
		}
		add(a);
		add(b);
	}

	public final boolean isEmpty(){
		return head == tail;
	}

	public int size(){
		return size;
	}

	public int capacity(){
		return arraySize - 1;
	}
}
