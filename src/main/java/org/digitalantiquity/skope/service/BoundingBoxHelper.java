package org.digitalantiquity.skope.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.postgis.LinearRing;
import org.postgis.Point;
import org.postgis.Polygon;

public class BoundingBoxHelper {
    private static final Logger logger = Logger.getLogger(BoundingBoxHelper.class);

    public static Polygon createBox(Double x1, Double y1, Double x2, Double y2) {
        logger.trace(String.format("(%s,%s) (%s,%s)", x1,y1,x2,y2));
        Polygon geo = new Polygon(
                new LinearRing[] {
                        new LinearRing(
                                new Point[] { new Point(x1, y1), new Point(x2, y1), new Point(x2, y2), new Point(x1, y2), new Point(x1, y1)
                                }
                        ) }
                );
        return geo;
    }

    public static List<Polygon> createBoundindBoxes(Double x1, Double y1, Double x2, double y2, Integer cols) {
        Double x = (Math.max(x1, x2) - Math.min(x1, x2)) / (double) cols;
        Double y = (Math.max(y1, y2) - Math.min(y1, y2)) / (double) cols;
        
        Double xi = Math.max(x1, x2);
        Double yi = Math.min(y1, y2);
        List<Polygon> polys = new ArrayList<>();
        for (int i = 0; i < cols; i++) {
            double id = (double) i;
            double id2 = (double) i + 1d;

            for (int j = 0; j < cols; j++) {
                double jd = (double) j;
                double jd2 = (double) j + 1d;
                logger.trace("i: " + i + " x1: " + (x1 - x * id) + " x2:" + (x1 - x * id2));
                logger.trace("i: " + j + " y1: " + (y1 + y * jd) + " y2:" + (y1 + y * jd2));
                polys.add(createBox(xi - x * id, yi + y * jd, xi - x * id2, yi + y * jd2));
            }
        }
        return polys;
    }

}
