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
package org.constellation.process.data;

import java.util.List;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.dto.DataBrief;
import org.constellation.dto.process.DatasetProcessReference;
import org.constellation.dto.process.StyleProcessReference;
import org.constellation.exception.ConfigurationException;
import org.constellation.process.AbstractCstlProcess;
import static org.constellation.process.data.DatasetStyleLinkerProcessDescriptor.DATASET;
import static org.constellation.process.data.DatasetStyleLinkerProcessDescriptor.STYLE;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DatasetStyleLinkerProcess extends AbstractCstlProcess {

    @Autowired
    private IStyleBusiness styleBusiness;

    @Autowired
    private IDataBusiness dataBusiness;

    public DatasetStyleLinkerProcess(ProcessDescriptor desc, ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {

        final StyleProcessReference style   = inputParameters.getValue(STYLE);
        final DatasetProcessReference dataset = inputParameters.getValue(DATASET);

        final List<DataBrief> briefs = dataBusiness.getDataBriefsFromDatasetId(dataset.getId());
        for (DataBrief brief : briefs) {
            try {
                styleBusiness.linkToData(style.getId(), brief.getId());
            } catch (ConfigurationException ex) {
                throw new ProcessException("Error while linking data and style", this, ex);
            }
        }

    }

}
