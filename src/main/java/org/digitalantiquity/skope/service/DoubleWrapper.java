package org.digitalantiquity.skope.service;

public class DoubleWrapper {

    private int count = 0;
    private double val = 0d;
    
    public void increment(double val) {
        count++;
        this.val += val;
    }
    
    public double getVal() {
        return val;
    }
    public void setVal(double val) {
        this.val = val;
    }
    public int getCount() {
        return count;
    }
    public void setCount(int count) {
        this.count = count;
    }

    public double getAverage() {
        return val / (double)count;
    }
}
