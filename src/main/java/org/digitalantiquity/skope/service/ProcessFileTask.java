package org.digitalantiquity.skope.service;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.IndexWriter;
import org.geojson.FeatureCollection;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class ProcessFileTask {

    private int extractBandNumberFromFilename(File f) {
        String name = FilenameUtils.getBaseName(f.getName());
        String band = StringUtils.substringAfterLast(name, "_");
        if (StringUtils.isNumeric(band)) {
            return Integer.parseInt(band) - 1;
        }
        return -1;
    }

    public void run(ThreadPoolTaskExecutor taskExecutor, Collection<File> files, String group, IndexWriter writer) {
        for (File file : files) {
            GeoTiffProcessor task = new GeoTiffProcessor(file, extractBandNumberFromFilename(file), group, writer);
            taskExecutor.execute(task);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        while (taskExecutor.getActiveCount() != 0) {
            int count = taskExecutor.getActiveCount();
            try {
                writer.commit();
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (count == 0) {
                taskExecutor.shutdown();
                break;
            }
        }
        try {
            writer.commit();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public synchronized FeatureCollection getFeatureCollection() {
        return null;
    }
}
