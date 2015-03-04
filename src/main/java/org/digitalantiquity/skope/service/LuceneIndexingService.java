package org.digitalantiquity.skope.service;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
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
import org.springframework.stereotype.Service;

import com.spatial4j.core.context.SpatialContext;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

@Service
public class LuceneIndexingService {

    static final int LEVEL = 18; // LEVEL 14 == ZOOM 3 ; 15 == ZOOM 4
    private final Logger logger = Logger.getLogger(getClass());
    SpatialContext ctx = SpatialContext.GEO;
    SpatialPrefixTree grid = new GeohashPrefixTree(ctx, 24);
    RecursivePrefixTreeStrategy strategy = new RecursivePrefixTreeStrategy(grid, "location");

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
            writer.deleteAll();
            writer.commit();


            for (int k = 0; k < numBands; k++) {
                Map<String, DoubleWrapper> map = new HashMap<String, DoubleWrapper>();
                for (int i = 0; i < w; i++) {// width...
                    for (int j = 0; j < h; j++) {

                        double[] latlon = geo(geometry, i, j);
                        double x = latlon[0];
                        double y = latlon[1];
                        Double s = 0d;

                        double d = raster.getSampleDouble(i, j, k);
                        if (j % 100 == 0 && i % 100 == 0) {
                            logger.debug("lat:" + y + " long:" + x + " temp:" + s);
                        }
                        incrementTreeMap(map, d, x, y);
                    }
                }
                indexByQuadMap(writer, map, k);

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
        return new double[] { pixelEnvelop.getCenterX(), pixelEnvelop.getCenterY() };

    }

    public void indexShapefile() throws IOException {
        Map<String, URL> connect = new HashMap<>();
        File file = new File("/Users/abrin/Dropbox/skope-dev");
        connect.put("url", file.toURI().toURL());
        ShapefileReader reader = new ShapefileReader();
        FeatureIterator<?> iterator = reader.readShapeAndGetFeatures(connect);

        IndexWriter writer = setupLuceneIndexWriter();
        int count = 0;
        writer.deleteAll();
        writer.commit();

        /**
         * Here's we're aggregating at the basic level of the "quad"
         * 
         * NOTE: we could gain further performance enhancements by grouping the QUADS together and indexing those we can then query at those "levels"
         */
        Map<String, DoubleWrapper> valueMap = new HashMap<String, DoubleWrapper>();
        while (iterator.hasNext()) {
            count++;
            SimpleFeature obj = (SimpleFeature) iterator.next();
            Double gridCode = (Double) obj.getAttribute(IndexFields.GRID_CODE);
            Point point = (Point) obj.getDefaultGeometry();
            Coordinate coord = point.getCoordinate();
            String quadTree = incrementTreeMap(valueMap, gridCode, coord.x, coord.y);
            if (count % 10_000 == 0) {
                long parseLong = Long.parseLong(quadTree);
                logger.debug(count + "| " + quadTree + " " + parseLong);
            }

            // indexRawEntries(writer, gridCode, coord, parseLong);
        }

        indexByQuadMap(writer, valueMap, 0);
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

    /**
     * Create an entry in the Lucene Index for the Map of QuadMap keys to averages
     * 
     * @param writer
     * @param valueMap
     * @param year
     * @throws IOException
     */
    private void indexByQuadMap(IndexWriter writer, Map<String, DoubleWrapper> valueMap, int year) throws IOException {
        int count = 0;
        for (String key : valueMap.keySet()) {
            count++;
            DoubleWrapper wrapper = valueMap.get(key);
            Double val = wrapper.getAverage();
            StringField codeField = new StringField(IndexFields.CODE, Double.toString(val), Field.Store.YES);
            LongField quad = new LongField(IndexFields.QUAD_, Long.parseLong(key), Field.Store.YES);
            DoubleField x = new DoubleField(IndexFields.X, wrapper.getX(), Field.Store.YES);
            DoubleField y = new DoubleField(IndexFields.Y, wrapper.getY(), Field.Store.YES);
            IntField yr = new IntField(IndexFields.YEAR, year, Field.Store.NO);
            Document doc = new Document();
            doc.add(codeField);
            doc.add(x);
            doc.add(y);
            doc.add(yr);
            doc.add(quad);
            if (key.equals(key.substring(0, LEVEL))) {
                logger.debug(">>" +key);
            }
            if (count % 10 == 0) {
                logger.debug(doc);
            }
            writer.addDocument(doc);

        }
    }

    private String incrementTreeMap(Map<String, DoubleWrapper> valueMap, Double gridCode, double x, double y) {
        String quadTree = QuadTreeHelper.toQuadTree(x, y);
        addQuadToMap(valueMap, gridCode, x, y, quadTree);
        addQuadToMap(valueMap, gridCode, x, y, quadTree.substring(0,LEVEL));
        return quadTree;
    }

    private void addQuadToMap(Map<String, DoubleWrapper> valueMap, Double gridCode, double x, double y, String quadTree) {
        DoubleWrapper double1 = valueMap.get(quadTree);
        if (double1 == null) {
            double1 = new DoubleWrapper(x,y);
        }
        double1.increment(gridCode);
        valueMap.put(quadTree, double1);
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
