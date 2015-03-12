package org.digitalantiquity.skope.service;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.digitalantiquity.skope.DocObject;
import org.digitalantiquity.skope.service.file.FileService;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.github.davidmoten.geo.GeoHash;
import com.spatial4j.core.context.SpatialContext;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

@Service
public class IndexingService {

    public final static Color FAR = Color.BLACK;
    public final static Color CLOSE = Color.WHITE;

    static final int LEVEL = 24; // LEVEL 14 == ZOOM 3 ; 15 == ZOOM 4
    private final Logger logger = Logger.getLogger(getClass());
    SpatialContext ctx = SpatialContext.GEO;
    SpatialPrefixTree grid = new GeohashPrefixTree(ctx, 24);
    RecursivePrefixTreeStrategy strategy = new RecursivePrefixTreeStrategy(grid, "location");
    private boolean indexUsingLucene = true;
    private boolean indexUsingPostgres = false;

    private boolean indexUsingFile = false;

    public IndexingService() {
        System.setProperty("java.awt.headless", "true");
    }

    // borrowing from http://gis.stackexchange.com/questions/106882/how-to-read-each-pixel-of-each-band-of-a-multiband-geotiff-with-geotools-java
    public void indexGeoTiff(String rootDir, JdbcTemplate template) throws IOException {
        try {

            String url = "https://www.dropbox.com/s/xhu23i328nm1q2b/ZuniCibola_PRISM_grow_prcp_ols_loocv_union_recons.tif?dl=1";
            // File f = new File("/Users/abrin/Dropbox/skope-dev/ZuniCibola_PRISM_annual_prcp.tif");
            logger.debug("downloading file... " + url);
            File f = new File("/tmp/skopeData", "tif");
            if (!f.exists()) {
                FileUtils.copyURLToFile(new URL(url), f);
            }

            logger.debug(f);
            if (indexUsingPostgres) {
                template.execute("truncate table skopedata;");
            }

            ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
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
            WritableRaster raster = img.getRaster();

            SplineInterpolator inter = new SplineInterpolator();
            // 1400 - green
            // 1000 - green
            // 700 - yellow
            // 500 - orange
            // 300 - red/pink
            // 0 - white
            double xv[] = { 0, .15, .3, .45, .6, .75, 1 };
            double yr[] = { Color.WHITE.getRed(), Color.PINK.getRed(), Color.ORANGE.getRed(), Color.YELLOW.getRed(), Color.GREEN.getRed(), 102, 51 };
            double yg[] = { Color.WHITE.getGreen(), Color.PINK.getGreen(), Color.ORANGE.getGreen(), Color.YELLOW.getGreen(), Color.GREEN.getGreen(), 204, 102 };
            double yb[] = { Color.WHITE.getBlue(), Color.PINK.getBlue(), Color.ORANGE.getBlue(), Color.YELLOW.getBlue(), Color.GREEN.getBlue(), 51, 51 };
            PolynomialSplineFunction red = inter.interpolate(xv, yr);
            PolynomialSplineFunction green = inter.interpolate(xv, yg);
            PolynomialSplineFunction blue = inter.interpolate(xv, yb);
            int numBands = raster.getNumBands();

            int w = img.getWidth();
            int h = img.getHeight();

            double minLat = 10000;
            double maxLat = -100000d;
            double minLong = 100000;
            double maxLong = -100000d;
            IndexWriter writer = setupLuceneIndexWriter("skope");
            writer.deleteAll();
            writer.commit();
            // numBands = 5;
            File file = new File("src/main/webapp/img/");
            file.mkdirs();

            BufferedImage imageOut = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
            logger.debug("bands:" + numBands + " width:" + w + " height:" + h);
            for (int k = 0; k < numBands; k++) {
                File outFile = new File("src/main/webapp/img/out" + k + ".png");
                logger.debug("> band:" + k);
                if (k == 0) {
                    double[] latlon = geo(geometry, 0, 0);
                    double[] latlon2 = geo(geometry, w - 1, h - 1);

                    logger.debug(latlon[0] + "," + latlon[1] + " x " + latlon2[0] + "," + latlon2[1]);
                }
                for (int i = 0; i < w; i++) {// width...
                    for (int j = 0; j < h; j++) {

                        double[] latlon = geo(geometry, i, j);
                        double x = latlon[0];
                        double y = latlon[1];
                        Coordinate coord = new Coordinate(x, y);
                        // Double s = 0d;

                        double d = raster.getSampleDouble(i, j, k);
                        Color color = getColor(d, red, green, blue);
                        imageOut.setRGB(i, j, color.getRGB());
                        if (indexUsingFile) {
                            try {
                                indexRawEntries(d, k, rootDir, latlon);
                            } catch (Exception e) {
                                logger.error(e, e);
                            }
                        }
                    }
                }
                ImageIO.write(imageOut, "png", outFile);
            }

            for (int i = 0; i < w; i++) {// width...
                logger.debug(">>>  "+i+ "..." + w);
                for (int j = 0; j < h; j++) {
                    double[] latlon = geo(geometry, i, j);
                    double x = latlon[0];
                    double y = latlon[1];
                    Coordinate coord = new Coordinate(x, y);
                    DocObject vals = new DocObject(coord);

                    for (int k = 0; k < numBands; k++) {
                        double d = raster.getSampleDouble(i, j, k);

                        while (vals.getVals().size() <= k) {
                            vals.getVals().add(null);
                        }
                        vals.getVals().set(k, d);
                    }
                    try {
                        indexRawEntriesLucene(writer, vals);
                    } catch (Exception e) {
                        logger.error(e, e);
                    }
                }
                writer.commit();
            }

            writer.commit();
            writer.close();

            logger.debug(String.format("dimensions (%s, %s) x (%s, %s)", minLat, minLong, maxLat, maxLong));
        } catch (Exception ex) {
            logger.error(ex, ex);
        }
    }

