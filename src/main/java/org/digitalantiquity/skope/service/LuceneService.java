package org.digitalantiquity.skope.service;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.MathUtil;
import org.apache.lucene.util.Version;
import org.geojson.FeatureCollection;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.feature.FeatureIterator;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Envelope2D;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.postgis.Polygon;
import org.springframework.stereotype.Service;

import com.spatial4j.core.context.SpatialContext;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

@Service
public class LuceneService {

    private static final String YEAR = "year";
    private static final String GRID_CODE = "GRID_CODE";
    private static final double NUM_LEVELS = 18;
    private static final double NUM_TILES = 256;
    private static final String X = "x";
    private static final String Y = "y";
    private static final String CODE = "code";
    private static final String QUAD = "quad";
    private static final String QUAD_ = QUAD + "_";
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
                dw.increment(Double.parseDouble(document.get(CODE)));
            }
            fc.add(FeatureHelper.createFeature(poly, dw.getAverage()));
        }
        return fc;
    }

    public FeatureCollection search(double x1, double y1, double x2, double y2, int year, int cols) throws IOException {
        FeatureCollection fc = new FeatureCollection();

        List<Polygon> boxes = BoundingBoxHelper.createBoundindBoxes(x1, y1, x2, y2, cols);
        Long q1 = Long.parseLong(toQuadTree(x1, y1, NUM_LEVELS));
        Long q2 = Long.parseLong(toQuadTree(x2, y2, NUM_LEVELS));
        Query quadRangeQuery = NumericRangeQuery.newLongRange(QUAD_, Math.min(q1, q2), Math.max(q1, q2), true, true);

        NumericRangeQuery<Integer> yearRange = NumericRangeQuery.newIntRange(YEAR, year, year, true, true);
        BooleanQuery bq = new BooleanQuery();
        bq.add(quadRangeQuery, Occur.MUST);
        bq.add(yearRange, Occur.MUST);
        TopDocs search = searcher.search(bq, null, 10000000);
        logger.debug(quadRangeQuery + " (" + search.totalHits + ")");
        Map<String, DoubleWrapper> valueMap = new HashMap<String, DoubleWrapper>();
        for (int i = 0; i < search.scoreDocs.length; i++) {
            Document document = reader.document(search.scoreDocs[i].doc);
            String key = document.get(QUAD_);
            DoubleWrapper double1 = valueMap.get(key);
            if (double1 == null) {
                double1 = new DoubleWrapper();
            }
            double1.increment(Double.parseDouble(document.get(CODE)));
            valueMap.put(key, double1);
            // logger.debug(document);
        }

        Map<Integer, DoubleWrapper> polymap = new HashMap<Integer, DoubleWrapper>();
        for (String key : valueMap.keySet()) {
            logger.trace(key + " - " + valueMap.get(key).getAverage());
            Long key_ = Long.parseLong(key);
            // logger.debug(key);
            boolean seen = false;
            for (int i =0; i< boxes.size(); i++) {
                Polygon poly = boxes.get(i);
                Long quadTree = Long.parseLong(toQuadTree(poly.getPoint(0).x, poly.getPoint(0).y, NUM_LEVELS));
                Long quadTree_ = Long.parseLong(toQuadTree(poly.getPoint(2).x, poly.getPoint(2).y, NUM_LEVELS));

                // if we're between the two legs of the quadtree
                if (Math.min(quadTree, quadTree_) < key_ && Math.max(quadTree, quadTree_) > key_) {
                    DoubleWrapper doubleWrapper = polymap.get(i);
                    if (doubleWrapper == null) {
                        doubleWrapper = new DoubleWrapper();
                    }
                    doubleWrapper.increment(valueMap.get(key).getAverage());
                    polymap.put(i, doubleWrapper);
                    seen = true;
                }
                if (seen) {
                    continue;
                }
            }
            if (seen) {
                continue;
            }
        }
        for (int i=0; i< boxes.size(); i++) {
            Polygon poly = boxes.get(i);
            DoubleWrapper doubleWrapper = polymap.get(poly);
            Double avg = -1d;
            if (doubleWrapper != null) {
                avg = doubleWrapper.getAverage();
//                logger.debug("adding " + avg + " for: " + poly);
            }
            fc.add(FeatureHelper.createFeature(poly, avg));
        }
        return fc;
    }

    // borrowing from http://gis.stackexchange.com/questions/106882/how-to-read-each-pixel-of-each-band-of-a-multiband-geotiff-with-geotools-java
    public void indexGeoTiff() throws IOException {
        try {
            File f = new File("/Users/abrin/Dropbox/skope-dev/ZuniCibola_PRISM_annual_prcp.tif");
            System.setProperty("java.awt.headless", "true");
            ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY
                    .createValue();
            policy.setValue(OverviewPolicy.IGNORE);

            // this will basically read 4 tiles worth of data at once from the disk...
            ParameterValue<String> gridsize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();
            // gridsize.setValue(512 * 4 + "," + 512);

            // Setting read type: use JAI ImageRead (true) or ImageReaders read methods (false)
            ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
            useJaiRead.setValue(true);

            GeoTiffReader reader2 = new GeoTiffReader(f);
            GridCoverage2D image = reader2.read(new GeneralParameterValue[] { policy, gridsize, useJaiRead });
            Rectangle2D bounds2D = image.getEnvelope2D().getBounds2D();
            bounds2D.getCenterX();
            // calculate zoom level for the image
            GridGeometry2D geometry = image.getGridGeometry();

            String[] coverageNames = reader2.getGridCoverageNames();
            // At this point, coverageNames may contain, as an instance, "pressure,temperature,humidity"

            logger.debug("coverage names:" + coverageNames);
            logger.debug("coord system: " + image.getCoordinateReferenceSystem());
            BufferedImage img = ImageIO.read(f);
            // ColorModel colorModel = img.getColorModel(
            WritableRaster raster = img.getRaster();

            int numBands = raster.getNumBands();

            int w = img.getWidth();
            int h = img.getHeight();

            logger.debug("bands:" + numBands + " width:" + w + " height:" + h);
            double minLat = 10000;
            double maxLat = -100000d;
            double minLong = 100000;
            double maxLong = -100000d;
            IndexWriter writer = setupLuceneIndexWriter();
            for (int k = 0; k < numBands; k++) {
                Map<String, DoubleWrapper> map = new HashMap<String, DoubleWrapper>();
                for (int i = 0; i < w; i++) {// width...
                    for (int j = 0; j < h; j++) {

                        double[] latlon = geo(geometry, i, j);
                        double lat = latlon[0];
                        double lon = latlon[1];
                        minLat = Math.min(lat, minLat);
                        minLong = Math.min(lon, minLong);
                        maxLat = Math.max(lat, maxLat);
                        maxLong = Math.max(lon, maxLong);
                        Double s = 0d;

                        double d = raster.getSampleDouble(i, j, k);
                        if (j % 100 == 0 && i % 100 == 0) {
                            logger.debug("lat:" + lat + " long:" + lon + " temp:" + s);
                        }
                        incrementTreeMap(map, d, lon, lat);
                    }
                }
                quadMapToIndex(writer, map, k);

            }
            writer.close();
            logger.debug(String.format("dimensions (%s, %s) x (%s, %s)", minLat, minLong, maxLat, maxLong));
        } catch (Exception ex) {
            logger.error(ex);
        }
    }

    private static double[] geo(GridGeometry2D geometry, int x, int y) throws Exception {

        // int zoomlevel = 1;
        Envelope2D pixelEnvelop = geometry.gridToWorld(new GridEnvelope2D(x, y, 1, 1));

        // pixelEnvelop.getCoordinateReferenceSystem().getName().getCodeSpace();
        return new double[] { pixelEnvelop.getCenterY(), pixelEnvelop.getCenterX() };

    }

    public void indexShapefile() throws IOException {
        Map<String, URL> connect = new HashMap<>();
        File file = new File("/Users/abrin/Dropbox/skope-dev");
        connect.put("url", file.toURI().toURL());
        ShapefileReader reader = new ShapefileReader();
        FeatureIterator<?> iterator = reader.readShapeAndGetFeatures(connect);

        IndexWriter writer = setupLuceneIndexWriter();
        int count = 0;

        /**
         * Here's we're aggregating at the basic level of the "quad"
         * 
         * NOTE: we could gain further performance enhancements by grouping the QUADS together and indexing those we can then query at those "levels"
         */
        Map<String, DoubleWrapper> valueMap = new HashMap<String, DoubleWrapper>();
        while (iterator.hasNext()) {
            count++;
            SimpleFeature obj = (SimpleFeature) iterator.next();
            Double gridCode = (Double) obj.getAttribute(GRID_CODE);

            Point point = (Point) obj.getDefaultGeometry();
            Coordinate coord = point.getCoordinate();
            String quadTree = incrementTreeMap(valueMap, gridCode, coord.x, coord.y);
            if (count % 10_000 == 0) {
                long parseLong = Long.parseLong(quadTree);
                logger.debug(count + "| " + quadTree + " " + parseLong);
            }

            // indexRawEntries(writer, gridCode, coord, parseLong);
        }

        quadMapToIndex(writer, valueMap, 0);
        writer.close();
    }

    private IndexWriter setupLuceneIndexWriter() throws IOException {
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(Version.LATEST, analyzer);

        if (true) {
            // Create a new index in the directory, removing any previously indexed documents:
            iwc.setOpenMode(OpenMode.CREATE);
        } else {
            // Add new documents to an existing index:
            iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
        }

        // iwc.setRAMBufferSizeMB(256.0);

        File path = new File("indexes");
        path.mkdirs();
        Directory dir = FSDirectory.open(path);
        IndexWriter writer = new IndexWriter(dir, iwc);
        return writer;
    }

    private void quadMapToIndex(IndexWriter writer, Map<String, DoubleWrapper> valueMap, int year) throws IOException {
        int count =0;
        for (String key : valueMap.keySet()) {
            count++;
            Double val = valueMap.get(key).getAverage();
            StringField codeField = new StringField(CODE, Double.toString(val), Field.Store.YES);
            LongField quad = new LongField(QUAD_, Long.parseLong(key), Field.Store.YES);
            IntField yr = new IntField(YEAR, year, Field.Store.NO);
            Document doc = new Document();
            doc.add(codeField);
            doc.add(yr);
            doc.add(quad);
            if (count % 10 == 0) {
                logger.debug(doc);
            }
            writer.addDocument(doc);

        }
    }

    private String incrementTreeMap(Map<String, DoubleWrapper> valueMap, Double gridCode, double x, double y) {
        String quadTree = toQuadTree(x, y, NUM_LEVELS);
        DoubleWrapper double1 = valueMap.get(quadTree);
        if (double1 == null) {
            double1 = new DoubleWrapper();
        }
        double1.increment(gridCode);
        valueMap.put(quadTree, double1);
        return quadTree;
    }

    private void indexRawEntries(IndexWriter writer, Double gridCode, Coordinate coord, long parseLong) throws IOException {
        Document doc = new Document();

        Field latField = new StringField(X, Double.toString(coord.x), Field.Store.YES);
        Field longField = new StringField(Y, Double.toString(coord.y), Field.Store.YES);
        Field codeField = new StringField(CODE, Double.toString(gridCode), Field.Store.YES);
        addLuceneGeospatialField(coord, parseLong, doc);

        doc.add(latField);
        doc.add(longField);
        doc.add(codeField);
        writer.addDocument(doc);
    }

    private void addLuceneGeospatialField(Coordinate coord, long parseLong, Document doc) {
        Field quad = new LongField(QUAD, parseLong, Field.Store.YES);
        com.spatial4j.core.shape.Point makePoint = ctx.makePoint(coord.x, coord.y);
        for (Field f : strategy.createIndexableFields(makePoint)) {
            doc.add(f);
        }
        doc.add(quad);
    }

    // http://wiki.openstreetmap.org/wiki/QuadTiles
    public static String toQuadTree(Double x1_, Double y1_, double depth) {
        // http://koti.mbnet.fi/ojalesa/quadtree/quadtree.js
        String toReturn = "";
        Double x1 = Math.floor(x1_ * Math.pow(2, 10) / NUM_TILES);
        Double y1 = Math.floor(y1_ * Math.pow(2, 10) / NUM_TILES);
        for (int i = (int) depth; i > 0; i--) {
            int pow = 1 << (i - 1);
            int cell = 0;
            if ((x1.intValue() & pow) > 0) {
                cell++;
            }
            if ((y1.intValue() & pow) > 0) {
                cell += 2;
            }
            toReturn += cell;
        }
        return toReturn;
    }

    /**
     * Implementation in JS from koti.mbnet.fi
     * 
     * function(x, y, z){
     * var arr = [];
     * for(var i=z; i>0; i--) {
     * var pow = 1<<(i-1);
     * var cell = 0;
     * if ((x&pow) != 0) cell++;
     * if ((y&pow) != 0) cell+=2;
     * arr.push(cell);
     * }
     * return arr.join("");
     * }
     **/

}
