package org.digitalantiquity.skope.service.lucene;

import java.sql.Types;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TopDocs;
import org.digitalantiquity.skope.service.DoubleWrapper;
import org.digitalantiquity.skope.service.FeatureHelper;
import org.digitalantiquity.skope.service.IndexFields;
import org.geojson.Feature;
import org.postgis.Point;
import org.postgis.Polygon;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;

import com.github.davidmoten.geo.Coverage;
import com.github.davidmoten.geo.GeoHash;

public class EnvelopeQuerySubTask implements Runnable {
    String sql = "select avg(grid_code) from prism where ST_makeEnvelope(?, ?,?,?,4326) && geom";
    private final Logger logger = Logger.getLogger(getClass());

    private PreparedStatementCreatorFactory pcsf;

    private Polygon poly;
    private Feature feature;
    private EnvelopeQueryTask task;
    private int year;
    private int level;
    private LuceneService service;

    public EnvelopeQuerySubTask(EnvelopeQueryTask task,Polygon poly, LuceneService service, int level, int year) {
        this.task = task;
        this.poly = poly;
        this.service = service;
        this.level = level;
        this.year = year;
        pcsf = new PreparedStatementCreatorFactory(sql, Types.DOUBLE, Types.DOUBLE, Types.DOUBLE, Types.DOUBLE);
    }

    @Override
    public void run() {
        try {
        Point p1 = poly.getPoint(0);
        Point p2 = poly.getPoint(2);
        Coverage coverage = GeoHash.coverBoundingBoxMaxHashes(Math.max(p1.y, p2.y), Math.min(p1.x, p2.x), Math.min(p1.y, p2.y), Math.max(p1.x, p2.x), 40);
        BooleanQuery bq = service.buildLuceneQuery(level, coverage.getHashes(), year, false);
        TopDocs search = service.getSearcher().search(bq, null, 10000000);
        if (search.totalHits == 0) {
            return;
        }
        logger.debug(bq.toString() + " (" + search.totalHits + ")");

        DoubleWrapper doubleWrapper = null;

        for (int i = 0; i < search.scoreDocs.length; i++) {
            Document document = service.getReader().document(search.scoreDocs[i].doc);
            if (doubleWrapper == null) {
                doubleWrapper = new DoubleWrapper();
            }
            doubleWrapper.increment(Double.parseDouble(document.get(IndexFields.CODE)));
        }
        Double avg = null;
        if (doubleWrapper != null) {
            avg = doubleWrapper.getAverage();
            logger.trace("adding " + avg + " for: " + poly);
            feature = FeatureHelper.createFeature(poly, avg);
        }

        task.getFeatureCollection().add(feature);
        setFeature(feature);
        } catch (Exception e) {
            logger.error(e,e);
        }
    }

    
    
    public Feature getFeature() {
        return feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

}
