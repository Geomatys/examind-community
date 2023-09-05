/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2023 Geomatys.
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

import com.examind.process.sos.SosHarvesterUtils.SensorService;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.measure.IncommensurableException;
import javax.measure.UnconvertibleException;
import javax.measure.Unit;
import javax.measure.format.MeasurementParseException;
import org.apache.sis.measure.Units;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ObservationProvider;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.observation.OMUtils;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.ResponseMode;
import org.geotoolkit.observation.model.Result;
import org.geotoolkit.observation.query.ObservationQuery;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SosHarvestFileChecker {

    protected static final Logger LOGGER = Logger.getLogger("com.examind.process.sos");

    protected static final FilterFactory ff = FilterUtilities.FF;

    protected StringBuilder report = new StringBuilder();

    protected Collection<SensorService> services;

    protected Exception error;

    public SosHarvestFileChecker() {
    }

    public void setTargetServices(Collection<SensorService> services) {
        this.services = services;
    }

    public String getReport() {
        return report.toString();
    }

    public Exception getError() {
        return error;
    }

    public void clear() {
        report = new StringBuilder();
        error = null;
    }

    public boolean checkFile(String fileName, Integer dataId) {
        boolean valid = true;
        report.append(fileName).append(":\n");
        try {
            /*
            * 2 - Access to the provider ofthe data.
            */
            final DataProvider provider = DataProviders.getProviderForData(dataId);

            /*
            * 2 - verify the type of the provider.
            */
            if (!(provider instanceof ObservationProvider)) {
                throw new Exception("The file is not supported by csv observation store");
            }
            ObservationProvider csvProvider = (ObservationProvider) provider;

            Set<String> unParseableUoms = new HashSet<>();
            Map<String, Map<String, Set<String>>> unconvertibleUoms = new HashMap<>();

            /*
            * 3 - requesting all the sensor templates.
            */
            final List<Observation> csvTemplates = getObservationTemplates(csvProvider, null);

            if (csvTemplates.isEmpty()) {
                // TODO investigate why to inform the user
                throw new Exception("The data provider did not produce any observations.");
            }

            /*
            * 4 - Iterating on each template (meaning each sensor)
            */
            for (Observation csvTemplate : csvTemplates) {
                String sensorId = csvTemplate.getProcedure().getId();
                Map<String, Set<String>> serviceUnconvertibleUoms = new HashMap<>();

                List<Field> csvFields = getResultFields(csvTemplate.getResult());
                Map<String, Field> csvFieldMap = new HashMap<>();

                /*
                * 4.1 - verify unit of measure parsing.
                */
                for (Field field : csvFields) {
                    if (field.uom != null && !unParseableUoms.contains(field.uom) && !isParseableUnit(field.uom)) {
                        unParseableUoms.add(field.uom);
                    }
                    csvFieldMap.put(field.name, field);
                }

                /*
                * 4.2 - look for matching uom in service procedure fields
                */
                for (SensorService serv : services) {
                    final ObservationProvider servProvider = serv.provider;
                    Set<String> sensorUnconvertibleUom = new HashSet<>();

                    // if this fail its not really the file blame. the service is in a bad state.
                    List<Observation> servTemplates = getObservationTemplates(servProvider, sensorId);

                    // we expect to have at most 1 template response
                    // empty results mean that the procedure is not yet in the service provider
                    if (servTemplates.size() == 1) {
                        List<Field> servFields = getResultFields(servTemplates.get(0).getResult()) ;

                        for (Field servField : servFields) {
                            Field csvField = csvFieldMap.get(servField.name);
                            if (csvField != null) {

                                // verify uom conversion
                                if (csvField.uom != null && servField.uom != null && !csvField.uom.equals(servField.uom)) {
                                    String key = csvField.uom + " => " + servField.uom + " for property: " + csvField.name;
                                    if (!sensorUnconvertibleUom.contains(key) && !isConvertibleUnit(csvField, servField)) {
                                        sensorUnconvertibleUom.add(key);
                                    }
                                }
                            }
                        }

                    } else if (servTemplates.size() > 1) {
                        throw new Exception("Unexpected multiple observation template for procedure: " + sensorId + '\n');
                    }
                    if (!sensorUnconvertibleUom.isEmpty()) {
                        serviceUnconvertibleUoms.put(serv.getServiceNames(), sensorUnconvertibleUom);
                    }
                }
                
                if (!serviceUnconvertibleUoms.isEmpty()) {
                    unconvertibleUoms.put(sensorId, serviceUnconvertibleUoms);
                }
            }

            /*
            * 5 - Build report
            */

            /*
            * 5.1 - UOM parsing
            *
            * do not break the validy of the file as long as the field is recorded with the same uom (or not recorded at all).
            */
            if (!unParseableUoms.isEmpty()) {
                report.append("[WARNING] unparseable Unit Of Measure:\n");
                for (String unParseableUom : unParseableUoms) {
                    report.append(" - ").append(unParseableUom).append('\n');
                }
            }

            /*
            * 5.2 - UOM conversion
            *
            *  break the validy of the file.
            */
            if (!unconvertibleUoms.isEmpty()) {
                report.append("[ERROR] unconvertible Unit Of Measure:\n");
                for (Entry<String, Map<String, Set<String>>> unconvertibleUom : unconvertibleUoms.entrySet()) {
                    report.append(" - Sensor ").append(unconvertibleUom.getKey()).append('\n');
                    Map<String, Set<String>> servUnconvertibleUoms = unconvertibleUom.getValue();

                    // simplify when there is only on service
                    if (servUnconvertibleUoms.size() == 1) {
                        for (String servUnconvertibleUom : servUnconvertibleUoms.values().iterator().next()) {
                            report.append("\t - ").append(servUnconvertibleUom).append('\n');
                        }
                    } else {
                        for (Entry<String, Set<String>> servUnconvertibleUom : servUnconvertibleUoms.entrySet()) {
                            for (String uom : servUnconvertibleUom.getValue()) {
                                report.append("\t - ").append(servUnconvertibleUom).append('\n');
                            }
                        }
                    }
                }
                valid = false;
            }


        } catch (Exception ex) {
            valid = false;
            report.append(ex.getMessage()).append("\n");
            error = ex;
        }

        if (!valid) {
            report.append("KO.\n\n");
            if (error == null) {
                error = new ConstellationException(report.toString());
            } else {
                error = new ConstellationException(report.toString(), error);
            }
        } else {
            report.append("OK.\n\n");
        }
        return valid;
    }

    protected List<Field> getResultFields(Result result) {
        if (result instanceof ComplexResult cr) {
            return cr.getFields();
        } else if (result instanceof MeasureResult mr) {
            LOGGER.warning("The csv observation template does not have a Complex result. This is unexpected.");
            return Arrays.asList(mr.getField());
        }
        throw new IllegalArgumentException("Observation is either null or of an unknow implementation");
    }

    protected List<Observation> getObservationTemplates(ObservationProvider provider, String procedure) throws ConstellationStoreException {
        ObservationQuery query = new ObservationQuery(OMUtils.OBSERVATION_QNAME, ResponseMode.RESULT_TEMPLATE, null);
        if (procedure != null) {
            Filter filter = ff.equal(ff.property("procedure") , ff.literal(procedure));
            query.setSelection(filter);
        }
        query.setIncludeFoiInTemplate(false);
        return provider.getObservations(query).stream().map(obs -> (Observation)obs).toList();
    }

    protected boolean isParseableUnit(String uom) {
        try {
            Units.valueOf(uom);
        } catch (MeasurementParseException | IllegalStateException ex) {
            LOGGER.warning("Unable to parse unit of measure: " + uom + "(" + ex.getMessage() + ")");
            return false;
        }
        return true;
    }

    protected boolean isConvertibleUnit(Field csvField, Field servField) {
         try {
            Unit<?> csvUOM = Units.valueOf(csvField.uom);
            Unit<?> servUOM = Units.valueOf(servField.uom);

            csvUOM.getConverterToAny(servUOM);

        } catch (IncommensurableException | UnconvertibleException | MeasurementParseException | IllegalStateException ex) {
            LOGGER.log(Level.WARNING, "Error while looking for uom converter " + csvField + " => " + servField + " for field: " + csvField.name, ex);
            return false;
        }
        return true;
    }
}
