package org.digitalantiquity.skope.service;

import java.util.ArrayList;
import java.util.List;

import org.geojson.Feature;
import org.geojson.LngLatAlt;
import org.postgis.Polygon;

public class FeatureHelper {

    private static final String TEMP = "temp";

    public static Feature createFeature(Polygon poly, Double avg) {
        Feature feature = new Feature();
        org.geojson.Polygon geometry = new org.geojson.Polygon();
        List<LngLatAlt> points = new ArrayList<>();
        for (int i = 0; i < poly.numPoints(); i++) {
            points.add(new LngLatAlt(poly.getPoint(i).x, poly.getPoint(i).y));
        }
        geometry.add(points);
        feature.setGeometry(geometry);
        feature.setProperty(TEMP, avg * 9d / 5d + 32d);
        return feature;
    }

}
