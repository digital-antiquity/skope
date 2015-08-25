package org.digitalantiquity.skope.service.lucene;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.shape.Rectangle;

@Service
public class LuceneService {

    private static final int MAX_RESULTS_LIMIT = 1_000_000;

    private static final double METRE_DECIMAL_LAT = 0.00001;

    @Autowired
    private transient ThreadPoolTaskExecutor taskExecutor;

    @Value("${indexDir:#{'indexes/'}}")
    private String indexDir;

    @Value("${dataDir:#{'.'}}")
    private String dataDir = "";

    private final Logger logger = Logger.getLogger(getClass());
    SpatialContext ctx = SpatialContext.GEO;
    SpatialPrefixTree grid = new GeohashPrefixTree(ctx, 24);
    RecursivePrefixTreeStrategy strategy = new RecursivePrefixTreeStrategy(grid, "location");
    private IndexReader reader;
    private IndexSearcher searcher;

    void setupReaders(String indexName) throws IOException {
        setReader(DirectoryReader.open(FSDirectory.open(new File(getIndexDir() + indexName))));
        setSearcher(new IndexSearcher(getReader()));
    }

    public Map<String, String[]> getDetails(double y, double x) {
        Rectangle rectangle = createRectangle(y, x);
        Map<String, String[]> results = new HashMap<>();
        try {
            setupReaders("skope");
            SpatialArgs args = new SpatialArgs(SpatialOperation.IsWithin, rectangle);
            Filter filter = strategy.makeFilter(args);
            Map<String, List<String>> ret = doQuery(filter, MAX_RESULTS_LIMIT);
            for (String key : ret.keySet()) {
                for (String val : ret.get(key)) {
                    File file = new File(dataDir, val);
                    logger.debug(file);
                    try {
                        List<String> lines = IOUtils.readLines(new FileReader(file));
                        results.put(key, lines.get(0).split("\\|"));
                    } catch (Exception e) {
                        System.err.println(file);
                    }
                }
            }
        } catch (Exception e) {
            // logger.error(e, e);
        }
        return results;
    }

    private Rectangle createRectangle(double y, double x) {
        Double xMax = 400 * METRE_DECIMAL_LAT + x;
        Double yMax = 400 * METRE_DECIMAL_LAT + y;
        Double xMin = x - 400 * METRE_DECIMAL_LAT;
        Double yMin = y - 400 * METRE_DECIMAL_LAT;
        Rectangle rectangle = ctx.makeRectangle(yMin, yMax, xMin, xMax);
        return rectangle;
    }

    private Map<String, List<String>> doQuery(Filter filter, int limit) throws IOException {
        // TermQuery tq = new TermQuery(new Term(IndexFields.TYPE, type));
        TopDocs topDocs = getSearcher().search(new MatchAllDocsQuery(), filter, limit);// , new Sort(new SortField(IndexFields.YEAR,Type.INT)));
        Map<String, List<String>> results = new HashMap<>();

        logger.debug("getting records from lucene: " + topDocs.scoreDocs.length);
        for (int i = 0; i < topDocs.scoreDocs.length; i++) {
            Document document = getReader().document(topDocs.scoreDocs[i].doc);
            String key = document.get(IndexFields.TYPE);
            if (!results.containsKey(key)) {
                results.put(key, new ArrayList<>());
            }
            results.get(key).add(document.get(IndexFields.VAL));
        }
        return results;
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

    public File exportData(Double y, Double x, Integer startTime, Integer endTime, List<String> type) throws IOException {
        File outFile = File.createTempFile("skope-csv-export", "csv");
        try {
            FileWriter fwriter = new FileWriter(outFile);
            List<String> labels = new ArrayList<>();
            labels.add(0, "Year");
            for (int i=0; i< type.size(); i++) {
                labels.add(type.get(i));
            }
            CSVPrinter printer = CSVFormat.EXCEL.withHeader(labels.toArray(new String[0])).print(fwriter);

            setupReaders("skope");
            Rectangle rectangle = createRectangle(y, x);

            SpatialArgs args = new SpatialArgs(SpatialOperation.IsWithin, rectangle);
            Filter filter = strategy.makeFilter(args);

            Map<String, List<String>> ret = doQuery(filter, MAX_RESULTS_LIMIT);
            Map<String,String[]> vals = new HashMap<>();
            for (String key : ret.keySet()) {
                for (String val : ret.get(key)) {
                    File file = new File(dataDir, val);
                    logger.debug(file);
                    try {
                        List<String> lines = IOUtils.readLines(new FileReader(file));
                        vals.put(val, lines.get(0).split("\\|"));
                    } catch (Exception e) {
                        logger.error(e, e);
                    }
                }
            }
            
            for (int t = startTime; t <= endTime; t++) {
                
                List<Object> row = new ArrayList<>();
                row.add(t);
                for (int i =0; i< type.size(); i++) {
                    try {
                    row.add(vals.get(type.get(i))[t]);
                    } catch (Exception e) {
                        row.add(null);
                    }
                }
                printer.printRecord(row);
            }

            printer.close();
        } catch (Exception e) {
            logger.error("exception in processing export", e);
        }
        return outFile;
    }

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public String getIndexDir() {
        return indexDir;
    }

    public void setIndexDir(String indexDir) {
        this.indexDir = indexDir;
    }
}
