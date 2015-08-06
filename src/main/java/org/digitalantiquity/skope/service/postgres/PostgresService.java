package org.digitalantiquity.skope.service.postgres;

import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.digitalantiquity.skope.service.BoundingBoxHelper;
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
public class PostgresService {

    private final Logger logger = Logger.getLogger(getClass());
    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate(JdbcTemplate template) {
        this.jdbcTemplate = template;
    }

    @Autowired
    private transient ThreadPoolTaskExecutor taskExecutor;

    @Transactional(readOnly = true)
    public FeatureCollection search(double x1, double y1, double x2, double y2, Integer year, Integer numCols,Integer zoom) throws SQLException {
        List<Polygon> boxes = BoundingBoxHelper.createBoundindBoxes(x2, y1, x1, y2, numCols);
        PgEnvelopeQueryTask task = new PgEnvelopeQueryTask();
        return task.run(taskExecutor, jdbcTemplate, boxes, year);

    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

//    @Autowired(required = true)
    @Lazy(true)
    public void setDataSource(@Qualifier("postgres") DataSource dataSource) {
        try {
            setJdbcTemplate(new JdbcTemplate(dataSource));
        } catch (Exception e) {
            logger.debug("exception in geosearch:", e);
        }
    }

}
