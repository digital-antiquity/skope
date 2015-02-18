package org.digitalantiquity.skope.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.postgis.LinearRing;
import org.postgis.Point;
import org.postgis.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostGisService {

    private JdbcTemplate jdbcTemplate;
    private final Logger logger = Logger.getLogger(getClass());

    public void setJdbcTemplate(JdbcTemplate template) {
        this.jdbcTemplate = template;
    }

    public Polygon createBox(Double x1, Double y1, Double x2, Double y2) {
        Polygon geo = new Polygon(
                new LinearRing[] {
                        new LinearRing(
                                new Point[] {
                                        new Point(x1, y1),
                                        new Point(x2, y1),
                                        new Point(x2, y2),
                                        new Point(x1, y2),
                                        new Point(x1, y1)
                                }
                        ) }
                );
        return geo;
    }
    
    public List<Polygon> createBoundindBoxes(Double x1, Double y1, Double x2, Double y2, int cols) {
        Double x = (Math.max(x1, x2) - Math.min(x1, x2)) / (double)cols;
        Double y = (Math.max(y1, y2) - Math.min(y1, y2)) / (double)cols;
        List<Polygon> polys = new ArrayList<>();
        for (int i =0 ; i< cols; i++) {
            double id = (double)i;
            double id2 = (double)i +1d;
            logger.debug("i: " + i + " x1: "+ (x1 - x*id) + " x2:" + (x1 - x* id2));
            logger.debug("i: " + i + " y1: "+ (y1 + y*id) + " y2:" + (y1 + y* id2));
            polys.add(createBox(x1 - x*id, y1 + y*id, x1 - x* id2, y1 + y * id2));
        }
        return polys;
    }

    @Transactional(readOnly = true)
    public void test(Double x1, Double y1, Double x2, double y2) throws SQLException {

        String sql = "select avg(grid_code) from prism where st_contains(ST_geomFromText(?,4326), geom)";
        List<Polygon> createBoundindBoxes = createBoundindBoxes(x1, y1, x2, y2, 50);
        logger.debug("polys: " + createBoundindBoxes);
        PreparedStatementCreatorFactory pcsf = new PreparedStatementCreatorFactory(sql, Types.VARCHAR);
        
//        PGConnection pgConnection = (PGConnection)jdbcTemplate.getDataSource().getConnection();
//        pgConnection.addDataType("geometry", PGgeometry.class);
//        pgConnection.addDataType("box2d", PGbox2d.class);
        for (Polygon poly : createBoundindBoxes) {
            ResultSetExtractor rse = new ResultSetExtractor<Double>() {

                @Override
                public Double extractData(ResultSet rs) throws SQLException, DataAccessException {
                    rs.next();
                    return rs.getDouble(1);
                }
            };
            Double avg = (Double)jdbcTemplate.query(pcsf.newPreparedStatementCreator(Arrays.asList(poly.toString())), rse);
            logger.debug("avg: " + avg + " | " + poly.toString());
        }
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
