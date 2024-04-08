package com.conney.keeptriple.local.util;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LoopChooser<T> {

    private AtomicInteger idx = new AtomicInteger(0);

    private T[] array;

    public T choose() {
        if (array.length > 0) {
            return array[next(array.length)];
        } else {
            return null;
        }
    }

    public <T> T choose(T[] array) {
        if (array.length > 0) {
            return array[next(array.length)];
        } else {
            return null;
        }
    }

    public <T> T choose(List<T> list) {
        if (list.size() > 0) {
            return list.get(next(list.size()));
        } else {
            return null;
        }
    }

    private int next(int length) {
        return idx.updateAndGet(operand -> ++operand % length);
    }


    public T[] getArray() {
        return array;
    }

    public void setArray(T[] array) {
        this.array = array;
    }
}
