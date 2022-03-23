/*
 *    Examind - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2011 Geomatys.
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
package com.examind.process.sos;

import static com.examind.process.sos.SosHarvesterProcessDescriptor.*;
import static com.examind.store.observation.csvflat.CsvFlatUtils.extractCodes;
import com.opencsv.CSVReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Level;

import org.apache.commons.lang3.StringUtils;
import org.constellation.business.IProcessBusiness;
import org.constellation.dto.process.ChainProcess;
import org.constellation.dto.process.ServiceProcessReference;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
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
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
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
        final String sourceFolderStr = inputParameters.getValue(HarvesterPreProcessDescriptor.DATA_FOLDER);
        final String user            = inputParameters.getValue(HarvesterPreProcessDescriptor.USER);
        final String pwd             = inputParameters.getValue(HarvesterPreProcessDescriptor.PWD);

        final String observationType = inputParameters.getValue(HarvesterPreProcessDescriptor.OBS_TYPE);
        final String taskName        = inputParameters.getValue(HarvesterPreProcessDescriptor.TASK_NAME);
        String format                = inputParameters.getValue(HarvesterPreProcessDescriptor.FORMAT);

        final String resultColumn    = inputParameters.getValue(HarvesterPreProcessDescriptor.RESULT_COLUMN);
        final String typeColumn     = inputParameters.getValue(HarvesterPreProcessDescriptor.TYPE_COLUMN);
        final String separator      = inputParameters.getValue(HarvesterPreProcessDescriptor.SEPARATOR);
        final String charquote      = inputParameters.getValue(HarvesterPreProcessDescriptor.CHARQUOTE);

        final List<String> obsPropColumns = new ArrayList<>();
        for (GeneralParameterValue param : inputParameters.values()) {
            if (param.getDescriptor().getName().getCode().equals(HarvesterPreProcessDescriptor.OBS_PROP_COLUMN.getName().getCode())) {
                obsPropColumns.add(((ParameterValue)param).stringValue());
            }
        }

        final Set<String> codes = new HashSet<>();

        if (format == null) {
            format = "csv";
        }

        final boolean csvFlat = "csv-flat".equals(format);
        
        String ext = '.' + format;

        if (csvFlat) {
            ext = ".csv";
            if (resultColumn == null || obsPropColumns.isEmpty()) {
                throw new ProcessException("The result column, obs prop column can't be null", this);
            }
        } else {
            if (observationType == null) {
                throw new ProcessException("The observation type can't be null except for csvFlat store", this);
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
                    String[] currentHeaders = extractHeaders(child, format, separator.charAt(0));
                    if (headers != null && !Arrays.equals(headers, currentHeaders)) {
                        throw new ProcessException("Inconsistent dataset, the different files does not have the same headers", this);
                    }
                    headers = currentHeaders;

                    // extract codes
                    if (csvFlat) {
                        Set<String> currentCodes = extractCodes(child, obsPropColumns, separator.charAt(0));
                        codes.addAll(currentCodes);
                    }
                }
            } else {
                throw new ProcessException("The source folder does not point to a directory", this);
            }
        } catch (IOException | URISyntaxException | ConstellationStoreException ex) {
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

        inputs.add(new Parameter(DATASET_IDENTIFIER_NAME, String.class, DATASET_IDENTIFIER_DESC, DATASET_IDENTIFIER_DESC, 1, 1));
        inputs.add(new Parameter(OBS_TYPE_NAME,           String.class, OBS_TYPE_DESC,           OBS_TYPE_DESC, observationType != null ? 1 : 0, 1, observationType));
        
        inputs.add(new Parameter(THING_ID_NAME,           String.class, THING_ID_DESC,           THING_ID_DESC,           0, 1));
        inputs.add(new Parameter(THING_COLUMN_NAME,       String.class, THING_COLUMN_DESC,       THING_COLUMN_DESC,       0, 1, null, headers));
        inputs.add(new Parameter(THING_NAME_COLUMN_NAME,  String.class, THING_NAME_COLUMN_DESC,  THING_NAME_COLUMN_DESC,  0, 1, null, headers));
        inputs.add(new Parameter(THING_DESC_COLUMN_NAME,  String.class, THING_DESC_COLUMN_DESC,  THING_DESC_COLUMN_DESC,  0, 1, null, headers));
       
        inputs.add(new Parameter(SEPARATOR_NAME, String.class, SEPARATOR_DESC, SEPARATOR_DESC, 1, 1, separator));
        inputs.add(new Parameter(CHARQUOTE_NAME, String.class, CHARQUOTE_DESC, CHARQUOTE_DESC, charquote != null ? 1 : 0, 1, charquote));

        String defaultMainCol = null;
        if ("Timeserie".equals(observationType) || "Trajectory".equals(observationType)) {
            defaultMainCol = guessColumn(headers, Arrays.asList("time", "date"));
        }

        if (csvFlat && observationType == null) {
            inputs.add(new Parameter(Z_COLUMN_NAME, String.class, Z_COLUMN_DESC, Z_COLUMN_DESC, 1, 1, defaultMainCol, headers));
        } else {
            inputs.add(new Parameter(MAIN_COLUMN_NAME, String.class, MAIN_COLUMN_DESC, MAIN_COLUMN_DESC, 1, 1, defaultMainCol, headers));
        }
        
        inputs.add(new Parameter(DATE_COLUMN_NAME, String.class, DATE_COLUMN_DESC, DATE_COLUMN_DESC, 1, 1, guessColumn(headers, Arrays.asList("time", "date")), headers));
        inputs.add(new Parameter(DATE_FORMAT_NAME, String.class, DATE_FORMAT_DESC, DATE_FORMAT_DESC, 1, 1, "yyyy-MM-dd'T'HH:mm:ss'Z'"));

        inputs.add(new Parameter(LONGITUDE_COLUMN_NAME, String.class,  LONGITUDE_COLUMN_DESC, LONGITUDE_COLUMN_DESC, 1, 1, guessColumn(headers, Arrays.asList("longitude", "long")), headers));
        inputs.add(new Parameter(LATITUDE_COLUMN_NAME,  String.class,  LATITUDE_COLUMN_DESC,  LATITUDE_COLUMN_DESC,  1, 1, guessColumn(headers, Arrays.asList("latitude", "lat")), headers));
        inputs.add(new Parameter(FOI_COLUMN_NAME,       String.class,  FOI_COLUMN_DESC,       FOI_COLUMN_DESC,       0, 1, null, headers));
        inputs.add(new Parameter(UOM_COLUMN_NAME,       String.class,  UOM_COLUMN_DESC,       UOM_COLUMN_DESC,       0, 1, null, headers));
        inputs.add(new Parameter(UOM_REGEX_NAME,        String.class,  UOM_REGEX_DESC,        UOM_REGEX_DESC,        0, 1, null));

        inputs.add(new Parameter(REMOVE_PREVIOUS_NAME, Boolean.class, REMOVE_PREVIOUS_DESC, REMOVE_PREVIOUS_DESC, 0, 1, false));

        if ("csv".equals(format)) {

            inputs.add(new Parameter(OBS_PROP_COLUMN_NAME, String.class, OBS_PROP_COLUMN_DESC, OBS_PROP_COLUMN_DESC, 0, 92, null, headers));
            inputs.add(new Parameter(OBS_PROP_REGEX_NAME,  String.class, OBS_PROP_REGEX_DESC,  OBS_PROP_REGEX_DESC,  0, 1, null));
            inputs.add(new Parameter(STORE_ID_NAME,        String.class, STORE_ID_DESC,        STORE_ID_DESC,        1, 1, "observationCsvFile"));
            inputs.add(new Parameter(FORMAT_NAME,          String.class, FORMAT_DESC,          FORMAT_DESC,          1, 1, "text/csv; subtype=\"om\""));

        } else if (csvFlat) {

            
            final Parameter CCparam;
            if (obsPropColumns.size() == 1) {
                CCparam = new Parameter(OBS_PROP_COLUMN_NAME, String.class, OBS_PROP_COLUMN_DESC, OBS_PROP_COLUMN_DESC,  1, 1, obsPropColumns.get(0));
            } else {
                // issue here. we lost the recorded values after the first
                CCparam = new Parameter(OBS_PROP_COLUMN_NAME, String.class, OBS_PROP_COLUMN_DESC, OBS_PROP_COLUMN_DESC,  obsPropColumns.size(), obsPropColumns.size(), obsPropColumns.get(0), obsPropColumns.toArray());
            }
            inputs.add(CCparam);

            List<String> sortedCodes = new ArrayList<>(codes);
            Collections.sort(sortedCodes);
            inputs.add(new Parameter(OBS_PROP_NAME_COLUMN_NAME,    String.class, OBS_PROP_NAME_COLUMN_DESC,    OBS_PROP_NAME_COLUMN_DESC,    0, 92, null, headers));
            inputs.add(new Parameter(OBS_PROP_COLUMNS_FILTER_NAME, String.class, OBS_PROP_COLUMNS_FILTER_DESC, OBS_PROP_COLUMNS_FILTER_DESC, 0, 92, null, sortedCodes.toArray()));

            inputs.add(new Parameter(STORE_ID_NAME,                String.class, STORE_ID_DESC,                STORE_ID_DESC,                1, 1, "observationCsvFlatFile"));
            inputs.add(new Parameter(FORMAT_NAME,                  String.class, FORMAT_DESC,                  FORMAT_DESC,                  1, 1, "text/csv; subtype=\"om\""));
            inputs.add(new Parameter(RESULT_COLUMN_NAME,           String.class, RESULT_COLUMN_DESC,           RESULT_COLUMN_DESC,           1, 1, resultColumn));
            inputs.add(new Parameter(TYPE_COLUMN_NAME,             String.class, TYPE_COLUMN_DESC,             TYPE_COLUMN_DESC,             0, 1, typeColumn));
        
        } else if ("dbf".equals(format)) {
            inputs.add(new Parameter(STORE_ID_NAME, String.class, STORE_ID_DESC, STORE_ID_DESC, 1, 1, "observationDbfFile"));
            inputs.add(new Parameter(FORMAT_NAME,   String.class, FORMAT_DESC,   FORMAT_DESC,   1, 1, "application/dbase; subtype=\"om\""));
        }

        chain.setInputs(inputs);

        // no outputs for now
        final List<Parameter> outputs = new ArrayList<>();

        outputs.add(new Parameter(OBSERVATION_INSERTED_NAME, Integer.class, OBSERVATION_INSERTED_DESC, OBSERVATION_INSERTED_DESC, 0, 1));
        outputs.add(new Parameter(FILE_INSERTED_NAME,        Integer.class, FILE_INSERTED_DESC,        FILE_INSERTED_DESC,        0, 1));

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


    private String[] extractHeaders(Path dataFile, String format, char separator) throws ProcessException {
        LOGGER.log(Level.INFO, "Extracting headers from : {0}", dataFile.getFileName().toString());
        if ("csv".equals(format) || "csv-flat".equals(format)) {
            try (final CSVReader reader = new CSVReader(Files.newBufferedReader(dataFile, StandardCharsets.UTF_8), separator)) {

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
