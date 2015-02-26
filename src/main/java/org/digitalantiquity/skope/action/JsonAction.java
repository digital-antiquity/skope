/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.digitalantiquity.skope.action;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Result;
import org.digitalantiquity.skope.service.LuceneService;
import org.digitalantiquity.skope.service.PostGisService;
import org.geojson.FeatureCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensymphony.xwork2.ActionSupport;

@Component
@Scope("prototype")
public class JsonAction extends ActionSupport {

    private static final long serialVersionUID = 1449893280868529623L;

    private final Logger logger = Logger.getLogger(getClass());

    @Autowired
    private transient PostGisService postGisService;

    @Autowired
    private transient LuceneService luceneService;
    
    private Double x1 = -66.005859375;
    private Double x2 = -124.716798875;
    private Double y1 = 24.17431945794909;
    private double y2 = 49.359122687528746;

    private String json = "";
    private Integer cols = 100;
    private Integer zoom;
    private InputStream stream;

    @Action(value = "json", results = {
            @Result(name = SUCCESS, type = "stream", params = { "contentType", "application/json", "inputName", "stream" })
    })
    public String execute() throws SQLException {
        try {
            logger.debug(String.format("start (%s,%s) x(%s,%s) %s %s ", x1, y1, x2, y2, cols, zoom));
            FeatureCollection featureList = luceneService.searchBbox(x1, y1, x2, y2, cols);
            logger.debug("done lucene");
//            FeatureCollection featureList = postGisService.test(getX1(), getY1(), getX2(), getY2(), getCols());
            json = new ObjectMapper().writeValueAsString(featureList);
            logger.debug(json);
            stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            logger.debug("end");
        } catch (Exception e) {
            logger.error(e, e);
        }
        return SUCCESS;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public Integer getCols() {
        return cols;
    }

    public void setCols(Integer cols) {
        this.cols = cols;
    }

    public InputStream getStream() {
        return stream;
    }

    public void setStream(InputStream stream) {
        this.stream = stream;
    }

    public Double getX1() {
        return x1;
    }

    public void setX1(Double x1) {
        this.x1 = x1;
    }

    public Double getX2() {
        return x2;
    }

    public void setX2(Double x2) {
        this.x2 = x2;
    }

    public Double getY1() {
        return y1;
    }

    public void setY1(Double y1) {
        this.y1 = y1;
    }

    public double getY2() {
        return y2;
    }

    public void setY2(double y2) {
        this.y2 = y2;
    }

    public Integer getZoom() {
        return zoom;
    }

    public void setZoom(Integer zoom) {
        this.zoom = zoom;
    }
}
