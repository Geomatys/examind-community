/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2025 Geomatys.
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
package org.constellation.business;

import com.examind.dto.fs.Collection;
import com.examind.dto.fs.Service;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.constellation.dto.Data;
import org.constellation.exception.ConstellationException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface IFileSystemSetupBusiness {
    
    void installDatas(boolean async);
    
    void performDiff(boolean async);
    
    void asyncServiceReload(String dataset, Map<String, List<Service>> asyncInfos) throws ConstellationException;
    
    List<Data> getDataFromCollection(Collection col, boolean async) throws ConstellationException;
    
    void handleProvidersChanges(Path providerFilePath, Integer datasourceFileId) throws ConstellationException;
}
