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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Level;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.constellation.business.IProcessBusiness;
import org.constellation.dto.process.ChainProcess;
import org.constellation.dto.process.ServiceProcessReference;
import org.constellation.exception.ConstellationException;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.ChainProcessRetriever;
import org.constellation.process.ExamindProcessFactory;
import org.constellation.util.FileSystemReference;
import org.constellation.util.FileSystemUtilities;
import org.geotoolkit.data.dbf.DbaseFileHeader;
import org.geotoolkit.data.dbf.DbaseFileReader;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.processing.chain.model.Chain;
import org.geotoolkit.processing.chain.model.Constant;
import static org.geotoolkit.processing.chain.model.Element.BEGIN;
import static org.geotoolkit.processing.chain.model.Element.END;
import org.geotoolkit.processing.chain.model.ElementProcess;
import org.geotoolkit.processing.chain.model.Parameter;
import org.geotoolkit.processing.chain.model.StringMapList;
import org.geotoolkit.processing.chain.model.StringList;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class HarvesterPreProcess extends AbstractCstlProcess {

    @Autowired
    private IProcessBusiness processBusiness;

    private final static Map<String, String> codesMeasure;

    static {
        codesMeasure = new HashMap<>();
        codesMeasure.put("30", "measure1");
        codesMeasure.put("35", "measure2");
        codesMeasure.put("66", "measure3");
        codesMeasure.put("70", "measure4");
        codesMeasure.put("64", "measure5");
        codesMeasure.put("65", "measure6");
        codesMeasure.put("169", "measure7");
        codesMeasure.put("193", "measure8");
        codesMeasure.put("577", "measure9");
        codesMeasure.put("584", "measure10");
    }

    public HarvesterPreProcess(final ProcessDescriptor desc, final ParameterValueGroup input) {
        super(desc,input);
    }

    @Override
    protected void execute() throws ProcessException {
        final String sourceFolderStr = inputParameters.getValue(HarvesterPreProcessDescriptor.DATA_FOLDER);
        final String user            = inputParameters.getValue(HarvesterPreProcessDescriptor.USER);
        final String pwd             = inputParameters.getValue(HarvesterPreProcessDescriptor.PWD);

        final String observationType = inputParameters.getValue(HarvesterPreProcessDescriptor.OBS_TYPE);
        final String taskName        = inputParameters.getValue(HarvesterPreProcessDescriptor.TASK_NAME);
        String format                = inputParameters.getValue(HarvesterPreProcessDescriptor.FORMAT);

        final String valueColumn    = inputParameters.getValue(HarvesterPreProcessDescriptor.VALUE_COLUMN);
        final String codeColumn     = inputParameters.getValue(HarvesterPreProcessDescriptor.CODE_COLUMN);
        final String typeColumn     = inputParameters.getValue(HarvesterPreProcessDescriptor.TYPE_COLUMN);
        final String separator      = inputParameters.getValue(HarvesterPreProcessDescriptor.SEPARATOR);
        final Set<String> codes = new HashSet<>();

        if (format == null) {
            format = "csv";
        }

        final boolean coriolis = "csv-coriolis".equals(format);
        
        String ext = '.' + format;

        if (coriolis) {
            ext = ".csv";
            if (valueColumn == null || codeColumn == null || typeColumn == null) {
                throw new ProcessException("The value column, code column or type column can't be null", this);
            }
        } else {
            if (observationType == null) {
                throw new ProcessException("The observation type can't be null except for coriolis store", this);
            }
        }

        String processId;
        if (taskName != null) {
            processId = taskName;
            if (processBusiness.getChainProcess("examind-dynamic", taskName) != null) {
                throw new ProcessException("The dynamic task: " + processId + " already exist.", this);
            }
        } else {
            String uniqueId = UUID.randomUUID().toString();
            processId = "csv-sos-insertion-" + uniqueId;
        }

        final Chain chain = new Chain(processId);

        chain.setTitle("SOS Harvester");
        chain.setAbstract("SOS Harvester");

        int id  = 1;

        String harvesterProcessName = SosHarvesterProcessDescriptor.NAME;

        String[] headers = null;

        FileSystemReference fs = null;
        try {
            final URI dataUri = URI.create(sourceFolderStr);
            fs = FileSystemUtilities.getFileSystem(dataUri.getScheme(), sourceFolderStr, user, pwd, null, true);

            Path sourceFolder = FileSystemUtilities.getPath(fs, dataUri.getPath());

            List<Path> children = new ArrayList<>();
            if (Files.isDirectory(sourceFolder)) {
                 try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceFolder)) {
                    for (Path child : stream) {
                        if (child.getFileName().toString().endsWith(ext) ||
                            child.getFileName().toString().endsWith(ext.toUpperCase())) {
                            children.add(child);
                        }
                    }
                } catch (IOException e) {
                    throw new ProcessException("Error occurs during directory browsing", this, e);
                }

                for (Path child : children) {
                    String[] currentHeaders = extractHeaders(child, format);
                    if (headers != null && !Arrays.equals(headers, currentHeaders)) {
                        throw new ProcessException("Inconsistent dataset, the different files does not have the same headers", this);
                    }
                    headers = currentHeaders;

                    // extract codes
                    if (coriolis) {
                        Set<String> currentCodes = extractCodes(child, codeColumn);
                        codes.addAll(currentCodes);
                    }
                }
            } else {
                throw new ProcessException("The source folder does not point to a directory", this);
            }
        } catch (IOException | URISyntaxException ex) {
            throw new ProcessException("Error while opening data location.", this, ex);
        } finally {
            FileSystemUtilities.closeFileSystem(fs);
        }


        //input/out/constants parameters

        final Constant src     = chain.addConstant(id++, String.class, sourceFolderStr);
        final Constant userSrc = chain.addConstant(id++, String.class, user);
        final Constant pwdSrc  = chain.addConstant(id++, String.class, pwd);

        final List<Parameter> inputs = new ArrayList<>();

        final Parameter DRparam = new Parameter(REMOTE_READ_NAME, Boolean.class, REMOTE_READ_DESC, REMOTE_READ_DESC, 0, 1);
        inputs.add(DRparam);

         final Parameter SIDparam = new Parameter(SERVICE_ID_NAME, ServiceProcessReference.class, SERVICE_ID_DESC, SERVICE_ID_DESC, 1, 92);
        // not using Collections.singletonMap() because of marshalling issue
        Map<String, Object> userMap = new HashMap<>();
        HashMap<String, StringList> typeMap = new HashMap<>();
        ArrayList<String> list = new ArrayList<>();
        list.add("sos");
        list.add("sts");
        typeMap.put("type", new StringList(list));
        userMap.put("filter", new StringMapList(typeMap));
        SIDparam.setUserMap(userMap);
        inputs.add(SIDparam);

        final Parameter DSparam = new Parameter(DATASET_IDENTIFIER_NAME, String.class, DATASET_IDENTIFIER_DESC, DATASET_IDENTIFIER_DESC, 1, 1);
        inputs.add(DSparam);

        final Parameter OTparam = new Parameter(OBS_TYPE_NAME, String.class, OBS_TYPE_DESC, OBS_TYPE_DESC, observationType != null ? 1 : 0, 1, observationType);
        inputs.add(OTparam);

        final Parameter PRparam = new Parameter(PROCEDURE_ID_NAME, String.class, PROCEDURE_ID_DESC, PROCEDURE_ID_DESC, 0, 1);
        inputs.add(PRparam);

        final Parameter PCparam = new Parameter(PROCEDURE_COLUMN_NAME, String.class, PROCEDURE_COLUMN_DESC, PROCEDURE_COLUMN_DESC, 0, 1, null, headers);
        inputs.add(PCparam);

        final Parameter SPparam = new Parameter(SEPARATOR_NAME, String.class, SEPARATOR_DESC, SEPARATOR_DESC, 1, 1, separator);
        inputs.add(SPparam);

        String defaultMainCol = null;
        if ("Timeserie".equals(observationType) || "Trajectory".equals(observationType)) {
            defaultMainCol = guessColumn(headers, Arrays.asList("time", "date"));
        }

        if (coriolis && observationType == null) {
            final Parameter MCparam = new Parameter(Z_COLUMN_NAME, String.class, Z_COLUMN_DESC, Z_COLUMN_DESC, 1, 1, defaultMainCol, headers);
            inputs.add(MCparam);
        } else {
            final Parameter MCparam = new Parameter(MAIN_COLUMN_NAME, String.class, MAIN_COLUMN_DESC, MAIN_COLUMN_DESC, 1, 1, defaultMainCol, headers);
            inputs.add(MCparam);
        }
        
        final Parameter DCparam = new Parameter(DATE_COLUMN_NAME, String.class, DATE_COLUMN_DESC, DATE_COLUMN_DESC, 1, 1, guessColumn(headers, Arrays.asList("time", "date")), headers);
        inputs.add(DCparam);

        final Parameter DFparam = new Parameter(DATE_FORMAT_NAME, String.class, DATE_FORMAT_DESC, DATE_FORMAT_DESC, 1, 1, "yyyy-MM-dd'T'HH:mm:ss'Z'");
        inputs.add(DFparam);

        final Parameter LatCparam = new Parameter(LONGITUDE_COLUMN_NAME, String.class, LONGITUDE_COLUMN_DESC, LONGITUDE_COLUMN_DESC, 1, 1, guessColumn(headers, Arrays.asList("longitude", "long")), headers);
        inputs.add(LatCparam);

        final Parameter LonCparam = new Parameter(LATITUDE_COLUMN_NAME, String.class, LATITUDE_COLUMN_DESC, LATITUDE_COLUMN_DESC, 1, 1, guessColumn(headers, Arrays.asList("latitude", "lat")), headers);
        inputs.add(LonCparam);

        final Parameter FCparam = new Parameter(FOI_COLUMN_NAME, String.class, FOI_COLUMN_DESC, FOI_COLUMN_DESC, 0, 1, null, headers);
        inputs.add(FCparam);

        if (coriolis) {
            final Parameter MCSparam = new Parameter(MEASURE_COLUMNS_NAME, String.class, MEASURE_COLUMNS_DESC, MEASURE_COLUMNS_DESC, 0, 92, null, codes.toArray());
            inputs.add(MCSparam);
        } else {
            final Parameter MCSparam = new Parameter(MEASURE_COLUMNS_NAME, String.class, MEASURE_COLUMNS_DESC, MEASURE_COLUMNS_DESC, 0, 92, null, headers);
            inputs.add(MCSparam);
        }

        final Parameter RPparam = new Parameter(REMOVE_PREVIOUS_NAME, Boolean.class, REMOVE_PREVIOUS_DESC, REMOVE_PREVIOUS_DESC, 0, 1, false);
        inputs.add(RPparam);

        final Parameter EUparam = new Parameter(EXTRACT_UOM_NAME, Boolean.class, EXTRACT_UOM_DESC, EXTRACT_UOM_DESC, 0, 1, false);
        inputs.add(EUparam);

        if ("csv".equals(format)) {
            final Parameter SIparam = new Parameter(STORE_ID_NAME, String.class, STORE_ID_DESC, STORE_ID_DESC, 1, 1, "observationCsvFile");
            inputs.add(SIparam);

            final Parameter FOparam = new Parameter(FORMAT_NAME, String.class, FORMAT_DESC, FORMAT_DESC, 1, 1, "text/csv; subtype=\"om\"");
            inputs.add(FOparam);
        } else if (coriolis) {
            final Parameter SIparam = new Parameter(STORE_ID_NAME, String.class, STORE_ID_DESC, STORE_ID_DESC, 1, 1, "observationCsvCoriolisFile");
            inputs.add(SIparam);

            final Parameter FOparam = new Parameter(FORMAT_NAME, String.class, FORMAT_DESC, FORMAT_DESC, 1, 1, "text/csv; subtype=\"om\"");
            inputs.add(FOparam);
            
            final Parameter VCparam = new Parameter(VALUE_COLUMN_NAME, String.class, VALUE_COLUMN_DESC, VALUE_COLUMN_DESC, 1, 1, valueColumn);
            inputs.add(VCparam);

            final Parameter CCparam = new Parameter(CODE_COLUMN_NAME, String.class, CODE_COLUMN_DESC, CODE_COLUMN_DESC, 1, 1, codeColumn);
            inputs.add(CCparam);

            final Parameter TCparam = new Parameter(TYPE_COLUMN_NAME, String.class, TYPE_COLUMN_DESC, TYPE_COLUMN_DESC, 1, 1, typeColumn);
            inputs.add(TCparam);
        
        } else if ("dbf".equals(format)) {
            final Parameter SIparam = new Parameter(STORE_ID_NAME, String.class, STORE_ID_DESC, STORE_ID_DESC, 1, 1, "observationDbfFile");
            inputs.add(SIparam);

            final Parameter FOparam = new Parameter(FORMAT_NAME, String.class, FORMAT_DESC, FORMAT_DESC, 1, 1, "application/dbase; subtype=\"om\"");
            inputs.add(FOparam);
        }

        chain.setInputs(inputs);


        // no outputs for now
        final List<Parameter> outputs = new ArrayList<>();

        final Parameter oparam1 = new Parameter(OBSERVATION_INSERTED_NAME, Integer.class, OBSERVATION_INSERTED_DESC, OBSERVATION_INSERTED_DESC, 0, 1);
        outputs.add(oparam1);

        final Parameter oparam2 = new Parameter(FILE_INSERTED_NAME, Integer.class, FILE_INSERTED_DESC, FILE_INSERTED_DESC, 0, 1);
        outputs.add(oparam2);

        chain.setOutputs(outputs);


        //chain blocks
        final ElementProcess dock = chain.addProcessElement(id++, ExamindProcessFactory.NAME, harvesterProcessName);

        chain.addFlowLink(BEGIN.getId(), dock.getId());
        chain.addFlowLink(dock.getId(), END.getId());


        //data flow links
        chain.addDataLink(src.getId(),     "", dock.getId(), SosHarvesterProcessDescriptor.DATA_FOLDER_NAME);
        chain.addDataLink(userSrc.getId(), "", dock.getId(), SosHarvesterProcessDescriptor.USER_NAME);
        chain.addDataLink(pwdSrc.getId(),  "", dock.getId(), SosHarvesterProcessDescriptor.PWD_NAME);

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


    private String[] extractHeaders(Path dataFile, String format) throws ProcessException {
        LOGGER.log(Level.INFO, "Extracting headers from : {0}", dataFile.getFileName().toString());
        if ("csv".equals(format) || "csv-coriolis".equals(format)) {
            try (final CSVReader reader = new CSVReader(Files.newBufferedReader(dataFile))) {

                final Iterator<String[]> it = reader.iterator();

                // at least one line is expected to contain headers information
                if (it.hasNext()) {

                    // read headers
                    final String[] headers = it.next();
                    return headers;
                }
                throw new ProcessException("csv headers not found", this);
            } catch (IOException ex) {
                throw new ProcessException("problem reading csv file", this, ex);
            }
        } else {
            try (SeekableByteChannel sbc = Files.newByteChannel(dataFile, StandardOpenOption.READ)){

                final DbaseFileReader reader  = new DbaseFileReader(sbc, true, null);
                final DbaseFileHeader headers = reader.getHeader();

                final String[] results = new String[headers.getNumFields()];
                for (int i = 0; i < headers.getNumFields(); i++) {
                    results[i] = headers.getFieldName(i);
                }
                return results;

            } catch (IOException ex) {
                throw new ProcessException("problem reading dbf file", this, ex);
            }
        }
    }

    private Set<String> extractCodes(Path dataFile, String measureCode) throws ProcessException {
        try (final CSVReader reader = new CSVReader(Files.newBufferedReader(dataFile))) {

            final Iterator<String[]> it = reader.iterator();

            // at least one line is expected to contain headers information
            if (it.hasNext()) {

                // read headers
                final String[] headers = it.next();
                int measureCodeIndex = -1;
                final Set<String> storeCode = new HashSet<>();
                final Set<String> noLabelCodes = new HashSet<>();

                // find measureCodeIndex
                for (int i = 0; i < headers.length; i++) {
                    final String header = headers[i];

                    if (header.equals(measureCode)) {
                        measureCodeIndex = i;
                    }
                }

                if (measureCodeIndex == -1) {
                    throw new ProcessException("csv headers does not contains Measure Code parameter.", this);
                }

                // extract all codes
                while (it.hasNext()) {
                    final String[] line = it.next();
                    final String nextCode = line[measureCodeIndex];

                    if (nextCode == null || nextCode.isEmpty()) continue;

                    final String mcl = codesMeasure.get(nextCode);

                    if (mcl == null) noLabelCodes.add(nextCode);

                    storeCode.add(mcl);
                }

                for (String nlc: noLabelCodes) {
                    LOGGER.log(Level.WARNING, String.format("No label mapping for the code: %s", nlc));
                }

                return storeCode;
            }
            throw new ProcessException("csv headers not found", this);
        } catch (IOException ex) {
            throw new ProcessException("problem reading csv file", this, ex);
        }
    }

    private String guessColumn(String[] headers, List<String> findValues) {
        if (headers != null) {
            for (String findValue : findValues) {
                for (String header : headers) {
                    if (StringUtils.containsIgnoreCase(header, findValue)) {
                        return header;
                    }
                }
            }
        }
        return null;
    }

}
