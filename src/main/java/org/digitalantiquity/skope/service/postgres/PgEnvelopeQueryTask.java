package org.digitalantiquity.skope.service.postgres;

import java.util.Collection;

import org.geojson.FeatureCollection;
import org.postgis.Polygon;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class PgEnvelopeQueryTask {

    private FeatureCollection featureCollection = new FeatureCollection();

    public FeatureCollection run(ThreadPoolTaskExecutor taskExecutor, JdbcTemplate jdbcTemplate, Collection<Polygon> createBoundindBoxes, int year) {
        for (Polygon poly : createBoundindBoxes) {
            
            PgEnvelopeQuerySubTask task = new PgEnvelopeQuerySubTask(poly, jdbcTemplate, this, year);
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
