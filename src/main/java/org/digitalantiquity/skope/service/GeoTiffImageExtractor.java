package org.digitalantiquity.skope.service;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.log4j.Logger;
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

public class GeoTiffImageExtractor implements Runnable {

    private final Logger logger = Logger.getLogger(getClass());

    double xv[] = { 0, .15, .3, .45, .6, .75, 1 };
    double yr[] = { Color.WHITE.getRed(), Color.PINK.getRed(), Color.ORANGE.getRed(), Color.YELLOW.getRed(), Color.GREEN.getRed(), 102, 51 };
    double yg[] = { Color.WHITE.getGreen(), Color.PINK.getGreen(), Color.ORANGE.getGreen(), Color.YELLOW.getGreen(), Color.GREEN.getGreen(), 204, 102 };
    double yb[] = { Color.WHITE.getBlue(), Color.PINK.getBlue(), Color.ORANGE.getBlue(), Color.YELLOW.getBlue(), Color.GREEN.getBlue(), 51, 51 };

    private String group;

    private int band;

    private File file;

    private double min;

    private double max;

    public GeoTiffImageExtractor(File f, int band, String group, double min, double max) {
        this.file = f;
        this.band = band;
        this.group = group;
        this.min = min;
        this.max = max;
    }

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

            // String name = FilenameUtils.getBaseName(f.getName());
            BufferedImage precipOut = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);

//            SplineInterpolator inter = new SplineInterpolator();
            // 1400 - green
            // 1000 - green
            // 700 - yellow
            // 500 - orange
            // 300 - red/pink
            // 0 - white

            PolynomialSplineFunction red = null;
            PolynomialSplineFunction green = null;
            PolynomialSplineFunction blue = null;

            logger.debug(group + " >> bands:" + numBands + " width:" + w + " height:" + h);
            writeBand(geometry, raster, w, h, group, precipOut, red, green, blue, band);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    @Value("${imageDir:#{'src/main/webapp/img'}}")
    private String imageDir = "src/main/webapp/img/";

    
    private void writeBand(GridGeometry2D geometry, WritableRaster raster, int w, int h, String name, BufferedImage precipOut,
            PolynomialSplineFunction red, PolynomialSplineFunction green, PolynomialSplineFunction blue, int band) throws Exception, IOException {
        File precipOutFile = new File(imageDir + name + band + ".png");
        logger.debug(name + "> band:" + band);
        if (band == 0) {
            double[] latlon = geo(geometry, 0, 0);
            double[] latlon2 = geo(geometry, w - 1, h - 1);

            logger.debug(latlon[0] + "," + latlon[1] + " x " + latlon2[0] + "," + latlon2[1]);
        }
        for (int i = 0; i < w; i++) {// width...
            for (int j = 0; j < h; j++) {
                double precip = raster.getSampleDouble(i, j, 0);
                Color precipColor = getColor(precip, red, green, blue);
                precipOut.setRGB(i, j, precipColor.getRGB());
            }
        }
        ImageIO.write(precipOut, "png", precipOutFile);
    }

    private static double[] geo(GridGeometry2D geometry, int x, int y) throws Exception {

        // int zoomlevel = 1;
        Envelope2D pixelEnvelop = geometry.gridToWorld(new GridEnvelope2D(x, y, 1, 1));

        // pixelEnvelop.getCoordinateReferenceSystem().getName().getCodeSpace();
        return new double[] { pixelEnvelop.getCenterX(), pixelEnvelop.getCenterY() };

    }


    private Color getColor(double value, PolynomialSplineFunction red2, PolynomialSplineFunction green2, PolynomialSplineFunction blue2) {
        // we reverse the color so that the highest values are the darkest
        double ratio = 1d - (value / max);
        if (ratio > 1.0 || ratio < 0) {
            if (value > max) {
                max = value;
            }
            return new Color(0,0,0);
        }
        int v = (int) Math.floor(ratio * 255d);
        if (v > 255) {
            v = 255;
        } else if (v < 0) {
            v = 0;
        }
        if (v == 255) {
            return new Color(255,255,255,0);
        }
        return new Color(v, v, v);
    }

}
