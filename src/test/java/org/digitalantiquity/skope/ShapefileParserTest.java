package org.digitalantiquity.skope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.digitalantiquity.skope.service.IndexingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.davidmoten.geo.Coverage;
import com.github.davidmoten.geo.GeoHash;
import com.github.davidmoten.geo.LatLong;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * 
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration(locations={
    "classpath*:**applicationContext.xml"
})
public class ShapefileParserTest extends AbstractTransactionalJUnit4SpringContextTests {

    private final Logger logger = Logger.getLogger(getClass());


    private JdbcTemplate jdbcTemplate;
    public void setJdbcTemplate(JdbcTemplate template) {
        this.jdbcTemplate = template;
    }

    @Autowired(required = true)
    @Lazy(true)
    public void setDataSource(@Qualifier("postgres") DataSource dataSource) {
        logger.debug("DataSource:" + dataSource);
        try {
            setJdbcTemplate(new JdbcTemplate(dataSource));
        } catch (Exception e) {
            logger.debug("exception in geosearch:", e);
        }
        logger.debug("template:"+ jdbcTemplate);
    }

    
    @Value("${rootDir:#{'../dataDir/'}}")
    private String rootDir;

    @Test
    public void testHash() {
        double x1 = -126.91406249999999;
        double y1 = 13.923403897723347;
        double x2 = -74.1796875;
        double y2 = 55.37911044801047;
        
        Coordinate coord = new Coordinate(x1, y1);
        logger.debug(coord.hashCode());
        Coordinate coord2 = new Coordinate(x1, y1);
        logger.debug(coord2.hashCode());

        
        logger.debug(String.format("start (%s,%s) x(%s,%s)", x1, y1, x2, y2));
        String point = "9w69jps00000";
        LatLong latLong = GeoHash.decodeHash(point);
        logger.debug(latLong);
        String hash2 = GeoHash.encodeHash(35.37322998046875, -108.7591552734375);
        assertEquals(hash2, point);
        logger.debug(hash2);
        Coverage coverage = GeoHash.coverBoundingBox(y2,x1,y1,x2,4);
        boolean seen = false;
        logger.debug(coverage);
        for (String hash : coverage.getHashes()) {
            if (hash.equals(point) || point.startsWith(hash)) {
                logger.debug(hash + " -> " + GeoHash.decodeHash(hash));
                seen = true;
            }
        }
        assertTrue("should have seen hash",seen);
    }

    @Test
    public void indexGeoTiff() throws Exception {
        logger.debug(rootDir);
        IndexingService luceneService = new IndexingService();
        logger.debug(jdbcTemplate);
        luceneService.indexGeoTiff(rootDir,jdbcTemplate);
    }

}
