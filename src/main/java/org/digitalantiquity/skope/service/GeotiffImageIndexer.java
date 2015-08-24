package org.digitalantiquity.skope.service;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Envelope2D;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.springframework.beans.factory.annotation.Value;

import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.shape.Point;

public class GeotiffImageIndexer implements Runnable {

    private final Logger logger = Logger.getLogger(getClass());

    double xv[] = { 0, .15, .3, .45, .6, .75, 1 };
    double yr[] = { Color.WHITE.getRed(), Color.PINK.getRed(), Color.ORANGE.getRed(), Color.YELLOW.getRed(), Color.GREEN.getRed(), 102, 51 };
    double yg[] = { Color.WHITE.getGreen(), Color.PINK.getGreen(), Color.ORANGE.getGreen(), Color.YELLOW.getGreen(), Color.GREEN.getGreen(), 204, 102 };
    double yb[] = { Color.WHITE.getBlue(), Color.PINK.getBlue(), Color.ORANGE.getBlue(), Color.YELLOW.getBlue(), Color.GREEN.getBlue(), 51, 51 };

    private String group;
    SpatialContext ctx = SpatialContext.GEO;
    SpatialPrefixTree grid = new GeohashPrefixTree(ctx, 24);
    RecursivePrefixTreeStrategy strategy = new RecursivePrefixTreeStrategy(grid, "location");

    private File file;
    private IndexWriter writer;

    private IndexFileTask indexFileTask;

    public GeotiffImageIndexer(File f, String group, IndexWriter writer, IndexFileTask indexFileTask) {
        this.file = f;
        this.group = group;
        this.writer = writer;
        this.indexFileTask = indexFileTask;
    }

    private double max;
    private double min;
    double minLat = 10000;
    double maxLat = -100000d;
    double minLong = 100000;
    double maxLong = -100000d;

    @Value("${dataDir:#{''}}")
    private String dataDir = "../data/";

    @Override
    public void run() {
        try {

            ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
            policy.setValue(OverviewPolicy.IGNORE);
            // this will basically read 4 tiles worth of data at once from the disk...
            ParameterValue<String> gridsize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();
            // gridsize.setValue(512 * 4 + "," + 512);

            // Setting read type: use JAI ImageRead (true) or ImageReaders read methods (false)
            ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
            useJaiRead.setValue(true);

            GeoTiffReader reader2 = new GeoTiffReader(file);
            GridCoverage2D image = reader2.read(new GeneralParameterValue[] { policy, gridsize, useJaiRead });
            Rectangle2D bounds2D = image.getEnvelope2D().getBounds2D();
            bounds2D.getCenterX();
            // calculate zoom level for the image
            GridGeometry2D geometry = image.getGridGeometry();

            BufferedImage img = ImageIO.read(file);
            WritableRaster raster = img.getRaster();
            int numBands = raster.getNumBands();
            int w = img.getWidth();
            int h = img.getHeight();

            logger.debug(group + "(" + file + ")" + " >> bands:" + numBands + " width:" + w + " height:" + h);
            writeBand(geometry, raster, w, h, group, numBands);
            logger.debug(String.format("(%s -> %s) dimensions [[%s, %s] x [%s, %s]]", min, max, minLat, minLong, maxLat, maxLong));
            indexFileTask.reconcileValues(max, min, maxLat, minLat, maxLong, minLong);
        } catch (Exception e) {
            logger.error(e + " " + ExceptionUtils.getStackTrace(e));
        }

    }

    private void writeBand(GridGeometry2D geometry, WritableRaster raster, int w, int h, String name, int numBands) throws Exception, IOException {
        int iter = 0;
        File dir = new File(dataDir + group + "/" + FilenameUtils.getBaseName(file.getName()));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File manifest = new File(dir, "manifest.txt");
        manifest.delete();
        manifest.createNewFile();
        FileWriter fw = new FileWriter(manifest);
        manifest.createNewFile();
        BufferedWriter bw = new BufferedWriter(fw);

        for (int i = 0; i < w; i++) {// width...
            for (int j = 0; j < h; j++) {
                iter++;
                double[] latlon = geo(geometry, i, j);
                bw.append(String.format("%s|%s|%s]\n", iter, latlon[0], latlon[1]));
                writeData(dir, iter, latlon, raster, i, j, numBands);
            }
            if (i % 5 == 0) {
                logger.debug(file.getName() + " :: " + ((double)iter/(double)(w*h))*100);
            }
        }
        IOUtils.closeQuietly(bw);
        IOUtils.closeQuietly(fw);
    }

    private void writeData(File dir, int iter, double[] latlon, WritableRaster raster, int i, int j, int numBands) throws IOException {
        DoubleField x = new DoubleField(IndexFields.X, latlon[0], Field.Store.YES);
        DoubleField y = new DoubleField(IndexFields.Y, latlon[1], Field.Store.YES);
        // Field yr = new TextField(IndexFields.YEAR, StringUtils.join(data, "|"), Field.Store.YES);
        Field type = new StringField(IndexFields.TYPE, group, Field.Store.YES);
        // TextField hash = new TextField(IndexFields.HASH, GeoHash.encodeHash(latlon[1], latlon[0]), Field.Store.YES);
        File outfile = new File(dir, iter + ".txt");
        if (latlon[0] - minLat < 0 ) {
            minLat = latlon[0];
        }
        if (latlon[1] - minLong < 0 ) {
            minLong = latlon[1];
        }

        if (minLat - latlon[0] > 0 ) {
            maxLat = latlon[0];
        }
        if (maxLong - latlon[1]  > 0 ) {
            maxLong = latlon[1];
        }

        
        outfile.delete();
        outfile.createNewFile();
        FileWriter fw = new FileWriter(outfile);
        BufferedWriter bw = new BufferedWriter(fw);
        for (int k = 0; k < numBands; k++) {
            double val = raster.getSampleDouble(i, j, k);
            bw.append(String.format("%s", val));
            bw.write("|");
            if (val > max) {
                max = val;
            }
            if (val < min) {
                min = val;
            }
        }
        bw.append(String.format("\n#latlong [%s,%s]\n", latlon[0], latlon[1]));

        IOUtils.closeQuietly(bw);
        IOUtils.closeQuietly(fw);
        if (true) {
            Document doc = new Document();
            String path = outfile.getPath();
            path = path.replace("/Users/abrin/Documents/", "");
            TextField hash = new TextField(IndexFields.VAL, path, Field.Store.YES);
            doc.add(hash);
            doc.add(type);
            doc.add(x);
            doc.add(y);
            indexGeospatial(latlon, doc);
            writer.addDocument(doc);
        }

    }

    private static double[] geo(GridGeometry2D geometry, int x, int y) throws Exception {
        // int zoomlevel = 1;
        Envelope2D pixelEnvelop = geometry.gridToWorld(new GridEnvelope2D(x, y, 1, 1));

        // pixelEnvelop.getCoordinateReferenceSystem().getName().getCodeSpace();
        return new double[] { pixelEnvelop.getCenterX(), pixelEnvelop.getCenterY() };

    }

    private void indexGeospatial(double[] latlon, Document doc) {
        Point shape = ctx.makePoint(latlon[0], latlon[1]);
        for (IndexableField f : strategy.createIndexableFields(shape)) {
            doc.add(f);
        }
    }

}
