package org.digitalantiquity.skope.service;

public class QuadTreeHelper {

    static final double NUM_LEVELS = 18;
    static final double NUM_TILES = 256;
    
    // http://wiki.openstreetmap.org/wiki/QuadTiles
    public static String toQuadTree(Double x1_, Double y1_) {
        // http://koti.mbnet.fi/ojalesa/quadtree/quadtree.js
        String toReturn = "";
        Double x1 = Math.floor(x1_ * Math.pow(2, 10) / NUM_TILES);
        Double y1 = Math.floor(y1_ * Math.pow(2, 10) / NUM_TILES);
        for (int i = (int) NUM_LEVELS; i > 0; i--) {
            int pow = 1 << (i - 1);
            int cell = 0;
            if ((x1.intValue() & pow) > 0) {
                cell++;
            }
            if ((y1.intValue() & pow) > 0) {
                cell += 2;
            }
            toReturn += cell;
        }
        return toReturn;
    }

    /**
     * Implementation in JS from koti.mbnet.fi
     * 
     * function(x, y, z){
     * var arr = [];
     * for(var i=z; i>0; i--) {
     * var pow = 1<<(i-1);
     * var cell = 0;
     * if ((x&pow) != 0) cell++;
     * if ((y&pow) != 0) cell+=2;
     * arr.push(cell);
     * }
     * return arr.join("");
     * }
     **/

}
