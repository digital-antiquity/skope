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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ActionSupport;

@Component
@Scope("prototype")
public class DownloadAction extends ActionSupport {

    private static final long serialVersionUID = 1449893280868529623L;

    private final Logger logger = Logger.getLogger(getClass());

    private String filename;
    private InputStream stream;

    @Action(value = "download", results = {
            @Result(name = SUCCESS, type = "stream", params = { "contentType", "image/tiff", "inputName", "stream",
                    "contentDisposition", "attachment;filename=\"${filename}\""})
    })
    public String execute() throws SQLException, FileNotFoundException {
        File f = new File(filename);
        File file = new File(System.getProperty("java.io.tmpdir"), f.getName());
        logger.debug(filename);
        if (file.exists()) {
            logger.debug("file exists: " + file);
            setStream(new BufferedInputStream(new FileInputStream(file)));
        }
        logger.debug("done");
        return SUCCESS;
    }

    public InputStream getStream() {
        return stream;
    }

    public void setStream(InputStream stream) {
        this.stream = stream;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
