/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package com.examind.repository.filesystem;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.configuration.ConfigDirectory;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractFileSystemRepository {

    protected static final Logger LOGGER = Logging.getLogger("com.examind.repository.filesystem");

    protected MarshallerPool pool;

    protected int currentId = 1;

    public AbstractFileSystemRepository(Class... context) {
        ConfigDirectory.init();
        try {
            pool = new MarshallerPool(JAXBContext.newInstance(context), Collections.EMPTY_MAP);
        } catch (JAXBException ex) {
            Logger.getLogger(AbstractFileSystemRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
