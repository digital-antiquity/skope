package org.digitalantiquity.skope.service;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.geojson.Feature;
import org.geojson.LngLatAlt;
import org.postgis.Polygon;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;

public class EnvelopeQuerySubTask implements Runnable {
    String sql = "select avg(grid_code) from prism where ST_makeEnvelope(?, ?,?,?,4326) && geom";
    private final Logger logger = Logger.getLogger(getClass());

    private PreparedStatementCreatorFactory pcsf;

    private JdbcTemplate jdbcTemplate;
    private Polygon poly;
    private Feature feature;
    private EnvelopeQueryTask task;

    public EnvelopeQuerySubTask(Polygon poly, JdbcTemplate jdbcTemplate, EnvelopeQueryTask task) {
        this.jdbcTemplate = jdbcTemplate;
        this.poly = poly;
        this.task = task;
        pcsf = new PreparedStatementCreatorFactory(sql, Types.DOUBLE, Types.DOUBLE, Types.DOUBLE, Types.DOUBLE);
    }

    @Override
    public void run() {
        PreparedStatementCreator newPreparedStatementCreator = pcsf.newPreparedStatementCreator(Arrays.asList(poly.getPoint(0).x, poly.getPoint(0).y,poly.getPoint(2).x, poly.getPoint(2).y));

        Double avg = (Double) jdbcTemplate.query(newPreparedStatementCreator, new DoubleResultSetExtractor());
        logger.trace("avg: " + avg + " | ");
        Feature feature = FeatureHelper.createFeature(poly, avg);
        task.getFeatureCollection().add(feature);
        setFeature(feature);
    }

    
    
    public Feature getFeature() {
        return feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

}