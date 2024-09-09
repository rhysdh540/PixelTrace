import java.util.Arrays;
import java.util.NoSuchElementException;

public class IntQueue {
    private int[] arr;
    private int head = 0;
    private int tail = 0;

    public IntQueue(int capacity){
        arr = new int[capacity + 1];
        Arrays.fill(arr, Integer.MIN_VALUE);
    }

    public IntQueue(){
        this(64);
    }

    private void grow(){
        final int oldCapacity = arr.length;
        int amountToAdd = (oldCapacity < 64) ? (oldCapacity + 2) : (oldCapacity >> 1);

        int[] newElements = new int[oldCapacity + amountToAdd];
        System.arraycopy(arr, 0, newElements, 0, arr.length);
        arr = newElements;

        if (tail < head || (tail == head && arr[tail] != Integer.MIN_VALUE)) {
            // either the queue wraps around the end or its full (and the ending element isn't null, which shouldn't happen)
            // so move elements from head to the old end to the new end
            // for example if the old capacity was 10 and head was 5, elements 5-9 would be moved to position 15-19
            System.arraycopy(arr, head,
                    arr, head + amountToAdd,
                    oldCapacity - head);

            for (int i = head; i < head + amountToAdd; i++)
                arr[i] = Integer.MIN_VALUE;
            head += amountToAdd;
        }
    }

    private int wrapIncrement(int i){
        return ++i >= arr.length ? 0 : i;
	}

    public void add(int i){
        arr[tail] = i;
        tail = wrapIncrement(tail);
        if (head == tail)
            grow();
    }

    public int poll(){
        int i = arr[head];
        if(i == Integer.MIN_VALUE) {
            throw new NoSuchElementException();
        } else {
            arr[head] = Integer.MIN_VALUE;
            head = wrapIncrement(head);
            return i;
        }
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
