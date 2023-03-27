package com.gmail.woodyc40.pbft.type;

public class DESOperation {
    private final int first;
    private final int second;

    public DESOperation(int first, int second) {
        this.first = first;
        this.second = second;
    }

    public int first() {
        return this.first;
    }

    public int second() {
        return this.second;
    }
}
