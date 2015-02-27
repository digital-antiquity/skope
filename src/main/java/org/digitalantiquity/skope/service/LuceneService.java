package org.digitalantiquity.skope.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.store.FSDirectory;
import org.geojson.FeatureCollection;
import org.postgis.Polygon;
import org.springframework.stereotype.Service;

import com.spatial4j.core.context.SpatialContext;

@Service
public class LuceneService {

    private final Logger logger = Logger.getLogger(getClass());
    SpatialContext ctx = SpatialContext.GEO;
    SpatialPrefixTree grid = new GeohashPrefixTree(ctx, 24);
    RecursivePrefixTreeStrategy strategy = new RecursivePrefixTreeStrategy(grid, "location");
    final IndexReader reader;
    final IndexSearcher searcher;

    public LuceneService() throws IOException {
        reader = DirectoryReader.open(FSDirectory.open(new File("indexes")));
        searcher = new IndexSearcher(reader);

    }

    /**
     * Attempts to perform the search via Lucene's Spatial Search feature
     * 
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param cols
     * @return
     * @throws IOException
     */
    public FeatureCollection searchUsingLuceneSpatial(double x1, double y1, double x2, double y2, int cols) throws IOException {
        FeatureCollection fc = new FeatureCollection();

        List<Polygon> boxes = BoundingBoxHelper.createBoundindBoxes(x1, y1, x2, y2, cols);
        for (Polygon poly : boxes) {

            SpatialArgs args = new SpatialArgs(SpatialOperation.IsWithin, ctx.makeRectangle(Math.min(x1, x2), Math.max(x1, x2), Math.min(y1, y2),
                    Math.max(y1, y2)));
            Filter filter = strategy.makeFilter(args);
            int limit = 1_000_000;
            TopDocs topDocs = searcher.search(new MatchAllDocsQuery(), filter, limit);
            logger.debug(topDocs.scoreDocs.length + " | " + poly);
            DoubleWrapper dw = new DoubleWrapper();
            for (int i = 0; i < topDocs.scoreDocs.length; i++) {
                Document document = reader.document(topDocs.scoreDocs[i].doc);
                dw.increment(Double.parseDouble(document.get(IndexFields.CODE)));
            }
            fc.add(FeatureHelper.createFeature(poly, dw.getAverage()));
        }
        return fc;
    }

    public FeatureCollection search(double x1, double y1, double x2, double y2, int year, int cols) throws IOException {
        FeatureCollection fc = new FeatureCollection();

        List<Polygon> boxes = BoundingBoxHelper.createBoundindBoxes(x1, y1, x2, y2, cols);
        Long q1 = Long.parseLong(QuadTreeHelper.toQuadTree(Math.min(x1, x2), Math.min(y1, y2)));
        Long q2 = Long.parseLong(QuadTreeHelper.toQuadTree(Math.max(x1, x2),Math.max(y1, y2)));
        Query quadRangeQuery = NumericRangeQuery.newLongRange(IndexFields.QUAD_, Math.min(q1, q2), Math.max(q1, q2), false, false);

        NumericRangeQuery<Integer> yearRange = NumericRangeQuery.newIntRange(IndexFields.YEAR, year, year, true, true);
        BooleanQuery bq = new BooleanQuery();
        bq.add(quadRangeQuery, Occur.MUST);
        bq.add(yearRange, Occur.MUST);
        TopDocs search = searcher.search(bq, null, 10000000);
        logger.debug(quadRangeQuery + " (" + search.totalHits + ")");
        Map<String, DoubleWrapper> valueMap = new HashMap<String, DoubleWrapper>();
        for (int i = 0; i < search.scoreDocs.length; i++) {
            Document document = reader.document(search.scoreDocs[i].doc);
            String key = document.get(IndexFields.QUAD_);
            DoubleWrapper double1 = valueMap.get(key);
            if (double1 == null) {
                double1 = new DoubleWrapper();
            }
            double1.increment(Double.parseDouble(document.get(IndexFields.CODE)));
            valueMap.put(key, double1);
            // logger.debug(document);
        }

        for (Polygon poly : boxes) {
            Long quadTree = Long.parseLong(QuadTreeHelper.toQuadTree(poly.getPoint(0).x, poly.getPoint(0).y));
            Long quadTree_ = Long.parseLong(QuadTreeHelper.toQuadTree(poly.getPoint(2).x, poly.getPoint(2).y));
            long min = Math.min(quadTree, quadTree_);
            long max = Math.max(quadTree, quadTree_);
            DoubleWrapper doubleWrapper = null;

            for (String key : valueMap.keySet()) {
                logger.trace(key + " - " + valueMap.get(key).getAverage());
                Long key_ = Long.parseLong(key);
                // logger.debug(key);

                // if we're between the two legs of the quadtree
                if (min < key_ && max > key_) {
                    if (doubleWrapper == null) {
                        doubleWrapper = new DoubleWrapper();
                    }
                    doubleWrapper.increment(valueMap.get(key).getAverage());
                }
            }

            if (doubleWrapper != null) {
                double avg = doubleWrapper.getAverage();
                logger.trace("adding " + avg + " for: " + poly);
                fc.add(FeatureHelper.createFeature(poly, avg));
            }
        }
        return fc;
    }

}
