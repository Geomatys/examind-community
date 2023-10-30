/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
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
package com.examind.process.datacombine;

import static com.examind.process.datacombine.AbstractDataCombineDescriptor.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.process.DataProcessReference;
import org.constellation.dto.process.DatasetProcessReference;
import org.constellation.process.AbstractCstlProcess;
import static org.constellation.process.ProcessUtils.getMultipleValues;
import org.constellation.repository.DataRepository;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractDataCombineProcess extends AbstractCstlProcess {

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    protected IDataBusiness dataBusiness;

    @Autowired
    protected IProviderBusiness providerBusiness;

    public AbstractDataCombineProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    protected List<Integer> getDataIdsToCombine() throws ProcessException {
        DatasetProcessReference dataset  = inputParameters.getValue(DATASET);
        List<DataProcessReference> datas = (List<DataProcessReference>) getMultipleValues(inputParameters, DATA).stream().filter(d -> d != null).toList();

        if (dataset != null && !datas.isEmpty()) {
            throw new ProcessException("Either a list of data, a dataset should be given, not both.", this);
        } else if (dataset == null && datas.isEmpty()) {
            throw new ProcessException("No target data given for tiling operation. Either a list of data or dataset parameter must be set.", this);
        }

        List<Integer> dataIds;
        if (dataset != null) {
            dataIds = dataRepository.findByDatasetId(dataset.getId()).stream().map(f -> f.getId()).collect(Collectors.toList());
        } else {
            dataIds = new ArrayList<>();
            for (DataProcessReference dp : datas) {
                dataIds.add(dp.getId());
            }
        }

        return dataIds;
    }
}
