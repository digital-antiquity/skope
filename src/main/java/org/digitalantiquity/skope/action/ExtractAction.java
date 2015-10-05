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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Result;
import org.digitalantiquity.skope.service.GeoTiffDataReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.opensymphony.xwork2.ActionSupport;

@Component
@Scope("prototype")
public class ExtractAction extends ActionSupport {

    private static final long serialVersionUID = 7455769960306862309L;

    private final Logger logger = Logger.getLogger(getClass());

    @Autowired
    GeoTiffDataReaderService geoTiffService;

    private Double x1 = -66.005859375;
    private Double y1 = 24.17431945794909;

    private InputStream stream;
    private List<String> type = new ArrayList<>();
    private Integer startTime;
    private Integer endTime;
    private String fileName;
    private String bounds;

    @Action(value = "extract", results = {
            @Result(name = SUCCESS, type = "stream", params = { "contentType", "image/tiff", "inputName", "stream",
                    "contentDisposition", "attachment;filename=\"${fileName}\"" })
    })
    public String execute() throws SQLException {
        try {
            logger.debug(String.format("p:(%s,%s) %s %s %s", x1, y1, startTime, endTime, bounds));
            String gjson = writeGeometry(bounds);
            File file = geoTiffService.extractData(startTime, endTime, gjson);

            logger.debug("done request");
            setFileName("clip.tiff");
            stream = new FileInputStream(file);
            logger.debug("end");
        } catch (Exception e) {
            logger.error(e, e);
        }
        return SUCCESS;
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

    public String getBounds() {
        return bounds;
    }

    public void setBounds(String geoJson) {
        this.bounds = geoJson;
    }

    private String writeGeometry(String bounds) throws IOException, JsonGenerationException {
        String[] bb = StringUtils.split(bounds, ",");
        logger.debug(String.format("%s %s , %s %s", bb[0], bb[1], bb[2], bb[3]));
        Double minLon = Double.parseDouble(bb[1]);
        Double minLat = Double.parseDouble(bb[0]);
        Double maxLon = Double.parseDouble(bb[3]);
        Double maxLat = Double.parseDouble(bb[2]);
        StringWriter sw = new StringWriter();
        JsonGenerator jgen = new JsonFactory().createJsonGenerator(sw);
        jgen.writeStartObject();
        jgen.writeStringField("type", "Polygon");
        jgen.writeFieldName("coordinates");
        jgen.writeStartArray();
        writeArrayEntry(minLat, minLon, jgen);
        writeArrayEntry(minLat, maxLon, jgen);
        writeArrayEntry(maxLat, maxLon, jgen);
        writeArrayEntry(maxLat, minLon, jgen);
        writeArrayEntry(minLat, minLon, jgen);
        jgen.writeEndArray();
        jgen.writeEndObject();
        jgen.close();
        String result = sw.getBuffer().toString();
        logger.debug(result);
        return result;
    }

    private void writeArrayEntry(Double lat, Double lon, JsonGenerator jgen) throws IOException, JsonGenerationException {
        jgen.writeStartArray();
        jgen.writeNumber(lat);
        jgen.writeNumber(lon);
        jgen.writeEndArray();
    }
}
