package org.digitalantiquity.skope.service;

import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.digitalantiquity.skope.service.postgis.EnvelopeQueryTask;
import org.geojson.FeatureCollection;
import org.postgis.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Transactional(readOnly = true)
    public FeatureCollection search(Double x1, Double y1, Double x2, double y2, Integer numCols) throws SQLException {

        List<Polygon> createBoundindBoxes = BoundingBoxHelper.createBoundindBoxes(x1, y1, x2, y2, numCols);
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
