package org.digitalantiquity.skope.service.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.digitalantiquity.skope.service.BoundingBoxHelper;
import org.digitalantiquity.skope.service.DoubleWrapper;
import org.geojson.FeatureCollection;
import org.postgis.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.github.davidmoten.geo.Coverage;
import com.github.davidmoten.geo.GeoHash;

@Service
public class FileService {

    @Autowired
    private transient ThreadPoolTaskExecutor taskExecutor;

    private final Logger logger = Logger.getLogger(getClass());

    @Value("${rootDir:#{'../dataDir/'}}")
    private String rootDir;

    public FeatureCollection search(String name, double x1, double y1, double x2, double y2, int year, int cols, int level) throws Exception {
        List<Polygon> boxes = BoundingBoxHelper.createBoundindBoxes(x2, y1, x1, y2, cols);
        FileEnvelopeQueryTask task = new FileEnvelopeQueryTask();
        return task.run(taskExecutor, boxes, this, level, year, rootDir);
    }

    public static File constructFileName(String rootDir_, int year, String hash) {
        return new File(rootDir_ + "datadir/" + StringUtils.join(hash.split("(?<=\\G...)"), "/") + "_.dat");
    }

    public List<Double> getDetailsFor(String indexName, Double x1, Double y1, Double x2, double y2, Integer cols, Integer zoom, String type)
            throws NumberFormatException, IOException {
        Coverage coverage = GeoHash.coverBoundingBoxMaxHashes(Math.max(y1, y2), Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), 40);
        List<DoubleWrapper> dws = new ArrayList<>();
        logger.debug(coverage);
        String suffix = "precip/";
        if (type.equalsIgnoreCase("t")) {
            suffix = "temp/";
        }
        for (String hash : coverage.getHashes()) {
            hash = hash.substring(0, 8);
            File file = constructFileName(rootDir + suffix, 0, hash);
            if (file.exists()) {
                int i = 0;
                for (String line : FileUtils.readLines(file)) {
                    Double val = Double.parseDouble(line);
                    if (dws.size() <= i) {
                        dws.add(new DoubleWrapper());
                    }
                    dws.get(i).increment(val);
                    i++;
                }
            }
        }

        List<Double> toReturn = new ArrayList<>();
        for (DoubleWrapper dw : dws) {
            toReturn.add(dw.getAverage());
        }
        return toReturn;
    }

}
