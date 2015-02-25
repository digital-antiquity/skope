package org.digitalantiquity.skope.service;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.geojson.FeatureCollection;
import org.postgis.LinearRing;
import org.postgis.Point;
import org.postgis.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostGisService {

    private JdbcTemplate jdbcTemplate;
    private final Logger logger = Logger.getLogger(getClass());

    public void setJdbcTemplate(JdbcTemplate template) {
        this.jdbcTemplate = template;
    }

    @Autowired
    private transient ThreadPoolTaskExecutor taskExecutor;

    public Polygon createBox(Double x1, Double y1, Double x2, Double y2) {
        Polygon geo = new Polygon(
                new LinearRing[] {
                        new LinearRing(
                                new Point[] { new Point(x1, y1), new Point(x2, y1), new Point(x2, y2), new Point(x1, y2), new Point(x1, y1)
                                }
                        ) }
                );
        return geo;
    }

    public List<Polygon> createBoundindBoxes(Double x1, Double y1, Double x2, Double y2, int cols) {
        Double x = (Math.max(x1, x2) - Math.min(x1, x2)) / (double) cols;
        Double y = (Math.max(y1, y2) - Math.min(y1, y2)) / (double) cols;
        List<Polygon> polys = new ArrayList<>();
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < cols; j++) {
                double id = (double) i;
                double id2 = (double) i + 1d;
                double jd = (double) j;
                double jd2 = (double) j + 1d;
                logger.trace("i: " + i + " x1: " + (x1 - x * id) + " x2:" + (x1 - x * id2));
                logger.trace("i: " + j + " y1: " + (y1 + y * jd) + " y2:" + (y1 + y * jd2));
                polys.add(createBox(x1 - x * id, y1 + y * jd, x1 - x * id2, y1 + y * jd2));
            }
        }
        return polys;
    }

    @Transactional(readOnly = true)
    public FeatureCollection test(Double x1, Double y1, Double x2, double y2, Integer numCols) throws SQLException {


        List<Polygon> createBoundindBoxes = createBoundindBoxes(x1, y1, x2, y2, numCols);
        EnvelopeQueryTask task = new EnvelopeQueryTask();
        return task.run(taskExecutor, jdbcTemplate, createBoundindBoxes);

    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @Autowired(required = true)
    @Lazy(true)
    public void setDataSource(@Qualifier("postgis") DataSource dataSource) {
        try {
            setJdbcTemplate(new JdbcTemplate(dataSource));
        } catch (Exception e) {
            logger.debug("exception in geosearch:", e);
        }
    }
    
    
    

}
