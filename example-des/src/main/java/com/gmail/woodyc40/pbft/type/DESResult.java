package com.gmail.woodyc40.pbft.type;

public class DESResult {
    private final int result;

    public DESResult(int result) {
        this.result = result;
    }

    public int result() {
        return this.result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DESResult)) return false;

        DESResult that = (DESResult) o;

        return this.result == that.result;
    }

    @Override
    public int hashCode() {
        return this.result;
    }
}
