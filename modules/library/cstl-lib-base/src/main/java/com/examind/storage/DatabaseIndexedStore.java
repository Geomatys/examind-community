/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2026 Geomatys.
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
package com.examind.storage;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;

/**
 *
 * @author glegal
 */
public abstract class DatabaseIndexedStore extends DataStore {
    
    public abstract void createOrAddToResource(Map<String, String> parameters, List<Path> dataPaths) throws DataStoreException;
    
    public abstract void removeFromResource(Map<String, String> parameters, Path dataPath) throws DataStoreException;
    
    public abstract void removeResource(Map<String, String> parameters) throws DataStoreException;
    
    public abstract void removeAllResource() throws DataStoreException;
    
}