    private void indexRawEntriesLucene(IndexWriter writer, DocObject val) throws IOException {
        Coordinate coord = val.getCoord();
        DoubleField x = new DoubleField(IndexFields.X, coord.x, Field.Store.YES);
        DoubleField y = new DoubleField(IndexFields.Y, coord.y, Field.Store.YES);
        Field yr = new TextField(IndexFields.YEAR, StringUtils.join(val.getVals(), "|"), Field.Store.YES);
        TextField hash = new TextField(IndexFields.HASH, GeoHash.encodeHash(coord.y, coord.x), Field.Store.YES);

        Document doc = new Document();
        doc.add(hash);
        doc.add(x);
        doc.add(y);
        doc.add(yr);
        indexGeospatial(coord, doc);
        writer.addDocument(doc);

    }

    // function transition(value, maximum, start_point, end_point):
    // return start_point + (end_point - start_point)*value/maximum

    private Color getColor(double value, PolynomialSplineFunction red2, PolynomialSplineFunction green2, PolynomialSplineFunction blue2) {
        double ratio = value / 1400d;
        int red = (int) Math.floor(red2.value(ratio));
        int green = (int) Math.floor(green2.value(ratio));
        int blue = (int) Math.floor(blue2.value(ratio));
        if (red > 255) {
            red = 255;
        }
        if (green > 255) {
            green = 255;
        }
        if (blue > 255) {
            blue = 255;
        }
        if (red < 0) {
            red = 0;
        }
        if (green < 0) {
            green = 0;
        }
        if (blue < 0) {
            blue = 0;
        }
        return new Color(red, green, blue);
    }

    private static double[] geo(GridGeometry2D geometry, int x, int y) throws Exception {

        // int zoomlevel = 1;
        Envelope2D pixelEnvelop = geometry.gridToWorld(new GridEnvelope2D(x, y, 1, 1));

        // pixelEnvelop.getCoordinateReferenceSystem().getName().getCodeSpace();
        return new double[] { pixelEnvelop.getCenterX(), pixelEnvelop.getCenterY() };

    }

    public void indexShapefile(String rootDir) throws IOException {
        Map<String, URL> connect = new HashMap<>();
        File file = new File("/Users/abrin/Dropbox/skope-dev");
        connect.put("url", file.toURI().toURL());
        ShapefileReader reader = new ShapefileReader();
        FeatureIterator<?> iterator = reader.readShapeAndGetFeatures(connect);

        IndexWriter writer = setupLuceneIndexWriter("prism");
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
            if (count % 50_000 == 0) {
                // long parseLong = Long.parseLong(quadTree);
                logger.debug(count + "| " + quadTree);
            }

            indexRawEntriesLucene(writer, gridCode, 0, coord);
        }

