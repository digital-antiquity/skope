package org.digitalantiquity.skope;


import org.digitalantiquity.skope.service.LuceneIndexingService;
import org.junit.Test;

/**
 * 
 */
public class ShapefileParserTest {
    
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

