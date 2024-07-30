/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2024 Geomatys.
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

import static com.examind.process.download.UrlDownloaderProcessDescriptor.FILE_OUTPUT;
import static com.examind.process.sos.STADownloaderDescriptor.*;
import static com.examind.process.sos.STADownloaderUtils.readResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.constellation.process.AbstractCstlProcess;
import static org.constellation.process.ProcessUtils.getMultipleValues;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.nio.ZipUtilities;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.sts.json.Datastream;
import org.geotoolkit.sts.json.HistoricalLocation;
import org.geotoolkit.sts.json.HistoricalLocationsResponse;
import org.geotoolkit.sts.json.Location;
import org.geotoolkit.sts.json.Observation;
import org.geotoolkit.sts.json.ObservationsResponse;
import org.geotoolkit.sts.json.ObservedProperty;
import org.geotoolkit.sts.json.Thing;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class STADownloaderProcess extends AbstractCstlProcess {
    
    private static DateFormat PARSER = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
    static {
        PARSER.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    public STADownloaderProcess(final ProcessDescriptor desc, final ParameterValueGroup input) {
        super(desc,input);
    }

    @Override
    protected void execute() throws ProcessException {
        final String staUrl              = inputParameters.getValue(STA_URL);
        final List<String> thingIds      = getMultipleValues(inputParameters, THING_ID);
        final List<String> observedProps = getMultipleValues(inputParameters, OBSERVED_PROPERTY);
        final Envelope bounds            = inputParameters.getValue(BOUNDARY);
        
        final String outputFormat        = inputParameters.getValue(OUTPUT_FORMAT);
        final Boolean compress           = inputParameters.getValue(COMPRESS);
        
        boolean toCsvFlat = outputFormat.equals("application/csv");
        Filters filter = new Filters();
        
        if (bounds != null) {
            filter.addFilter();
            filter.append(STADownloaderUtils.buildBboxFilter(bounds));
        }
        
        if (!thingIds.isEmpty()) {
            filter.addFilter();
            filter.obsFilter.append(STADownloaderUtils.buildEntityFilter(thingIds, "Datastream/Thing/id"));
            filter.hloFilter.append(STADownloaderUtils.buildEntityFilter(thingIds, "Thing/id"));
        }
        if (!observedProps.isEmpty()) {
            filter.addFilter();
            filter.obsFilter.append(STADownloaderUtils.buildEntityFilter(observedProps, "Datastream/ObservedProperty/id"));
            filter.hloFilter.append(STADownloaderUtils.buildEntityFilter(observedProps, "Thing/Datastream/ObservedProperty/id"));
        }
        
        if (toCsvFlat) {
            filter.obsFilter.expand = "Datastreams/ObservedProperty,Datastreams/Thing";
            filter.obsFilter.select = "Datastreams/Thing/id,Datastreams/Thing/name,Datastreams/Thing/description,Datastreams/unitOfMeasurement,result,resultTime";
        
            filter.hloFilter.expand = "Locations,Thing";
            filter.hloFilter.select = "Locations/location,time,Thing/id";
        }
        try {
            Map<String, Map<Date, double[]>> historicalLocations = new HashMap<>();
            
            // extract historical location for csv flat
            if (toCsvFlat) {
                String hlQuery = staUrl + "/HistoricalLocations";
                hlQuery = hlQuery + filter.hloFilter.getQuery();

                LOGGER.info("STA historical location initial query: " + hlQuery);
                URL hlUrl = new URL(hlQuery.replace(" ", "%20"));
                HistoricalLocationsResponse hlResponse = readResponse(hlUrl, HistoricalLocationsResponse.class);
                extractHistoricalLocations(hlResponse, historicalLocations);

                while (hlResponse.getIotNextLink() != null) {
                    hlQuery = hlResponse.getIotNextLink();
                    LOGGER.info("STA historical location next query: " + hlQuery);
                    hlUrl = new URL(hlQuery.replace(" ", "%20"));
                    hlResponse = readResponse(hlUrl, HistoricalLocationsResponse.class);
                    extractHistoricalLocations(hlResponse, historicalLocations);

                }
            }
            
            
            String obsQuery = staUrl + "/Observations";
            obsQuery = obsQuery + filter.obsFilter.getQuery();
            
            LOGGER.info("STA Download initial query: " + obsQuery);
            URL url = new URL(obsQuery.replace(" ", "%20"));
            
            Path resultFile;
            if (toCsvFlat) {
                resultFile = Files.createTempFile("sta-download", ".csv");
            } else {
                resultFile = Files.createTempFile("sta-download", ".json");
            }
            
            ObservationsResponse response = readResponse(url, ObservationsResponse.class);
            LOGGER.info("response read");
            ObservationsResponse result   = null;
            if (toCsvFlat) {
                storeCsvFlatFile(true, response, resultFile, historicalLocations);
            } else {
                result = new ObservationsResponse(response.getValue());
            }
            
            while (response.getIotNextLink() != null) {
                obsQuery = response.getIotNextLink();
                LOGGER.info("STA Download next query: " + obsQuery);
                url = new URL(obsQuery.replace(" ", "%20"));
                response = readResponse(url, ObservationsResponse.class);
                if (toCsvFlat) {
                    storeCsvFlatFile(false, response, resultFile, historicalLocations);
                } else {
                    result.getValue().addAll(response.getValue());
                }
            }

            if (!toCsvFlat) {
                storeFile(result, resultFile);
            }
            Path outputFile;
            if (compress) {
                outputFile = Files.createTempFile("sta-download" , ".zip");
                ZipUtilities.zipNIO(outputFile, resultFile);
            } else {
                outputFile = resultFile;
            }
            outputParameters.getOrCreate(FILE_OUTPUT).setValue(outputFile.toFile());
        } catch (IOException ex) {
            throw new ProcessException("Error while requesting input url", this, ex);
        }
    }
    
    private void storeCsvFlatFile(final boolean first, final ObservationsResponse result, Path file, Map<String, Map<Date, double[]>> historicalLocations) throws IOException {
        // TODO= z_value
        if (first) {
            IOUtilities.appendToFile("time;thing_id;thing_name;thing_desc;lon;lat;obsprop_id;obsprop_name;obsprop_desc;result;uom_name;uom_symbol;\n", file);
        } 
        StringBuilder block  = new StringBuilder();
        for (Observation obs : result.getValue()) {
            
            Datastream ds       = obs.getDatastream();
            Thing th            = ds != null ? ds.getThing() : null;
            ObservedProperty op = ds != null ? ds.getObservedProperty() : null;
            String timeStr      = obs.getResultTime();
            Date time           = null;
            if (timeStr != null && !timeStr.isEmpty()) {
                block.append(timeStr).append(';');
                try {
                    time = PARSER.parse(timeStr);
                } catch (ParseException ex) {
                    LOGGER.warning("Unable to parse date:");
                }
            } else {
                block.append(';');
            }
            
            if (th != null) {
                block.append(emptyIfNull(th.getIotId())).append(';');
                block.append(emptyIfNull(th.getName())).append(';');
                block.append(emptyIfNull(th.getDescription())).append(';');
                
                // extract locations
                Map<Date, double[]> thingLocations = historicalLocations.get(th.getIotId());
                if (thingLocations != null && !thingLocations.isEmpty()) {
                    double[] pos = findClosestDate(thingLocations, time);
                    if (pos != null) {
                        block.append(pos[0]).append(";").append(pos[1]).append(";");
                    } else {
                        LOGGER.warning("Date not found in map. TODO ?");
                        block.append(";;");
                    }
                } else {
                    block.append(";;");
                }
                
            } else {
                block.append(";;;;;");
            }
            
            if (op != null) {
                block.append(emptyIfNull(op.getIotId())).append(';');
                block.append(emptyIfNull(op.getName())).append(';');
                block.append(emptyIfNull(op.getDescription())).append(';');
            } else {
                block.append(";;;");
            }
            block.append(obs.getResult()).append(';');
            
            // TODO, real uom case
            if (ds != null) {
                if (ds.getUnitOfMeasurement() instanceof Map m) {
                    block.append(emptyIfNull((String) m.get("name"))).append(';');
                    block.append(emptyIfNull((String) m.get("symbol"))).append(';');
                }
            } else {
                block.append(";;");
            }
            block.append("\n");
        }
        IOUtilities.appendToFile(block.toString(), file);
    }
    
    private String emptyIfNull(String s) {
        if (s == null) return "";
        return s;
    }
    
    protected void storeFile(final ObservationsResponse result, Path file) throws UnsupportedEncodingException, IOException, ProcessException {
        new ObjectMapper().writeValue(Files.newOutputStream(file, StandardOpenOption.WRITE), result);
    }

    private void extractHistoricalLocations(HistoricalLocationsResponse hlResponse, Map<String, Map<Date, double[]>> historicalLocations) {
        for (HistoricalLocation hl : hlResponse.getValue()) {
            Date time = hl.getTime();
            if (time == null) {
                LOGGER.warning("No time found for historical location");
                    continue;
            }
            if (hl.getLocations() != null && !hl.getLocations().isEmpty()) {
                if (hl.getLocations().size() > 1) {
                    LOGGER.warning("Error multiple locations for an historical one.");
                }
                Location loc = hl.getLocations().get(0);
                
                if (hl.getThing() == null && hl.getThing().getIotId() == null) {
                    LOGGER.warning("No Thing found for historical location");
                    continue;
                } 
                String thId = hl.getThing().getIotId();
                
                if (loc.getLocation() instanceof Map locMap) {
                    Object geom = locMap.get("geometry");
                    if (geom instanceof Map geomMap) {
                        Object type = geomMap.get("type");
                        if (type instanceof String typeStr) {
                            if ("Point".equalsIgnoreCase(typeStr)) {
                                // "coordinates": [-34.33398, 49.3061]
                                Object coordinates = geomMap.get("coordinates");
                                if (coordinates instanceof List coordList) {
                                    if (coordList.size() == 2 ||coordList.size() == 3) {
                                        Map<Date, double[]> thingHl = historicalLocations.computeIfAbsent(thId, t -> new HashMap<>());
                                        thingHl.put(time, new double[] {(double)coordList.get(0), (double)coordList.get(1)});
                                    } else {
                                        LOGGER.warning("Bad number of coordinates " + coordList.size());
                                    }
                                } else {
                                    LOGGER.warning("Bad coordinates object: " + coordinates);
                                }
                            } else {
                                LOGGER.warning("Not a point geometry: " + typeStr);
                            }
                        } else {
                            LOGGER.warning("No geometry type found");
                        }
                    } else {
                        LOGGER.warning("No geometry found");
                    }
                } else {
                    LOGGER.warning("No Location/location found");
                }
            } else {
                LOGGER.warning("No Location found");
            }
        }
    }
    
    private double[] findClosestDate(Map<Date, double[]> thingLocations, Date time) {
        if (thingLocations.size() == 1) return thingLocations.values().iterator().next();
        
        // exact match
        double[] pos = thingLocations.get(time);
        if (pos != null) {
            return pos;
        }
        
        List<Date> dates = new ArrayList(thingLocations.keySet());
        Date previous = dates.get(0);
        for (int i = 1; i < dates.size(); i++) {
            Date next = dates.get(i);
            if (previous.before(time) && next.after(time) ) {
                return thingLocations.get(previous); 
            }
            previous = next;
        }
        return thingLocations.get(previous);
    }
    
    
    private static class Filters {
        
        public Filter obsFilter = new Filter();
        public Filter hloFilter = new Filter();
        
        public void append(String s) {
            obsFilter.append(s);
            hloFilter.append(s);
        }
        
        public void addFilter() {
           obsFilter.addFilter();
           hloFilter.addFilter();
        }
    }
    
    private static class Filter {
        public boolean hasFilter = false;
        public String expand = "";
        public String select = "";
        
        public StringBuilder filter = new StringBuilder();
        
        public void addFilter() {
            if (hasFilter) filter.append(" and ");
            hasFilter = true;
        }
        
        public void append(String s) {
            filter.append(s);
        }

        private String getQuery() {
            StringBuilder result = new StringBuilder("?");
            boolean first = true;
            if (!expand.isEmpty()) {
                if (!first) result.append("&");
                result.append("$expand=").append(expand);
                first = false;
            }
            if (!select.isEmpty()) {
                if (!first) result.append("&");
                result.append("$select=").append(select);
                first = false;
            }
            if (!filter.isEmpty()) {
                if (!first) result.append("&");                
                result.append("$filter=").append(filter.toString());
            }
            return result.toString();
        }
    }
}
