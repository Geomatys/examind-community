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
package org.constellation.process.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.parameter.Parameters;
import org.constellation.api.ServiceDef;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.Sensor;
import org.constellation.dto.process.DataProcessReference;
import org.constellation.dto.service.Service;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationRuntimeException;
import org.constellation.exception.NotRunningServiceException;
import org.constellation.process.AbstractCstlProcess;
import static org.constellation.process.data.DeleteDataDescriptor.INSTANCE;
import static org.constellation.process.data.DeleteDataDescriptor.DATA;
import static org.constellation.process.data.DeleteDataDescriptor.DELETE_FILES;
import org.constellation.ws.ISensorConfigurer;
import org.constellation.ws.IWSEngine;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DeleteDataProcess extends AbstractCstlProcess {

    @Autowired
    private IDataBusiness dataBusiness;

    public DeleteDataProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    public DeleteDataProcess(DataProcessReference data, final Boolean deleteFiles) {
        this(INSTANCE, toParameter(data, deleteFiles));
    }

    private static ParameterValueGroup toParameter(DataProcessReference data, final Boolean deleteFile) {
        Parameters params = Parameters.castOrWrap(INSTANCE.getInputDescriptor().createValue());
        params.getOrCreate(DATA).setValue(data);
        params.getOrCreate(DELETE_FILES).setValue(deleteFile);
        return params;
    }

    @Override
    protected void execute() throws ProcessException {
        final DataProcessReference data = inputParameters.getValue(DATA);
        final boolean removeFiles       = inputParameters.getValue(DELETE_FILES);
        try {
            dataBusiness.removeData(data.getId(), removeFiles);
        } catch (ConstellationException | ConstellationRuntimeException ex) {
            throw new ProcessException(ex.getLocalizedMessage(), this, ex);
        }
    }
}
