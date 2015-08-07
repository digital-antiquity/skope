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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Result;
import org.digitalantiquity.skope.service.file.FileService;
import org.digitalantiquity.skope.service.lucene.LuceneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensymphony.xwork2.ActionSupport;

@Component
@Scope("prototype")
public class DetailAction extends ActionSupport {

    private static final long serialVersionUID = 1449893280868529623L;

    private final Logger logger = Logger.getLogger(getClass());

    @Autowired
    private transient LuceneService luceneService;

    @Autowired
    private transient FileService fileService;

    private Double x1 = -66.005859375;
    private Double x2 = -124.716798875;
    private Double y1 = 24.17431945794909;
    private double y2 = 49.359122687528746;

    private String json = "";
    private Integer cols = 100;
    private Integer zoom;
    private InputStream stream;
    private String indexName = "skope";
    private String type = "P";
    private int mode = 2;
    private Integer time = 0;
    @Action(value = "detail", results = {
            @Result(name = SUCCESS, type = "stream", params = { "contentType", "text/csv", "inputName", "stream"})
    })
    public String execute() throws SQLException {
        try {
            logger.debug(String.format("m: %s start (%s,%s) x(%s,%s) %s %s ", mode, x1, y1, x2, y2, cols, zoom));
            Map<String,String[]> list = luceneService.getDetails(x1, y2);
            logger.debug("done request");
            json = new ObjectMapper().writeValueAsString(list);
            stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
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

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
