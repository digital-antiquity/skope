package org.digitalantiquity.skope.service;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.digitalantiquity.skope.service.geotiff.GeoTiffImageExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class ProcessFileTask {

    private final Logger logger = Logger.getLogger(getClass());
    private double max;
    private double min;

    public ProcessFileTask(double min, double max) {
        this.min = min;
        this.max = max;
    }

    private int extractBandNumberFromFilename(File f) {
        String name = FilenameUtils.getBaseName(f.getName());
        String band = StringUtils.substringAfterLast(name, "_");
        if (StringUtils.isNumeric(band)) {
            return Integer.parseInt(band) - 1;
        }
        return -1;
    }
    
//    @Value("${paleoCarOutputDir:#{'/Users/abrin/Desktop/OUTPUT/'}}")
    private String paleoDir = "/project/web/data/";


    public void run(ThreadPoolTaskExecutor taskExecutor, String group) {
        String[] ext = { "tif" };
        for (File file : FileUtils.listFiles(new File(paleoDir + group + "-out/"), ext, false)) {
            if (file.getName().contains("merge")) {
                GeoTiffImageExtractor task = new GeoTiffImageExtractor(file, extractBandNumberFromFilename(file), group,min,max);
                taskExecutor.execute(task);
            }
        }

        while (taskExecutor.getActiveCount() != 0) {
            int count = taskExecutor.getActiveCount();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (count == 0) {
                taskExecutor.shutdown();
                break;
            }
        }
    }

}
