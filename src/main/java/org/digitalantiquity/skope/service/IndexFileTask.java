package org.digitalantiquity.skope.service;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.index.IndexWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class IndexFileTask {

    private double max;
    private double min;
    private double maxLat;
    private double minLat = 10000000.0;
    private double maxLong;
    private double minLong = 10000000.0;

    @Value("${paleoCarOutputDir:#{'/Users/abrin/Desktop/OUTPUT/'}}")
    private String paleoDir;

    public void run(ThreadPoolTaskExecutor taskExecutor, String group, IndexWriter writer) {
        String[] ext = { "tif" };
        for (File file : FileUtils.listFiles(new File(paleoDir + group + "/in/"), ext, false)) {
            if (file.getName().contains("recon")) {
                GeotiffImageIndexer task = new GeotiffImageIndexer(file, group, writer, this);
                taskExecutor.execute(task);
            }
        }

        while (taskExecutor.getActiveCount() != 0) {
            int count = taskExecutor.getActiveCount();
            try {
                Thread.sleep(2000);
                writer.commit();
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
    }

    public synchronized void reconcileValues(double max, double min, double maxLat, double minLat, double maxLong, double minLong) {
        if (this.max < max) {
            this.max = max;
        }
        if (this.min > min) {
            this.min = min;
        }
        
        if (this.minLat > minLat && minLat != 0.0) {
            this.minLat = minLat;
        }

        if (this.maxLat > maxLat && maxLat != 0.0) {
            this.maxLat = maxLat;
        }
        if (this.minLong > minLong && minLong != 0.0) {
            this.minLong = minLong;
        }
        if (this.maxLong < maxLong && maxLong != 0.0) {
            this.maxLong = maxLong;
        }
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMaxLat() {
        return maxLat;
    }

    public void setMaxLat(double maxLat) {
        this.maxLat = maxLat;
    }

    public double getMinLat() {
        return minLat;
    }

    public void setMinLat(double minLat) {
        this.minLat = minLat;
    }

    public double getMaxLong() {
        return maxLong;
    }

    public void setMaxLong(double maxLong) {
        this.maxLong = maxLong;
    }

    public double getMinLong() {
        return minLong;
    }

    public void setMinLong(double minLong) {
        this.minLong = minLong;
    }

    public String printVals() {
        return String.format("range: %s -> %s [[%s,%s], [%s,%s]]", min, max, minLat, minLong, maxLat, maxLong);
    }

}
