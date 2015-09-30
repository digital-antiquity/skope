package org.digitalantiquity.skope.service.geotiff;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.operation.TransformException;

public class GeoTiffImageReader {

    private File file;
    private int numBands;
    private WritableRaster raster;
    private GridGeometry2D geometry;
    private final Logger logger = Logger.getLogger(getClass());

    public GeoTiffImageReader(File file) throws IOException {
        this.file = file;

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
        geometry = image.getGridGeometry();

        BufferedImage img = ImageIO.read(file);
        raster = img.getRaster();
        numBands = raster.getNumBands();
    }

    private Float getSample(Float lat, Float lon, int band) throws InvalidGridGeometryException, TransformException {
        DirectPosition2D dp = new DirectPosition2D(lon, lat);
        GridCoordinates2D toGrid = geometry.worldToGrid(dp);
        return raster.getSampleFloat(toGrid.x, toGrid.y, band);

    }

    public List<Float> getBandData(Double y1, Double x1) {
        List<Float> toReturn = new ArrayList<>();
        for (int i = 0; i < numBands; i++) {
            try {
                toReturn.add(getSample(y1.floatValue(), x1.floatValue(), i));
            } catch (Exception e) {
                toReturn.add(null);
                logger.error("error extracting band data:", e);
            }
        }
        return toReturn;
    }
}
