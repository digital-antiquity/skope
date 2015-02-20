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

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Result;
import org.digitalantiquity.skope.service.PostGisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ActionSupport;

@Component
@Scope("prototype")
public class IndexAction extends ActionSupport {

    private static final long serialVersionUID = 6357271605459841569L;

    
    private final Logger logger = Logger.getLogger(getClass());

    @Autowired
    private transient PostGisService postGisService;


    @Action(value = "index", results = {
            @Result(name = SUCCESS, location = "index.ftl", type = "freemarker")
    })
    public String execute() throws SQLException {
        return SUCCESS;
    }

}
