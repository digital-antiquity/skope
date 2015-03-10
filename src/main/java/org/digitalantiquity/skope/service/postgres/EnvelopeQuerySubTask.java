package org.digitalantiquity.skope.service.postgres;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.digitalantiquity.skope.service.DoubleResultSetExtractor;
import org.digitalantiquity.skope.service.FeatureHelper;
import org.geojson.Feature;
import org.postgis.Point;
import org.postgis.Polygon;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;

import com.github.davidmoten.geo.Coverage;
import com.github.davidmoten.geo.GeoHash;

public class EnvelopeQuerySubTask implements Runnable {
    private final Logger logger = Logger.getLogger(getClass());

    private PreparedStatementCreatorFactory pcsf;

    private JdbcTemplate jdbcTemplate;
    private Polygon poly;
    private Feature feature;
    private EnvelopeQueryTask task;

    private String sql;

    public EnvelopeQuerySubTask(Polygon poly, JdbcTemplate jdbcTemplate, EnvelopeQueryTask task, int year) {
        this.jdbcTemplate = jdbcTemplate;
        this.poly = poly;
        Point p1 = poly.getPoint(0);
        Point p2 = poly.getPoint(2);
        Coverage coverage = GeoHash.coverBoundingBoxMaxHashes(Math.max(p1.y, p2.y), Math.min(p1.x, p2.x), Math.min(p1.y, p2.y), Math.max(p1.x, p2.x), 40);

        this.task = task;
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String hash : coverage.getHashes()) {
            if (count != 0) {
                sb.append(",");
            }
            count++;
            sb.append("'").append(hash).append("'");
        }
        sql = "select avg(temp) from skopedata where year=" + year + " and hash in (" + sb.toString() + ")";
//        logger.debug(sql);
//        select avg(temp) from skopedata where year=0 and hash in ('9w4m1n','9w4m1p','9w4m1q','9w4m1r','9w4m1w','9w4m1x','9w4m1y','9w4m1z','9w4m30','9w4m31','9w4m32','9w4m33','9w4m34','9w4m35','9w4m36','9w4m37','9w4m38','9w4m39','9w4m3b','9w4m3c','9w4m3d','9w4m3e','9w4m3f','9w4m3g','9w4m3h','9w4m3j','9w4m3k','9w4m3m','9w4m3s','9w4m3t','9w4m3u','9w4m3v')
        pcsf = new PreparedStatementCreatorFactory(sql);
    }

    @Override
    public void run() {
        PreparedStatementCreator newPreparedStatementCreator = pcsf.newPreparedStatementCreator(Arrays.asList());

        Double avg = (Double) jdbcTemplate.query(newPreparedStatementCreator, new DoubleResultSetExtractor());
        if (avg != null && avg > 0)  {
            logger.trace("avg: " + avg + " | " + sql);
            
            Feature feature = FeatureHelper.createFeature(poly, avg);
            task.getFeatureCollection().add(feature);
        }
        setFeature(feature);
    }

    public Feature getFeature() {
        return feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

}
