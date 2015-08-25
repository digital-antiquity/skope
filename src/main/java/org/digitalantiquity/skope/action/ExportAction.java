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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Result;
import org.digitalantiquity.skope.service.lucene.LuceneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ActionSupport;

@Component
@Scope("prototype")
public class ExportAction extends ActionSupport {

    private static final long serialVersionUID = 7455769960306862309L;

    private final Logger logger = Logger.getLogger(getClass());

    @Autowired
    private transient LuceneService luceneService;

    private Double x1 = -66.005859375;
    private Double y1 = 24.17431945794909;

    private String json = "";
    private InputStream stream;
    private List<String> type = new ArrayList<>();
    private Integer startTime;
    private Integer endTime;
    private String fileName;

    @Action(value = "export", results = {
            @Result(name = SUCCESS, type = "stream", params = { "contentType", "text/csv", "inputName", "stream",
                    "contentDisposition", "attachment;filename=\"${fileName}\"" })
    })
    public String execute() throws SQLException {
        try {
            logger.debug(String.format("p:(%s,%s) %s %s ", x1, y1, startTime, endTime));
            File file = luceneService.exportData(x1, y1, startTime, endTime, getType());

            logger.debug("done request");
            setFileName(StringUtils.join(getType(),"_") + ".csv");
            stream = new FileInputStream(file);
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

    public Double getY1() {
        return y1;
    }

    public void setY1(Double y1) {
        this.y1 = y1;
    }

    public Integer getStartTime() {
        return startTime;
    }

    public void setStartTime(Integer startTime) {
        this.startTime = startTime;
    }

    public Integer getEndTime() {
        return endTime;
    }

    public void setEndTime(Integer endTime) {
        this.endTime = endTime;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<String> getType() {
        return type;
    }

    public void setType(List<String> type) {
        this.type = type;
    }

}