        indexByQuadMap(writer, null, valueMap, 0, rootDir);
        writer.close();
    }

    private IndexWriter setupLuceneIndexWriter(String indexName) throws IOException {
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(Version.LATEST, analyzer);

        if (true) {
            // Create a new index in the directory, removing any previously indexed documents:
            iwc.setOpenMode(OpenMode.CREATE);
            // } else {
            // // Add new documents to an existing index:
            // iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
        }

        // iwc.setRAMBufferSizeMB(256.0);

        File path = new File("indexes/" + indexName);
        path.mkdirs();
        Directory dir = FSDirectory.open(path);
        IndexWriter writer = new IndexWriter(dir, iwc);
        return writer;
    }

    public void indexRawEntries(Double val, int year, String rootDir, double[] coord) throws IOException {
        String key = GeoHash.encodeHash(coord[1], coord[0], 8);
        File f = FileService.constructFileName(rootDir, year, key);
        f.getParentFile().mkdirs();
        boolean append = true;
        if (year == 0) {
            append = false;
        }
        FileUtils.writeStringToFile(f, Double.toString(val) + "\r\n", append);
    }

    /**
     * Create an entry in the Lucene Index for the Map of QuadMap keys to averages
     * 
     * @param writer
     * @param valueMap
     * @param year
     * @throws IOException
     */
    public void indexByQuadMap(IndexWriter writer, JdbcTemplate jdbcTemplate, Map<String, DoubleWrapper> valueMap, int year, String rootDir) throws IOException {
        int count = 0;
        for (String key : valueMap.keySet()) {
            count++;
            DoubleWrapper wrapper = valueMap.get(key);
            Double val = wrapper.getAverage();
            if (indexUsingFile) {
                File f = FileService.constructFileName(rootDir, year, key);
                f.getParentFile().mkdirs();
                boolean append = true;
                if (year == 0) {
                    append = false;
                }
                FileUtils.writeStringToFile(f, Double.toString(val) + "\r\n", append);
            }
            if (indexUsingPostgres) {
                jdbcTemplate.execute("insert into skopedata (hash,year,temp) values ('" + key + "'," + year + "," + Double.toString(val) + ");");
            }

            if (indexUsingLucene) {
                StringField codeField = new StringField(IndexFields.CODE, Double.toString(val), Field.Store.YES);
                Document doc = new Document();
                if (NumberUtils.isNumber(key)) {
                    LongField quad = new LongField(IndexFields.QUAD_, Long.parseLong(key), Field.Store.YES);
                    doc.add(quad);
                } else {
                    StringField hash = new StringField(IndexFields.HASH, key, Field.Store.YES);
                    doc.add(hash);
                    IntField level = new IntField(IndexFields.LEVEL, key.length(), Field.Store.YES);
                    logger.trace(">>> " + hash + " " + val + " - " + key.length());
                    doc.add(level);
                }
                DoubleField x = new DoubleField(IndexFields.X, wrapper.getX(), Field.Store.YES);
                DoubleField y = new DoubleField(IndexFields.Y, wrapper.getY(), Field.Store.YES);
                IntField yr = new IntField(IndexFields.YEAR, year, Field.Store.NO);
                doc.add(codeField);
                doc.add(x);
                doc.add(y);
                doc.add(yr);
                if (count % 10_000 == 0) {
                    logger.debug(year + ": (" + count + ")" + doc);
                }
                writer.addDocument(doc);
            }
        }
    }

    private void indexRawEntriesLucene(IndexWriter writer, Double code, int year, Coordinate coord) throws IOException {
        StringField codeField = new StringField(IndexFields.CODE, Double.toString(code), Field.Store.YES);
        Field quad = new StringField(IndexFields.QUAD, QuadTreeHelper.toQuadTree(coord.x, coord.y), Field.Store.YES);
        DoubleField x = new DoubleField(IndexFields.X, coord.x, Field.Store.YES);
        DoubleField y = new DoubleField(IndexFields.Y, coord.y, Field.Store.YES);
        IntField yr = new IntField(IndexFields.YEAR, year, Field.Store.YES);
        TextField hash = new TextField(IndexFields.HASH, GeoHash.encodeHash(coord.y, coord.x), Field.Store.YES);

        Document doc = new Document();
        doc.add(codeField);
        doc.add(hash);
        doc.add(x);
        doc.add(y);
        doc.add(yr);
        doc.add(quad);
        indexGeospatial(coord, doc);
        writer.addDocument(doc);
    }

    private void indexGeospatial(Coordinate coord, Document doc) {
        com.spatial4j.core.shape.Point shape = ctx.makePoint(coord.x, coord.y);
        for (IndexableField f : strategy.createIndexableFields(shape)) {
            doc.add(f);
        }
    }

    private String incrementTreeMap(Map<String, DoubleWrapper> valueMap, Double gridCode, double x, double y) {
        String quadTree = QuadTreeHelper.toQuadTree(x, y);
        // addQuadToMap(valueMap, gridCode, x, y, quadTree);
        addGeoHashToMap(valueMap, gridCode, x, y);
        return quadTree;
    }

    private void addGeoHashToMap(Map<String, DoubleWrapper> valueMap, Double gridCode, double x, double y) {
        increment(valueMap, gridCode, x, y, GeoHash.encodeHash(y, x, 3));
        increment(valueMap, gridCode, x, y, GeoHash.encodeHash(y, x, 4));// 9w4 nsx
        increment(valueMap, gridCode, x, y, GeoHash.encodeHash(y, x, 5));//
        increment(valueMap, gridCode, x, y, GeoHash.encodeHash(y, x, 6));
        increment(valueMap, gridCode, x, y, GeoHash.encodeHash(y, x, 7));
    }

    private void increment(Map<String, DoubleWrapper> valueMap, Double gridCode, double x, double y, String gh) {
        DoubleWrapper double1 = valueMap.get(gh);
        if (double1 == null) {
            double1 = new DoubleWrapper(x, y);
        }
        double1.increment(gridCode);
        valueMap.put(gh, double1);
    }

    private void addQuadToMap(Map<String, DoubleWrapper> valueMap, Double gridCode, double x, double y, String quadTree) {
        increment(valueMap, gridCode, x, y, quadTree);
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
