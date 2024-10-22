/*
 *    Examind community - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2022 Geomatys.
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
package org.constellation.process.service;

import java.util.Locale;
import java.util.ResourceBundle;
import org.apache.sis.util.ResourceInternationalString;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class BundleInternationalString extends ResourceInternationalString {

    public static final String BUNDLE_LOCATION = "org/constellation/process/service/bundle";

    public BundleInternationalString(String key) {
        super(key);
    }

    @Override
    protected ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle(BUNDLE_LOCATION);
    }

}
