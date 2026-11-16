package com.winters.boweilibrary.autopipeline.utils;

public class RingBuffer {
    private byte[] buffer; // 缓冲区
    private int capacity; // 当前缓冲区容量
    private int head;     // 写入指针
    private int tail;     // 读取指针
    private int size;     // 当前缓冲区中的数据量

    public RingBuffer(int initialCapacity) {
        this.capacity = initialCapacity;
        this.buffer = new byte[initialCapacity];
        this.head = 0;
        this.tail = 0;
        this.size = 0;
    }

    /**
     * 写入数据到缓冲区
     * @param data 要写入的数据
     * @return 是否成功写入
     */
    public synchronized boolean write(byte[] data) {
        if (data.length > capacity - size) {
            // 缓冲区不足，扩展缓冲区
            expandBuffer(data.length);
        }

        for (byte b : data) {
            buffer[head] = b;
            head = (head + 1) % capacity;
        }
        size += data.length;
        return true;
    }

    /**
     * 从缓冲区复制数据
     * @param length 要复制的数据长度
     * @return 复制的数据
     */
    public synchronized byte[] getDataPointer(int length) {
        if (length > size) {
            // 缓冲区中数据不足
            return null;
        }

        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = buffer[(tail + i) % capacity];
        }
        return result;
    }

    /**
     * 获取当前读取指针（tail）
     * @return 当前读取指针
     */
    public synchronized int getTail() {
        return tail;
    }

    /**
     * 更新读取指针，标记数据已处理，并自动扩展缓冲区（如果需要）
     * @param processedLength 已处理的数据长度
     */
    public synchronized void markProcessed(int processedLength) {
        if (processedLength > size) {
            throw new IllegalArgumentException("Processed length exceeds buffer size");
        }

        tail = (tail + processedLength) % capacity;
        size -= processedLength;

    }

    /**
     * 获取缓冲区中的数据量
     * @return 当前缓冲区中的数据量
     */
    public synchronized int getSize() {
        return size;
    }



    /**
     * 扩展缓冲区大小
     * @param additionalSpace 需要的额外空间
     */
    private void expandBuffer(int additionalSpace) {
        int newCapacity = capacity + Math.max(additionalSpace, capacity); // 扩展为当前容量的两倍或足够容纳额外数据
        byte[] newBuffer = new byte[newCapacity];

        // 将旧缓冲区的数据复制到新缓冲区
        for (int i = 0; i < size; i++) {
            newBuffer[i] = buffer[(tail + i) % capacity];
        }

        buffer = newBuffer;
        capacity = newCapacity;
        tail = 0;
        head = size;
    }

    /**
     * 检查缓冲区是否为空
     * @return 是否为空
     */
    public synchronized boolean isEmpty() {
        return size == 0;
    }
}