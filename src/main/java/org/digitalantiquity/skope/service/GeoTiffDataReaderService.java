package org.digitalantiquity.skope.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.digitalantiquity.skope.service.geotiff.GeoTiffImageReader;
import org.springframework.stereotype.Service;

@Service
public class GeoTiffDataReaderService {

    private GeoTiffImageReader gdd;
    private GeoTiffImageReader ppt;

    private final Logger logger = Logger.getLogger(getClass());

    // @Value("${geoTiffDir:#{'images/'}}")
    private String geoTiffDir = "/home/images/";

    public GeoTiffDataReaderService() throws IOException {

        // this.gdd = new GeoTiffImageReader(gddF);
        // this.ppt = new GeoTiffImageReader(pptF);
    }

    private void execFile(File file, Double lat, Double lon) {
        String line = String.format("gdallocationinfo -xml -wgs84 \"%s\" %s %s",file.getAbsolutePath(), lat.toString(), lon.toString());
        CommandLine cmdLine = CommandLine.parse(line);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(1);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
        executor.setWatchdog(watchdog);
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(new byte[2048]);
            executor.getStreamHandler().setProcessOutputStream(bis);
            int exitValue = executor.execute(cmdLine);
            logger.debug(IOUtils.toString(bis));
        } catch (ExecuteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public Map<String, List<Float>> getBandData(Double y1, Double x1) {
        Map<String, List<Float>> toReturn = new HashMap<>();
        logger.debug("begin init");
        File gddF = new File(geoTiffDir, "GDD.tif");
        File pptF = new File(geoTiffDir, "PPT.tif");
        execFile(gddF, y1,x1);
        toReturn.put("PPT", ppt.getBandData(y1, x1));
        logger.debug("done PPT");
        toReturn.put("GDD", gdd.getBandData(y1, x1));
        logger.debug("done GDD;done");
        return toReturn;
    }

}
