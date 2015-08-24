package org.digitalantiquity.skope.service;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.spatial4j.core.context.SpatialContext;

@Service
public class IndexingService {

    public final static Color FAR = Color.BLACK;
    public final static Color CLOSE = Color.WHITE;

    static final int LEVEL = 24; // LEVEL 14 == ZOOM 3 ; 15 == ZOOM 4
    private final Logger logger = Logger.getLogger(getClass());
    SpatialContext ctx = SpatialContext.GEO;
    SpatialPrefixTree grid = new GeohashPrefixTree(ctx, 24);
    RecursivePrefixTreeStrategy strategy = new RecursivePrefixTreeStrategy(grid, "location");

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    public IndexingService() {
        System.setProperty("java.awt.headless", "true");
    }

    String[] urls = { "https://www.dropbox.com/s/1te2hjcvei1816n/ppt.water_year.tif?dl=1", "https://www.dropbox.com/s/q74gcq76imtvqng/ppt.annual.tif?dl=1" };

    // borrowing from http://gis.stackexchange.com/questions/106882/how-to-read-each-pixel-of-each-band-of-a-multiband-geotiff-with-geotools-java
    public void indexGeoTiff(String rootDir, JdbcTemplate template, ThreadPoolTaskExecutor taskExecutor2) throws IOException {
	//"PPT_may_sept_demosaic", "PPT_annual_demosaic",
        String[] groups = { "GDD_may_sept_demosaic","PPT_water_year_demosaic" };
        IndexWriter writer = setupLuceneIndexWriter("skope");
        writer.deleteAll();
        writer.commit();
        File file = new File("src/main/webapp/img/");
        file.mkdirs();

        if (taskExecutor == null) {
            taskExecutor = taskExecutor2;
        }
        JSONObject dataList = new JSONObject();
        for (String group : groups) {
            JSONObject kvp = new JSONObject();
		System.out.println("indexing: " + group);
            dataList.put(group, kvp);
            IndexFileTask task2 = new IndexFileTask();
            task2.run(taskExecutor, group, writer);
		System.out.println("processing: " + group);
            logger.debug(task2.printVals());
            kvp.put("min", task2.getMin());
            kvp.put("max", task2.getMax());
            kvp.put("minLat", task2.getMinLat());
            kvp.put("minLong", task2.getMinLong());
            kvp.put("maxLat", task2.getMaxLat());
            kvp.put("maxLong", task2.getMaxLong());
            logger.debug(kvp.toJSONString());
            ProcessFileTask task = new ProcessFileTask(task2.getMin(), task2.getMax());
            task.run(taskExecutor, group);
            writer.commit();
        }
        FileWriter manifest = new FileWriter("manifest.json");
        manifest.write(dataList.toJSONString());
        IOUtils.closeQuietly(manifest);
        writer.commit();
        writer.close();
    }

    private IndexWriter setupLuceneIndexWriter(String indexName) throws IOException {
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(Version.LATEST, analyzer);

        if (true) {
            // Create a new index in the directory, removing any previously indexed documents:
            iwc.setOpenMode(OpenMode.CREATE);
            // } else {
            // // Add new documents to an existing index:
            // iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
        }

        // iwc.setRAMBufferSizeMB(256.0);

        File path = new File("indexes/" + indexName);
        path.mkdirs();
        Directory dir = FSDirectory.open(path);
        IndexWriter writer = new IndexWriter(dir, iwc);
        return writer;
    }

}
