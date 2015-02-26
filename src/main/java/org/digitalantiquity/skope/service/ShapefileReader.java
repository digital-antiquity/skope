package org.digitalantiquity.skope.service;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;

public class ShapefileReader {

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * reading Shape
     * @param connect
     * @return
     * @throws IOException
     */
    FeatureIterator<?> readShapeAndGetFeatures(Map<String, URL> connect) throws IOException {
        DataStore dataStore = DataStoreFinder.getDataStore(connect);
        String[] typeNames = dataStore.getTypeNames();
        String typeName = typeNames[0];
        logger.info(typeName);
        System.out.println("Reading content " + typeName);
        logger.info("info:" + dataStore.getInfo().getTitle() + dataStore.getInfo().getDescription());
        FeatureSource<?, ?> featureSource = dataStore.getFeatureSource(typeName);
        FeatureCollection<?, ?> collection = featureSource.getFeatures();
        FeatureIterator<?> iterator = collection.features();
        return iterator;
    }

}
