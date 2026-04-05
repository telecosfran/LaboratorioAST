package practica1.CircularQ;

import java.util.Iterator;
import util.Queue;

public class CircularQueue<E> implements Queue<E> {

    private final E[] queue;
    private final int N;
    private int G; // Puntero de inicio (Head)
    private int P; // Puntero de fin (Tail)
    private int n; // Número de elementos 

    public CircularQueue(int N) {
        this.N = N;
        this.queue = (E[]) (new Object[N]);
        this.G = 0;
        this.P = 0;
        this.n = 0;
    }

    @Override
    public int size() {
        return this.n;
    }

    @Override
    public int free() {
        return this.N - this.n;
    }

    @Override
    public boolean empty() {
        return this.n == 0;
    }

    @Override
    public boolean full() {
        return this.N == this.n;
    }

    @Override
    public E peekFirst() {
        if (this.empty()) {
            System.out.println("Queue is empty, there is no first element");
            return null;
        }
        return this.queue[G];
    }

    @Override
    public E get() {
        if (empty()) {
            System.out.println("Queue is empty");
            return null;
        }

        E item = this.queue[G];
        this.queue[G] = null;

        G = (G + 1) % N;
        this.n--;

        return item;
    }

    @Override
    public void put(E e) {
        if (full()) {
            System.out.println("Queue is full");
            return;
        }

        this.queue[P] = e;
        P = (P + 1) % N;

        this.n++;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < this.n; i++) {
            int index = (this.G + i) % this.N;
            sb.append(this.queue[index]);

            if (i < this.n - 1) {
                sb.append(", ");
            }
        }

        sb.append("]");
        return sb.toString();
    }

    @Override
    public Iterator<E> iterator() {
        return new MyIterator();
    }

    private class MyIterator implements Iterator<E> {

        private int count = 0;

        @Override
        public boolean hasNext() {
            return this.count < n;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new java.util.NoSuchElementException();
            }

            int index = (G + count) % N;
            E item = queue[index];

            count++;
            return item;
        }

        @Override
        public void remove() {
            if (count == 0) {
                throw new IllegalStateException("Has de fer next() abans de remove()");
            }

            int curr = (G + count - 1 + N) % N;

            while (curr != (P - 1 + N) % N) {
                int nextNeighbor = (curr + 1) % N;
                queue[curr] = queue[nextNeighbor];
                curr = nextNeighbor;
            }

            queue[curr] = null;
            n--;
            P = curr;
            count--;
        }
    }
}
