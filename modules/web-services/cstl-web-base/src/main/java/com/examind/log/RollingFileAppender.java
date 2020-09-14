/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
package com.examind.log;

import java.io.File;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;

/**
 * Override the logback class to support environement variable cstl home.
 *
 * @author Guilhem legal (Geomatys)
 */
public class RollingFileAppender extends ch.qos.logback.core.rolling.RollingFileAppender {

    @Override
    public void setFile(String file) {
        String path = file;
        if (path.contains("cstl.home_IS_UNDEFINED")) {
            String exaHome = Application.getProperty(AppProperty.CSTL_HOME, System.getProperty("user.home") + File.separator + ".constellation");
            path = path.replace("cstl.home_IS_UNDEFINED", exaHome);
            super.fileName = path;
        }
        super.setFile(path);
    }
}
