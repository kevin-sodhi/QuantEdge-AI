package com.kevin.algo.dsa;

import java.util.Arrays;

/**
 * DynamicArray<T>
 * ---------------
 * Minimal resizable array to practice arrays and amortized growth.
 * - pushBack: O(1) amortized
 * - get/set : O(1)
 * - size    : O(1)
 *
 * Notes:
 * - Backed by Object[]; we cast on get().
 * - Grow strategy: double capacity when full.
 */
public class DynamicArray<T> {
    private Object[] data;
    private int size; // number of valid elements (<= data.length)

    public DynamicArray() { this(8); } // default small capacity
    public DynamicArray(int capacity) {
        if (capacity <= 0) capacity = 1;
        this.data = new Object[capacity];
        this.size = 0;
    }

    /** Append to end; grows capacity if needed. */
    public void pushBack(T value) {
        ensureCapacity(size + 1);
        data[size++] = value;
    }

    /** Returns element at index (0..size-1). */
    @SuppressWarnings("unchecked")
    public T get(int index) {
        rangeCheck(index);
        return (T) data[index];
    }

    /** Overwrites element at index. */
    public void set(int index, T value) {
        rangeCheck(index);
        data[index] = value;
    }

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    /** Ensure we can fit at least 'minCapacity' elements. */
    private void ensureCapacity(int minCapacity) {
        if (minCapacity <= data.length) return;
        int newCap = Math.max(data.length << 1, minCapacity); // x2 growth
        data = Arrays.copyOf(data, newCap);
    }

    private void rangeCheck(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        }
    }
}