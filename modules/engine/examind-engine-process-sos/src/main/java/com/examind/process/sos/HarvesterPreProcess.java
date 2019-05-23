/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package com.examind.process.sos;

import static com.examind.process.sos.SosHarvesterProcessDescriptor.*;
import com.opencsv.CSVReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.constellation.business.IProcessBusiness;
import org.constellation.dto.process.ChainProcess;
import org.constellation.dto.process.ServiceProcessReference;
import org.constellation.exception.ConstellationException;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.ChainProcessRetriever;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.processing.chain.model.Chain;
import org.geotoolkit.processing.chain.model.Constant;
import org.geotoolkit.processing.chain.model.StringMap;
import static org.geotoolkit.processing.chain.model.Element.BEGIN;
import static org.geotoolkit.processing.chain.model.Element.END;
import org.geotoolkit.processing.chain.model.ElementProcess;
import org.geotoolkit.processing.chain.model.Parameter;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class HarvesterPreProcess extends AbstractCstlProcess {

    @Autowired
    private IProcessBusiness processBusiness;

    public HarvesterPreProcess(final ProcessDescriptor desc, final ParameterValueGroup input) {
        super(desc,input);
    }

    @Override
    protected void execute() throws ProcessException {
        final String sourceFolderStr    = inputParameters.getValue(HarvesterPreProcessDescriptor.DATA_FOLDER);
        final String observationType = inputParameters.getValue(HarvesterPreProcessDescriptor.OBS_TYPE);

        String uniqueId = UUID.randomUUID().toString();
        String processId = "csv-sos-insertion-" + uniqueId;
        final Chain chain = new Chain(processId);

        chain.setTitle("SOS Harvester");
        chain.setAbstract("SOS Harvester");

        int id  = 1;

        String harvesterProcessName = SosHarvesterProcessDescriptor.NAME;

        String[] headers = null;

        Path sourceFolder = Paths.get(sourceFolderStr);
        if (Files.isDirectory(sourceFolder)) {
             try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceFolder)) {
                for (Path child : stream) {
                    if (child.getFileName().toString().endsWith(".csv")) {
                        String[] currentHeaders = extractHeaders(child);
                        if (headers != null && !Arrays.equals(headers, currentHeaders)) {
                            throw new ProcessException("Inconsistent dataset, the diferent CSV files does not have the same headers", this);
                        }
                        headers = currentHeaders;
                    }
                }
            } catch (IOException e) {
                throw new ProcessException("Error occurs during directory browsing", this, e);
            }
        } else {
            throw new ProcessException("The source folder does not point to a directory", this);
        }


        //input/out/constants parameters

        final Constant c = chain.addConstant(id++, String.class, sourceFolderStr);

        final List<Parameter> inputs = new ArrayList<>();

        final Parameter SIDparam = new Parameter(SERVICE_ID_NAME, ServiceProcessReference.class, SERVICE_ID_DESC, SERVICE_ID_DESC, 1, 1);
        // not using Collections.singletonMap() because of marshalling issue
        Map<String, Object> userMap = new HashMap<>();
        HashMap<String, String> typeMap = new HashMap<>();
        typeMap.put("type", "sos");
        userMap.put("filter", new StringMap(typeMap));
        SIDparam.setUserMap(userMap);
        inputs.add(SIDparam);

        final Parameter DSparam = new Parameter(DATASET_IDENTIFIER_NAME, String.class, DATASET_IDENTIFIER_DESC, DATASET_IDENTIFIER_DESC, 1, 1);
        inputs.add(DSparam);

        final Parameter OTparam = new Parameter(OBS_TYPE_NAME, String.class, OBS_TYPE_DESC, OBS_TYPE_DESC, 1, 1, observationType);
        inputs.add(OTparam);

        final Parameter SPparam = new Parameter(SEPARATOR_NAME, String.class, SEPARATOR_DESC, SEPARATOR_DESC, 1, 1, ",");
        inputs.add(SPparam);

        final Parameter MCparam = new Parameter(MAIN_COLUMN_NAME, String.class, MAIN_COLUMN_DESC, MAIN_COLUMN_DESC, 1, 1, null, headers);
        inputs.add(MCparam);

        final Parameter DCparam = new Parameter(DATE_COLUMN_NAME, String.class, DATE_COLUMN_DESC, DATE_COLUMN_DESC, 1, 1, null, headers);
        inputs.add(DCparam);

        final Parameter DFparam = new Parameter(DATE_FORMAT_NAME, String.class, DATE_FORMAT_DESC, DATE_FORMAT_DESC, 1, 1, "yyyy-MM-dd'T'hh:mm:ss'Z'");
        inputs.add(DFparam);

        final Parameter LatCparam = new Parameter(LONGITUDE_COLUMN_NAME, String.class, LONGITUDE_COLUMN_DESC, LONGITUDE_COLUMN_DESC, 1, 1, null, headers);
        inputs.add(LatCparam);

        final Parameter LonCparam = new Parameter(LATITUDE_COLUMN_NAME, String.class, LATITUDE_COLUMN_DESC, LATITUDE_COLUMN_DESC, 1, 1, null, headers);
        inputs.add(LonCparam);

        final Parameter FCparam = new Parameter(FOI_COLUMN_NAME, String.class, FOI_COLUMN_DESC, FOI_COLUMN_DESC, 0, 1, null, headers);
        inputs.add(FCparam);

        final Parameter MCSparam = new Parameter(MEASURE_COLUMNS_NAME, String.class, MEASURE_COLUMNS_DESC, MEASURE_COLUMNS_DESC, 0, 92, null, headers);
        inputs.add(MCSparam);

        chain.setInputs(inputs);


        // no outputs for now
        final List<Parameter> outputs = new ArrayList<>();

        //final Parameter param = new Parameter(out.getIdentifier().getValue(), type, out.getFirstTitle(), out.getFirstAbstract(), 0, 1);
        //outputs.add(param);

        chain.setOutputs(outputs);


        //chain blocks
        final ElementProcess dock = chain.addProcessElement(id++, ExamindProcessFactory.NAME, harvesterProcessName);

        chain.addFlowLink(BEGIN.getId(), dock.getId());
        chain.addFlowLink(dock.getId(), END.getId());


        //data flow links
        chain.addDataLink(c.getId(), "", dock.getId(), SosHarvesterProcessDescriptor.DATA_FOLDER_NAME);

        for (Parameter in : inputs) {
            chain.addDataLink(BEGIN.getId(), in.getCode(),  dock.getId(), in.getCode());
        }

        for (Parameter out : outputs) {
            chain.addDataLink(dock.getId(), out.getCode(),  END.getId(), out.getCode());
        }

        try {
            ChainProcess cp = ChainProcessRetriever.convertToDto(chain);
            processBusiness.createChainProcess(cp);
        } catch (ConstellationException ex) {
            throw new ProcessException("Error while creating chain", this, ex);
        }

        outputParameters.getOrCreate(HarvesterPreProcessDescriptor.PROCESS_ID).setValue(processId);
    }


    private String[] extractHeaders(Path csvFile) throws ProcessException {
           try (final CSVReader reader = new CSVReader(Files.newBufferedReader(csvFile))) {

            final Iterator<String[]> it = reader.iterator();

            // at least one line is expected to contain headers information
            if (it.hasNext()) {

                // read headers
                final String[] headers = it.next();
                return headers;
            }
            throw new ProcessException("csv headers not found", this);
        } catch (IOException  ex) {
            throw new ProcessException("problem reading csv file", this, ex);
        }
    }
}
