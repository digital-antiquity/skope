package org.digitalantiquity.skope.service.file;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.digitalantiquity.skope.service.BoundingBoxHelper;
import org.geojson.FeatureCollection;
import org.postgis.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class FileService {

    @Autowired
    private transient ThreadPoolTaskExecutor taskExecutor;

    private final Logger logger = Logger.getLogger(getClass());

    public FeatureCollection search(String name, double x1, double y1, double x2, double y2, int year, int cols, int level) throws Exception {
        List<Polygon> boxes = BoundingBoxHelper.createBoundindBoxes(x2, y1, x1, y2, cols);
        EnvelopeQueryTask task = new EnvelopeQueryTask();
        return task.run(taskExecutor, boxes, this, level, year);
    }

    public static File constructFileName(int year, String hash) {
        return new File("../datadir/" + StringUtils.join(hash.split("(?<=\\G...)"),"/") +"_.dat");
    }

}
