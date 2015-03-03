package org.digitalantiquity.skope.service;

public class DoubleWrapper {

    private int count = 0;
    private double val = 0d;
    private double y;
    private double x;
    
    public DoubleWrapper() {}
    
    public DoubleWrapper(double x, double y) {
        this.setX(x);
        this.setY(y);
    }

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

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
