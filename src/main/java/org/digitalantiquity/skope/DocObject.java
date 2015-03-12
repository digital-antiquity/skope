package org.digitalantiquity.skope;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

public class DocObject {

    public DocObject(Coordinate coord) {
        this.coord = coord;
    }

    private Coordinate coord;

    private List<Double> vals = new ArrayList<>();

    public List<Double> getVals() {
        return vals;
    }

    public void setVals(List<Double> vals) {
        this.vals = vals;
    }

    public Coordinate getCoord() {
        return coord;
    }

    public void setCoord(Coordinate coord) {
        this.coord = coord;
    }
}
