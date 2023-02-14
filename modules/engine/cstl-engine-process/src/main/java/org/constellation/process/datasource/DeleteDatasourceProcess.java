/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
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
package org.constellation.process.datasource;

import java.util.ArrayList;
import java.util.List;
import org.apache.sis.parameter.Parameters;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.dto.DataSource;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationRuntimeException;
import org.constellation.process.AbstractCstlProcess;
import static org.constellation.process.datasource.DeleteDatasourceDescriptor.*;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DeleteDatasourceProcess extends AbstractCstlProcess {

    @Autowired
    private IDatasourceBusiness datasourceBusiness;

    public DeleteDatasourceProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    public DeleteDatasourceProcess(final String dsPath, final Integer dsId) {
        this(INSTANCE, toParameter(dsPath, dsId));
    }

    private static ParameterValueGroup toParameter(final String dsPath, final Integer dsId) {
        Parameters params = Parameters.castOrWrap(INSTANCE.getInputDescriptor().createValue());
        params.getOrCreate(DATASOURCE_IDENTIFIER).setValue(dsId);
        params.getOrCreate(DATASOURCE_PATH).setValue(dsPath);
        return params;
    }

    @Override
    protected void execute() throws ProcessException {
        final String dsPath = inputParameters.getValue(DATASOURCE_PATH);
        final Integer dsId  = inputParameters.getValue(DATASOURCE_IDENTIFIER);

        if (!(dsId == null ^ dsPath == null))  {
            throw new ProcessException("One and only one of datasource path or datasource id must be specified", this);
        }

        final List<DataSource> sources = new ArrayList<>();
        if (dsId == null) {
            sources.addAll(datasourceBusiness.search(dsPath, null, null));
        } else {
            DataSource ds = datasourceBusiness.getDatasource(dsId);
            if (ds == null) {
                throw new ProcessException("Unexisting datasource: " + dsId, this);
            }
            sources.add(ds);
        }
        if (sources.isEmpty()) return;
        float part = 100 / sources.size();
        int i = 1;
        try {
            for (DataSource ds : sources) {
                final Integer sid = ds.getId();
                datasourceBusiness.delete(sid);
                fireProgressing("datasource " + ds.getUrl() + " removed.", part*i, false);
                i++;
                checkDismissed();
            }
        } catch (ConstellationException | ConstellationRuntimeException ex) {
            throw new ProcessException(ex.getLocalizedMessage(), this, ex);
        }
    }
}
