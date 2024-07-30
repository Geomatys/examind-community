/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2024 Geomatys.
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
package com.examind.process.sos;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import org.geotoolkit.nio.IOUtilities;
import org.opengis.geometry.Envelope;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class STADownloaderUtils {
    
    public static <T> T readResponse(final URL url, Class<T> clazz) throws IOException {
        URLConnection conec = url.openConnection();
        InputStream is;
        int returnCode = ((HttpURLConnection)conec).getResponseCode();
        is = switch (returnCode) {
            case 200 -> conec.getInputStream();
            case 404 -> throw new IOException("The url respond with a 404 code");
            default  -> {
                InputStream eis = ((HttpURLConnection)conec).getErrorStream();
                String errorContent = IOUtilities.toString(eis);
                throw new IOException("The url respond with a " + returnCode + " code.\ncontent:" + errorContent);
            }
        };
        return new ObjectMapper().readValue(is, clazz);
    }
    
    public static String buildBboxFilter(Envelope bounds) {
        StringBuilder sb = new StringBuilder();
        double minx = bounds.getMinimum(0);
        double maxx = bounds.getMaximum(0);
        double miny = bounds.getMinimum(1);
        double maxy = bounds.getMaximum(1);
        sb.append("(st_contains(location, geography'POLYGON ((");

        sb.append(minx).append(" ").append(miny).append(",");
        sb.append(maxx).append(" ").append(miny).append(",");
        sb.append(maxx).append(" ").append(maxy).append(",");
        sb.append(minx).append(" ").append(maxy).append(",");
        sb.append(minx).append(" ").append(miny).append("))'))");
        
        return sb.toString();
    }
    
    public static String buildEntityFilter(List<String> entityIds, String relativeLocation) {
        StringBuilder filter = new StringBuilder();
        filter.append("(");
        for (int i = 0; i < entityIds.size(); i++) {
            String entityId = entityIds.get(i);
            if (i != 0) {
                filter.append(" or ");
            }
            filter.append(relativeLocation).append(" eq '").append(entityId).append("'");
        }
        filter.append(")");
        return filter.toString();
    }
}
