package org.digitalantiquity.skope.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@PropertySource(value = "classpath:skope.properties")
public class GeoTiffDataReaderService {

    private final Logger logger = Logger.getLogger(getClass());

    @Autowired
    Environment environment;

    private String[] execFile(File file, Double lon, Double lat) {
        String line = String.format("gdallocationinfo -valonly -wgs84 \"%s\" %s %s", file.getAbsolutePath(), lat.toString(), lon.toString());
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
            return StringUtils.split(outputStream.toString(), "\n");
        } catch (ExecuteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getFilesList() {
        return Arrays.asList(environment.getProperty("fileList").split(","));
    }

    public String getFileDir() {
        return environment.getProperty("imageDir");
    }

    public Map<String, String[]> getBandData(Double y1, Double x1) {
        Map<String, String[]> toReturn = new HashMap<>();
        logger.debug("begin init");
        for (String name : getFilesList()) {
            File file = new File(getFileDir(), name);
            toReturn.put(name, execFile(file, y1, x1));
        }
        logger.debug("done");
        return toReturn;
    }

    public File exportData(Double x, Double y, Integer startTime, Integer endTime) throws IOException {
        File outFile = File.createTempFile("skope-csv-export", "csv");
        try {
            FileWriter fwriter = new FileWriter(outFile);
            List<String> labels = new ArrayList<>();
            labels.add(0, "Year");
            List<String> filesList = getFilesList();
            labels.addAll(filesList);
            Map<String, String[]> vals = new HashMap<>();
            for (String name : filesList) {
                File file = new File(getFileDir(), name);
                vals.put(name, execFile(file, y, x));
            }
            CSVPrinter printer = CSVFormat.EXCEL.withHeader(labels.toArray(new String[0])).print(fwriter);
            String format = String.format("### data for (Lat: %s ; Lon:%s) from %s to %s", x, y, startTime, endTime);
            logger.debug(format);
            printer.printComment(format);
            if (endTime == 2000) {
                endTime -= 1;
            }
            for (int t = startTime; t <= endTime; t++) {

                List<Object> row = new ArrayList<>();
                row.add(t);
                for (int i = 1; i < labels.size(); i++) {
                    try {
                        row.add(vals.get(labels.get(i))[t]);
                    } catch (Exception e) {
                        logger.debug(e, e);
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

    public File extractData(Integer startTime, Integer endTime, String geoJson, String name) throws IOException {
        File tiffFile = new File(getFileDir(), name + ".tif");
        File jsonFile = File.createTempFile("clip", ".json");
        FileUtils.writeStringToFile(jsonFile, geoJson);
        File outFile = File.createTempFile("clip", ".tif");

        String line = String.format("gdalwarp -cutline %s -crop_to_cutline -co COMPRESS=DEFLATE -wm 2000000000 -of GTiff %s %s ",
                jsonFile, tiffFile.getAbsolutePath(), outFile.getAbsolutePath());
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
            return outFile;
        } catch (ExecuteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}
