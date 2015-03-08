package org.digitalantiquity.skope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.digitalantiquity.skope.service.LuceneIndexingService;
import org.junit.Test;

import com.github.davidmoten.geo.Coverage;
import com.github.davidmoten.geo.GeoHash;
import com.github.davidmoten.geo.LatLong;

/**
 * 
 */
public class ShapefileParserTest {

    private final Logger logger = Logger.getLogger(getClass());


    @Test
    public void testHash() {
        double x1 = -126.91406249999999;
        double y1 = 13.923403897723347;
        double x2 = -74.1796875;
        double y2 = 55.37911044801047;
        
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
        LuceneIndexingService luceneService = new LuceneIndexingService();
        luceneService.indexGeoTiff();
    }

    @Test
    public void indexShapefile() throws Exception {
        LuceneIndexingService luceneService = new LuceneIndexingService();
        luceneService.indexShapefile();
    }
}
