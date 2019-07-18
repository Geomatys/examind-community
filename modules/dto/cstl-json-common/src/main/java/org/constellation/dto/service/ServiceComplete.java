/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.dto.service;

import org.constellation.dto.contact.Details;

public class ServiceComplete extends Service {

    private String title;
    private String description;

    public ServiceComplete() {

    }

    public ServiceComplete(Service service , Details details) {
        super(service.getId(), service.getIdentifier(), service.getType(),
              service.getDate(), service.getConfig(), service.getOwner(),
              service.getStatus(),service.getVersions(), service.getImpl());
        this.description = details != null ? details.getDescription() : "";
        this.title = details != null ? details.getName() : "";
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    @Override
    public String toString() {
        return super.toString() + " description=" + description + ", title=" + title + "]";
    }

}
