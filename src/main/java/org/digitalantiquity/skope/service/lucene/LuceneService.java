package org.digitalantiquity.skope.service.lucene;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.store.FSDirectory;
import org.digitalantiquity.skope.service.BoundingBoxHelper;
import org.digitalantiquity.skope.service.DoubleWrapper;
import org.digitalantiquity.skope.service.FeatureHelper;
import org.digitalantiquity.skope.service.IndexFields;
import org.digitalantiquity.skope.service.QuadTreeHelper;
import org.geojson.FeatureCollection;
import org.postgis.Point;
import org.postgis.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.shape.Rectangle;
import com.spatial4j.core.shape.SpatialRelation;

@Service
public class LuceneService {

    private static final double METRE_DECIMAL_LAT = 0.00001;

    @Autowired
    private transient ThreadPoolTaskExecutor taskExecutor;

    private final Logger logger = Logger.getLogger(getClass());
    SpatialContext ctx = SpatialContext.GEO;
    SpatialPrefixTree grid = new GeohashPrefixTree(ctx, 24);
    RecursivePrefixTreeStrategy strategy = new RecursivePrefixTreeStrategy(grid, "location");
    private IndexReader reader;
    private IndexSearcher searcher;

    void setupReaders(String indexName) throws IOException {
        setReader(DirectoryReader.open(FSDirectory.open(new File("indexes/" + indexName))));
        setSearcher(new IndexSearcher(getReader()));
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
     * @throws ParseException
     */
    public FeatureCollection searchUsingLuceneSpatial(String name, double x1, double y1, double x2, double y2, int year, int cols, int level) throws Exception {
        FeatureCollection fc = new FeatureCollection();
        setupReaders(name);
        List<Polygon> boxes = BoundingBoxHelper.createBoundindBoxes(x1, y1, x2, y2, cols);
        for (Polygon poly : boxes) {

            Point p1 = poly.getPoint(0);
            Point p2 = poly.getPoint(2);
            // Math.min(p1.x, p2.x), Math.max(p1.x, p2.x), Math.min(p1.y, p2.y), Math.max(p1.y, p2.y)
            Rectangle rectangle = ctx.makeRectangle(Math.min(p1.x, p2.x), Math.max(p1.x, p2.x), Math.min(p1.y, p2.y), Math.max(p1.y, p2.y));

            // Rectangle rectangle = ctx.makeRectangle(p1.x,p2.x, p1.y,p2.y);

            SpatialArgs args = new SpatialArgs(SpatialOperation.IsWithin, rectangle);
            Filter filter = strategy.makeFilter(args);
            int limit = 1_000_000;
            String quadTree = QuadTreeHelper.toQuadTree(Math.min(p1.x, p2.x), Math.min(p1.x, p1.y));
            String quadTree2 = QuadTreeHelper.toQuadTree(Math.max(p1.x, p2.x), Math.max(p1.x, p2.y));
            Long q1 = Long.parseLong(quadTree);
            Long q2 = Long.parseLong(quadTree2);
            QueryParser parser = new QueryParser(IndexFields.QUAD, new StandardAnalyzer());
            Query quadField = parser.parse(IndexFields.QUAD + ":[" + quadTree + " TO " + quadTree2 + "] ");
            if (q1 > q2) {
                quadField = parser.parse(IndexFields.QUAD + ":[" + quadTree2 + " TO " + quadTree + "] ");
            }
            NumericRangeQuery<Integer> yearRange = NumericRangeQuery.newIntRange(IndexFields.YEAR, year, year, true, true);
            BooleanQuery bq = new BooleanQuery();
            bq.add(quadField, Occur.MUST);
            bq.add(yearRange, Occur.MUST);
            TopDocs topDocs = getSearcher().search(bq, filter, limit);
            if (topDocs.scoreDocs.length == 0) {
                continue;
            }
            logger.debug(topDocs.scoreDocs.length + " | " + poly);
            DoubleWrapper dw = new DoubleWrapper();
            for (int i = 0; i < topDocs.scoreDocs.length; i++) {
                Document document = getReader().document(topDocs.scoreDocs[i].doc);
                dw.increment(Double.parseDouble(document.get(IndexFields.CODE)));
            }
            if (dw.getCount() > 0) {
                fc.add(FeatureHelper.createFeature(poly, dw.getAverage()));
            }
        }
        return fc;
    }

    public List<Double> getDetails(double y, double x) {
        Double xMax = 400 * METRE_DECIMAL_LAT + x;
        Double yMax = 400 * METRE_DECIMAL_LAT + y;
        Double xMin = x - 400 * METRE_DECIMAL_LAT;
        Double yMin = y - 400 * METRE_DECIMAL_LAT;
        Rectangle rectangle = ctx.makeRectangle(yMin, yMax, xMin, xMax);
        List<Double> toReturn = new ArrayList<>();
        try {
            setupReaders("skope");
            SpatialArgs args = new SpatialArgs(SpatialOperation.IsWithin, rectangle);
            logger.debug(args);
            Filter filter = strategy.makeFilter(args);
            int limit = 1_000_000;

            TopDocs topDocs = getSearcher().search(new MatchAllDocsQuery(), filter, limit, new Sort(new SortField(IndexFields.YEAR,Type.INT)));

             logger.debug(topDocs.scoreDocs.length);
            for (int i = 0; i < topDocs.scoreDocs.length; i++) {
                Document document = getReader().document(topDocs.scoreDocs[i].doc);
                toReturn.add(Double.parseDouble(document.get(IndexFields.CODE)));
            }
        } catch (Exception e) {
            logger.error(e, e);
        }
        return toReturn;
    }

    public FeatureCollection search(String name, double x1, double y1, double x2, double y2, int year, int cols, int level) throws Exception {
        setupReaders(name);

        return geoHash1(x1, y1, x2, y2, year, cols, level);

    }

    private FeatureCollection geoHash1(double x1, double y1, double x2, double y2, int year, int cols, int level) throws IOException {
        List<Polygon> boxes = BoundingBoxHelper.createBoundindBoxes(x2, y1, x1, y2, cols);

        LuceneEnvelopeQueryTask task = new LuceneEnvelopeQueryTask();
        return task.run(taskExecutor, boxes, this, level, year);
    }

    public BooleanQuery buildLuceneQuery(int level, Set<String> coverage, int year, boolean wildcard) {
        BooleanQuery bq = new BooleanQuery();
        NumericRangeQuery<Integer> yearRange = NumericRangeQuery.newIntRange(IndexFields.YEAR, year, year, true, true);
        bq.add(yearRange, Occur.MUST);
        Map<Integer, List<String>> keyLengthMap = new HashMap<Integer, List<String>>();
        for (String cov : coverage) {
            int len = cov.length();
            List<String> keys = keyLengthMap.get(len);
            if (keys == null) {
                keys = new ArrayList<String>();
                keyLengthMap.put(len, keys);
            }
            keys.add(cov);
        }
        if (level <= 6) {
            // bq.add(NumericRangeQuery.newIntRange(IndexFields.LEVEL, 4, 4, true, true), Occur.MUST);
        } else if (level <= 10) {
            // bq.add(NumericRangeQuery.newIntRange(IndexFields.LEVEL, 6, 6, true, true), Occur.MUST);
        } else {
            // wildcard = true;
            // bq.add(NumericRangeQuery.newIntRange(IndexFields.LEVEL, 6, 6, true, true), Occur.MUST);
        }
        BooleanQuery bqs = new BooleanQuery();

        for (Integer len : keyLengthMap.keySet()) {
            BooleanQuery bqqs = new BooleanQuery();
            bqqs.add(NumericRangeQuery.newIntRange(IndexFields.LEVEL, len, len, true, true), Occur.MUST);
            BooleanQuery bqqqs = new BooleanQuery();
            for (String hash : keyLengthMap.get(len)) {
                String text = hash;
                if (wildcard) {
                    text += "*";
                }
                Query tq = new WildcardQuery(new Term(IndexFields.HASH, text));
                bqqqs.add(tq, Occur.SHOULD);
            }
            bqqs.add(bqqqs, Occur.MUST);
            bqs.add(bqqs, Occur.SHOULD);
        }
        bq.add(bqs, Occur.MUST);
        return bq;
    }

    private void traditionalLucene(double x1, double y1, double x2, double y2, int year, int cols, FeatureCollection fc) throws IOException {
        List<Polygon> boxes = BoundingBoxHelper.createBoundindBoxes(x1, y1, x2, y2, cols);
        String quadTree = QuadTreeHelper.toQuadTree(Math.min(x1, x2), Math.min(y1, y2));
        String quadTree2 = QuadTreeHelper.toQuadTree(Math.max(x1, x2), Math.max(y1, y2));
        Long q1 = Long.parseLong(quadTree);
        Long q2 = Long.parseLong(quadTree2);
        Query quadRangeQuery = NumericRangeQuery.newLongRange(IndexFields.QUAD_, Math.min(q1, q2), Math.max(q1, q2), false, false);
        logger.debug("q:" + q1 + " <->" + q2);
        NumericRangeQuery<Integer> yearRange = NumericRangeQuery.newIntRange(IndexFields.YEAR, year, year, true, true);
        BooleanQuery bq = new BooleanQuery();
        bq.add(quadRangeQuery, Occur.MUST);
        bq.add(yearRange, Occur.MUST);
        TopDocs search = getSearcher().search(bq, null, 10000000);
        logger.debug(quadRangeQuery + " (" + search.totalHits + ")");

        java.util.Collections.sort(boxes, new Comparator<Polygon>() {

            @Override
            public int compare(Polygon o1, Polygon o2) {
                if (o1.getPoint(0).x < o2.getPoint(0).x) {
                    return 0;
                }
                return 1;
            }

        });
        for (Polygon poly : boxes) {
            Point p1 = poly.getPoint(0);
            Point p2 = poly.getPoint(2);
            Rectangle rectangle = ctx.makeRectangle(Math.min(p1.x, p2.x), Math.max(p1.x, p2.x), Math.min(p1.y, p2.y), Math.max(p1.y, p2.y));

            DoubleWrapper doubleWrapper = null;

            for (int i = 0; i < search.scoreDocs.length; i++) {
                Document document = getReader().document(search.scoreDocs[i].doc);
                String key = document.get(IndexFields.QUAD_);
                Double x = Double.parseDouble(document.get(IndexFields.X));
                Double y = Double.parseDouble(document.get(IndexFields.Y));
                com.spatial4j.core.shape.Point pt = ctx.makePoint(x, y);
                if (rectangle.relate(pt) == SpatialRelation.CONTAINS) {

                    if (doubleWrapper == null) {
                        doubleWrapper = new DoubleWrapper();
                    }
                    // logger.debug(key);
                    doubleWrapper.increment(Double.parseDouble(document.get(IndexFields.CODE)));
                }
            }
            Double avg = null;
            if (doubleWrapper != null) {
                avg = doubleWrapper.getAverage();
                logger.trace("adding " + avg + " for: " + poly);
                fc.add(FeatureHelper.createFeature(poly, avg));
            }

        }
    }

    public IndexSearcher getSearcher() {
        return searcher;
    }

    public void setSearcher(IndexSearcher searcher) {
        this.searcher = searcher;
    }

    public IndexReader getReader() {
        return reader;
    }

    public void setReader(IndexReader reader) {
        this.reader = reader;
    }
}