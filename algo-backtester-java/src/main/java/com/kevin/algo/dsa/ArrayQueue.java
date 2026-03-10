package com.kevin.algo.dsa;

/**
 * ArrayQueue (circular)
 * ---------------------
 * Fixed capacity ring buffer (FIFO).
 * We'll use this for rolling-window indicators (e.g., SMA N=20).
 * - offer(x): add to tail if not full
 * - poll()  : remove from head if not empty
 * - peek()  : view head without removing
 *
 * Complexity:
 *   All ops O(1).
 */
public class ArrayQueue {
    private final double[] buf; // store doubles for SMA; keep it simple for now
    private int head = 0;       // index of current head
    private int tail = 0;       // index after last element
    private int size = 0;       // number of elements in queue

    public ArrayQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.buf = new double[capacity];
    }

    /** Adds to tail if not full; throws if full to catch logic errors early. */
    public void offer(double x) {
        if (size == buf.length) throw new IllegalStateException("queue full");
        buf[tail] = x;
        tail = (tail + 1) % buf.length;
        size++;
    }

    /** Removes and returns head; throws if empty. */
    public double poll() {
        if (size == 0) throw new IllegalStateException("queue empty");
        double x = buf[head];
        head = (head + 1) % buf.length;
        size--;
        return x;
    }

    /** Returns head without removing; throws if empty. */
    public double peek() {
        if (size == 0) throw new IllegalStateException("queue empty");
        return buf[head];
    }

    public int size()      { return size; }
    public int capacity()  { return buf.length; }
    public boolean isFull(){ return size == buf.length; }
    public boolean isEmpty(){ return size == 0; }

    /** Clears without reallocating. */
    public void clear() {
        head = tail = size = 0;
    }
}