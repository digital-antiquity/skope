package org.digitalantiquity.skope.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.digitalantiquity.skope.service.geotiff.GeoTiffImageReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeoTiffDataReaderService {

    private GeoTiffImageReader gdd;
    private GeoTiffImageReader ppt;

    private final Logger logger = Logger.getLogger(getClass());

//    @Value("${geoTiffDir:#{'images/'}}")
    private String geoTiffDir = "/home/images/";

    public GeoTiffDataReaderService() throws IOException {

//        this.gdd = new GeoTiffImageReader(new File(geoTiffDir, "GDD.tif"));
//        this.ppt = new GeoTiffImageReader(new File(geoTiffDir, "PPT.tif"));
    }

    public Map<String, List<Float>> getBandData(Double y1, Double x1) {
        Map<String, List<Float>> toReturn = new HashMap<>();
        logger.debug("begin init");
        toReturn.put("PPT", ppt.getBandData(y1, x1));
        logger.debug("done PPT");
        toReturn.put("GDD", gdd.getBandData(y1, x1));
        logger.debug("done GDD;done");
        return toReturn;
    }

}
