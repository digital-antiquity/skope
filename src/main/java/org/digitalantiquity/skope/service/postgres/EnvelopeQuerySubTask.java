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
