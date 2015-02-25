package org.digitalantiquity.skope.service;

import java.util.Collection;

import org.geojson.FeatureCollection;
import org.postgis.Polygon;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class EnvelopeQueryTask {

    private FeatureCollection featureCollection = new FeatureCollection();

    public FeatureCollection run(ThreadPoolTaskExecutor taskExecutor, JdbcTemplate jdbcTemplate, Collection<Polygon> createBoundindBoxes) {
        for (Polygon poly : createBoundindBoxes) {
            EnvelopeQuerySubTask task = new EnvelopeQuerySubTask(poly, jdbcTemplate, this);
            taskExecutor.execute(task);
        }

        while (taskExecutor.getActiveCount() != 0) {
            int count = taskExecutor.getActiveCount();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (count == 0) {
                taskExecutor.shutdown();
                break;
            }
        }
        return getFeatureCollection();
    }

    public synchronized FeatureCollection getFeatureCollection() {
        return featureCollection;
    }

}
