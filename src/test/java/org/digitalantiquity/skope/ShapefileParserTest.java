package org.digitalantiquity.skope;


import org.digitalantiquity.skope.service.LuceneService;
import org.junit.Test;

/**
 * 
 */
public class ShapefileParserTest {
    
//    @Test
    public void test() throws Exception {
        LuceneService luceneService = new LuceneService();
        luceneService.indexGeoTiff();
    }

    @Test
    public void indexShapefile() throws Exception {
        LuceneService luceneService = new LuceneService();
        luceneService.indexShapefile();
    }
}

