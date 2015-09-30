package org.digitalantiquity.skope.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.digitalantiquity.skope.service.geotiff.GeoTiffImageReader;
import org.springframework.stereotype.Service;

@Service
public class GeoTiffDataReaderService {

    private GeoTiffImageReader gdd;
    private GeoTiffImageReader ppt;

    private final Logger logger = Logger.getLogger(getClass());

    // @Value("${geoTiffDir:#{'images/'}}")
    private String geoTiffDir = "/home/ubuntu/images/";

    private String[] execFile(File file, Double lon, Double lat) {
        String line = String.format("gdallocationinfo -valonly -wgs84 \"%s\" %s %s",file.getAbsolutePath(), lat.toString(), lon.toString());
        logger.debug(line);
        CommandLine cmdLine = CommandLine.parse(line);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(1);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
        executor.setWatchdog(watchdog);
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            executor.setStreamHandler(streamHandler);
            executor.setExitValue(0);
            int exitValue = executor.execute(cmdLine);
            return StringUtils.split(outputStream.toString(),"\n");
        } catch (ExecuteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public Map<String, String[]> getBandData(Double y1, Double x1) {
        Map<String, String[]> toReturn = new HashMap<>();
        logger.debug("begin init");
        File gddF = new File(geoTiffDir, "GDD.tif");
        File pptF = new File(geoTiffDir, "PPT.tif");
        toReturn.put("PPT_water_year_demosaic", execFile(pptF, y1,x1));
        logger.debug("done PPT");
        toReturn.put("GDD_may_sept_demosaic", execFile(gddF, y1,x1));
        logger.debug("done GDD;done");
        return toReturn;
    }

}
