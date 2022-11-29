/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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

package org.constellation.sos.io.generic;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.apache.sis.storage.DataStoreException;
import org.constellation.generic.GenericReader;
import org.constellation.generic.Values;
import org.constellation.dto.service.config.generic.Automatic;
import org.geotoolkit.observation.ObservationReader;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.temporal.TemporalPrimitive;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.sis.referencing.CRS;

import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import org.constellation.exception.ConstellationMetadataException;
import org.geotoolkit.geometry.jts.JTS;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.OBSERVATION_ID_BASE_NAME;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.OBSERVATION_TEMPLATE_ID_BASE_NAME;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.PHENOMENON_ID_BASE_NAME;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.SENSOR_ID_BASE_NAME;
import org.geotoolkit.observation.OMUtils;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.Offering;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.ResponseMode;
import org.geotoolkit.observation.model.Result;
import org.geotoolkit.observation.model.SamplingFeature;
import org.geotoolkit.observation.model.TextEncoderProperties;
import org.geotoolkit.temporal.object.DefaultInstant;
import org.geotoolkit.temporal.object.TemporalUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;


/**
 *
 * @author Guilhem Legal
 */
public class DefaultGenericObservationReader extends GenericReader implements ObservationReader {

    /**
     * The base for observation id.
     */
    protected final String observationIdBase;

    protected final String observationIdTemplateBase;

    protected final String phenomenonIdBase;

    protected final String sensorIdBase;

    protected static final GeometryFactory GF = new GeometryFactory();

    public DefaultGenericObservationReader(Automatic configuration, Map<String, Object> properties) throws ConstellationMetadataException {
        super(configuration);
        this.observationIdBase = (String) properties.get(OBSERVATION_ID_BASE_NAME);
        this.phenomenonIdBase  = (String) properties.get(PHENOMENON_ID_BASE_NAME);
        this.sensorIdBase      = (String) properties.get(SENSOR_ID_BASE_NAME);
        this.observationIdTemplateBase = (String) properties.get(OBSERVATION_TEMPLATE_ID_BASE_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getEntityNames(final Map<String, Object> hints) throws DataStoreException {
        OMEntity entityType = (OMEntity) hints.get(ENTITY_TYPE);
        if (entityType == null) {
            throw new DataStoreException("Missing entity type parameter");
        }
        String sensorType   = (String) hints.get(SENSOR_TYPE);
        switch (entityType) {
            case FEATURE_OF_INTEREST: return getFeatureOfInterestNames();
            case OBSERVED_PROPERTY:   return getPhenomenonNames();
            case PROCEDURE:           return getProcedureNames(sensorType);
            case LOCATION:            throw new DataStoreException("not implemented yet.");
            case HISTORICAL_LOCATION: throw new DataStoreException("not implemented yet.");
            case OFFERING:            return getOfferingNames(sensorType);
            case OBSERVATION:         throw new DataStoreException("not implemented yet.");
            case RESULT:              throw new DataStoreException("not implemented yet.");
            default: throw new DataStoreException("unexpected entity type:" + entityType);
        }
    }

    private List<String> getOfferingNames(String sensorType) throws DataStoreException {
        try  {
            final Values values = loadData("var02");
            final List<String> result = new ArrayList<>();
            for (String procedure : values.getVariables("var02")) {
                if (procedure.startsWith(sensorIdBase)) {
                    procedure = procedure.replace(sensorIdBase, "");
                }
                result.add("offering-" + procedure);
            }
            return result;
        } catch (ConstellationMetadataException ex) {
            throw new DataStoreException(ex);
        }
    }

    private List<String> getProcedureNames(String sensorType) throws DataStoreException {
        try {
            final Values values = loadData(Arrays.asList("var02"));
            return values.getVariables("var02");
        } catch (ConstellationMetadataException ex) {
            throw new DataStoreException(ex);
        }
    }

    private List<String> getPhenomenonNames() throws DataStoreException {
        try {
            final Values values = loadData(Arrays.asList("var03", "var83"));
            final List<String> results = values.getVariables("var03");
            results.addAll(values.getVariables("var83"));
            return results;
        } catch (ConstellationMetadataException ex) {
            throw new DataStoreException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existEntity(final Map<String, Object> hints) throws DataStoreException {
        OMEntity entityType = (OMEntity) hints.get(ENTITY_TYPE);
        if (entityType == null) {
            throw new DataStoreException("Missing entity type parameter");
        }
        String identifier   = (String) hints.get(IDENTIFIER);
        String sensorType   = (String) hints.get(SENSOR_TYPE);
        switch (entityType) {
            case FEATURE_OF_INTEREST: return getFeatureOfInterestNames().contains(identifier);
            case OBSERVED_PROPERTY:   return getPhenomenonNames().contains(identifier);
            case PROCEDURE:           return existProcedure(identifier);
            case LOCATION:            throw new DataStoreException("not implemented yet.");
            case HISTORICAL_LOCATION: throw new DataStoreException("not implemented yet.");
            case OFFERING:            return getOfferingNames(sensorType).contains(identifier);
            case OBSERVATION:         throw new DataStoreException("not implemented yet.");
            case RESULT:              throw new DataStoreException("not implemented yet.");
            default: throw new DataStoreException("unexpected entity type:" + entityType);
        }
    }

    private List<String> getFeatureOfInterestNames() throws DataStoreException {
        try {
            final Values values = loadData(Arrays.asList("var04", "var67"));
            final List<String> result = values.getVariables("var04");
            final List<String> curves = values.getVariables("var67");
            if (!curves.isEmpty()) {
                result.addAll(curves);
            }
            return result;
        } catch (ConstellationMetadataException ex) {
            throw new DataStoreException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemporalPrimitive getEventTime() throws DataStoreException {
         try {
            final Values values = loadData(Arrays.asList("var06"));
            String v = values.getVariable("var06");
            if (v != null) {
                Calendar d = TemporalUtilities.parseDateCal(v);
                return new DefaultInstant(Collections.EMPTY_MAP, d.getTime());
            } else {
                return null;
            }
         } catch (ConstellationMetadataException | ParseException ex) {
            throw new DataStoreException(ex);
         }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Offering> getObservationOfferings(final Map<String, Object> hints) throws DataStoreException {
        String sensorType   = (String) hints.get(SENSOR_TYPE);
        Object identifierVal = hints.get(IDENTIFIER);
        List<String> identifiers = new ArrayList<>();
        if (identifierVal instanceof Collection) {
            identifiers.addAll((Collection<? extends String>) identifierVal);
        } else if (identifierVal instanceof String) {
            identifiers.add((String) identifierVal);
        } else if (identifierVal == null) {
            identifiers.addAll(getOfferingNames(sensorType));
        }
        final List<Offering> offerings = new ArrayList<>();
        for (String offeringName : identifiers) {
            Offering off = getObservationOffering(offeringName);
            if (off != null) {
                offerings.add(off);
            }
        }
        return offerings;
    }

    private Offering getObservationOffering(final String offeringId) throws DataStoreException {
        try {
            final Values values = loadData(Arrays.asList("var07", "var08", "var09", "var10", "var11", "var12", "var18", "var46"), offeringId);

            final boolean exist = values.getVariable("var46") != null;
            if (!exist) {
                return null;
            }

            final List<String> srsName = values.getVariables("var07");

            // event time
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Date begin = null;
            String beginStr = values.getVariable("var08");
            if (beginStr != null) {
                begin = df.parse(beginStr);
            }
            Date end = null;
            String endStr   = values.getVariable("var09");
            if (endStr != null) {
                end = df.parse(endStr);
            }
            TemporalGeometricPrimitive time = OMUtils.buildTime(offeringId, begin, end);

            // procedure
            final String procedure = sensorIdBase + offeringId.substring(9);

            // phenomenon
            final List<String> observedProperties = new ArrayList<>();
            for (String phenomenonId : values.getVariables("var12")) {
                if (phenomenonId!= null && !phenomenonId.isEmpty()) {
                    Values compositeValues = loadData(Arrays.asList("var17"), phenomenonId);
                    final List<org.geotoolkit.observation.model.Phenomenon> components = new ArrayList<>();
                    for (String componentID : compositeValues.getVariables("var17")) {
                        components.add((org.geotoolkit.observation.model.Phenomenon) getPhenomenon(componentID));
                    }
                    compositeValues = loadData(Arrays.asList("var15", "var16"), phenomenonId);
                    final CompositePhenomenon composite = new CompositePhenomenon(phenomenonId,
                                                                                  compositeValues.getVariable("var15"),
                                                                                  compositeValues.getVariable("var16"),
                                                                                  compositeValues.getVariable("var16"),
                                                                                  null,
                                                                                  components);
                    observedProperties.add(composite.getId());
                }
            }
            for (String phenomenonId : values.getVariables("var11")) {
                if (phenomenonId != null && !phenomenonId.isEmpty()) {
                    final Phenomenon phenomenon = getPhenomenon(phenomenonId);
                    observedProperties.add(phenomenon.getId());
                }
            }

            // feature of interest
            final List<String> foisV200    = new ArrayList<>();
            for (String foiID : values.getVariables("var18")) {
                foisV200.add(foiID);
            }

            return new Offering(offeringId,
                                offeringId,
                                null,
                                null,
                                null, // bounds
                                srsName,
                                time,
                                procedure,
                                observedProperties,
                                foisV200);

        } catch (ConstellationMetadataException | ParseException ex) {
            throw new DataStoreException(ex);
        }
    }

    @Override
    public Phenomenon getPhenomenon(String phenomenonName) throws DataStoreException {
        // we remove the phenomenon id base
        if (phenomenonName.contains(phenomenonIdBase)) {
            phenomenonName = phenomenonName.replace(phenomenonIdBase, "");
        }
        try {
            final Values values = loadData(Arrays.asList("var13", "var14", "var47"), phenomenonName);
            final boolean exist = values.getVariable("var47") != null;
            if (!exist) {
                return getCompositePhenomenon(phenomenonName);
            }
            return new Phenomenon(phenomenonName, values.getVariable("var13"), values.getVariable("var13"), values.getVariable("var14"), null);
        } catch (ConstellationMetadataException ex) {
            throw new DataStoreException(ex);
        }
    }

    private CompositePhenomenon getCompositePhenomenon(String phenomenonId) throws DataStoreException {
        // we remove the phenomenon id base
        if (phenomenonId.contains(phenomenonIdBase)) {
            phenomenonId = phenomenonId.replace(phenomenonIdBase, "");
        }
        try {
            Values compositeValues = loadData("var68", phenomenonId);
            final boolean exist = compositeValues.getVariable("var68") != null;
            if (!exist) {
                return null;
            }
            compositeValues = loadData(Arrays.asList("var17"), phenomenonId);
            final List<org.geotoolkit.observation.model.Phenomenon> components = new ArrayList<>();
            for (String componentID : compositeValues.getVariables("var17")) {
                components.add((org.geotoolkit.observation.model.Phenomenon) getPhenomenon(componentID));
            }
            compositeValues = loadData(Arrays.asList("var15", "var16"), phenomenonId);
            final CompositePhenomenon phenomenon = new CompositePhenomenon(phenomenonId,
                                                                            compositeValues.getVariable("var15"),
                                                                            compositeValues.getVariable("var15"),
                                                                            compositeValues.getVariable("var16"),
                                                                            null,
                                                                            components);
            return phenomenon;
        } catch (ConstellationMetadataException ex) {
            throw new DataStoreException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SamplingFeature getFeatureOfInterest(final String id) throws DataStoreException {
        try {
            final Values values = loadData(Arrays.asList("var19", "var20", "var21", "var22", "var23", "var24", "var48"), id);

            final boolean exist = values.getVariable("var48") != null;
            if (!exist) {
                return getFeatureOfInterestCurve(id);
            }

            final String name            = values.getVariable("var19");
            final String description     = values.getVariable("var20");
            final String sampledFeature  = values.getVariable("var21");

            //final String pointID         = values.getVariable("var22");
            final String srsName         = values.getVariable("var23");
            //final String dimension       = values.getVariable("var24");
            final List<Double> coordinates = getCoordinates(id);

            final CoordinateReferenceSystem crs = CRS.forCode(srsName);
            final Point location = GF.createPoint(new Coordinate(coordinates.get(0), coordinates.get(1)));
            JTS.setCRS(location, crs);

            return new org.geotoolkit.observation.model.SamplingFeature(id, name, description, null, sampledFeature, location);
        } catch (ConstellationMetadataException | FactoryException ex) {
            throw new DataStoreException(ex);
        }
    }

    public SamplingFeature getFeatureOfInterestCurve(final String id) throws DataStoreException {
        try {
            final Values values = loadData(Arrays.asList("var51", "var52", "var53", "var54", "var55", "var56", "var56", "var57",
                                                         "var58", "var59", "var60", "var61", "var62", "var63", "var82"), id);

            final boolean exist = values.getVariable("var51") != null;
            if (!exist) {
                return null;
            }

            final String name            = values.getVariable("var53");
            final String description     = values.getVariable("var52");
            final String sampledFeature  = values.getVariable("var59");

            /*final String boundSrsName    = values.getVariable("var54");
            final String envID           = values.getVariable("var82");
            final Double lcx             = (Double) values.getTypedVariable("var55");
            final Double lcy             = (Double) values.getTypedVariable("var56");
            final Double ucx             = (Double) values.getTypedVariable("var57");
            final Double ucy             = (Double) values.getTypedVariable("var58");
            final Envelope env           = SOSXmlFactory.buildEnvelope(version, envID, lcx, lcy, ucx, ucy, boundSrsName);

            final String lengthUom       = values.getVariable("var60");
            final Double lengthValue     = (Double) values.getTypedVariable("var61");*/

            final String shapeID         = values.getVariable("var62");
            //final String shapeSrsName    = values.getVariable("var63");

            final LineString location    = buildShape(shapeID);

            //return buildSamplingCurve(version, id, name, description, sampleFeatureProperty, location, lengthValue, lengthUom, env);

            return new org.geotoolkit.observation.model.SamplingFeature(id, name, description, null, sampledFeature, location);
        } catch (ConstellationMetadataException ex) {
            throw new DataStoreException(ex);
        }
    }

    private LineString buildShape(final String shapeID) throws ConstellationMetadataException {
        final Values values = loadData(Arrays.asList("var64", "var65", "var66"), shapeID);
        final List<Object> xValues     = values.getTypedVariables("var64");
        final List<Object> yValues     = values.getTypedVariables("var65");
        final List<Object> zValues     = values.getTypedVariables("var66");
        final Coordinate[] pos         = new Coordinate[xValues.size()];
        for (int i = 0; i < xValues.size(); i++) {
            final List<Double> coord = new ArrayList<>();
            final Double x = (Double) xValues.get(i);
            final Double y = (Double) yValues.get(i);
            coord.add(x);
            coord.add(y);
            if (zValues.size() < i) {
                final Double z = (Double) zValues.get(i);
                pos[i] = new Coordinate(x, y, z);
            } else {
                pos[i] = new Coordinate(x, y);
            }
        }
        return GF.createLineString(pos);

    }

    private List<Double> getCoordinates(String samplingFeatureId) throws DataStoreException {
        try {
            final Values values = loadData(Arrays.asList("var25", "var45"), samplingFeatureId);
            final List<Double> result = new ArrayList<>();
            String coordinate = values.getVariable("var25");
            if (coordinate != null) {
                try {
                    result.add(Double.parseDouble(coordinate));
                } catch (NumberFormatException ex) {
                    throw new DataStoreException(ex);
                }
            }
            coordinate = values.getVariable("var45");
            if (coordinate != null) {
                try {
                    result.add(Double.parseDouble(coordinate));
                } catch (NumberFormatException ex) {
                    throw new DataStoreException(ex);
                }
            }
            return result;
        } catch (ConstellationMetadataException ex) {
            throw new DataStoreException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observation getObservation(final String identifier, final QName resultModel, final ResponseMode mode) throws DataStoreException {
        try {
            final List<String> variables;
            if (resultModel.equals(OBSERVATION_QNAME))  {
                variables = Arrays.asList("var26", "var50", "var27", "var49", "var28", "var29", "var30", "var31");
            } else if (resultModel.equals(MEASUREMENT_QNAME))  {
                variables = Arrays.asList("var69", "var70", "var71", "var72", "var73", "var74", "var75", "var76");
            } else {
                throw new IllegalArgumentException("unexpected resultModel:" + resultModel);
            }
            final Values values = loadData(variables, identifier);
            final String foiPoint = values.getVariable(variables.get(0));
            final String foiCurve = values.getVariable(variables.get(1));
            final SamplingFeature featureOfInterest;
            if (foiPoint != null) {
                featureOfInterest = getFeatureOfInterest(foiPoint);
            } else if (foiCurve != null){
                featureOfInterest = getFeatureOfInterestCurve(foiCurve);
            } else {
               featureOfInterest = null;
               LOGGER.log(Level.INFO, "no featureOfInterest for result:{0}", identifier);
            }
            final String proc             = values.getVariable(variables.get(4));
            final String phenomenon       = values.getVariable(variables.get(2));
            final String phenomenonComp   = values.getVariable(variables.get(3));
            final String resultID         = values.getVariable(variables.get(7));
            final String obsID;
            if (identifier.startsWith(observationIdBase)) {
                obsID = "obs-" + identifier.substring(observationIdBase.length());
            } else if (identifier.startsWith(observationIdTemplateBase)) {
                obsID = "obs-" + identifier.substring(observationIdTemplateBase.length());
            } else {
                obsID = "obs-?";
            }

            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            String beginStr = values.getVariable(variables.get(5));
            Date begin = null;
            if (beginStr != null) {
                begin = df.parse(beginStr);
            }
            String endStr   = values.getVariable(variables.get(6));
            Date end = null;
            if (endStr != null) {
                end = df.parse(endStr);
            }
            final TemporalGeometricPrimitive samplingTime = OMUtils.buildTime(obsID, begin, end);

            final Phenomenon observedProperty;
            if (phenomenon != null) {
                observedProperty = getPhenomenon(phenomenon);
            } else if (phenomenonComp != null) {
                observedProperty = getCompositePhenomenon(phenomenonComp);
            } else {
                observedProperty = null;
                LOGGER.log(Level.INFO, "no phenomenon for result:{0}", identifier);
            }
            final Result result = getResult(resultID, resultModel);
            String type;
            if (resultModel.equals(OBSERVATION_QNAME)) {
                type = "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_ComplexObservation";
            } else {
                // TODO here we don't handle specific result type, but the getResult actually return ony measure.
                type = "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement";
            }
            final Procedure procedure = new Procedure(proc);
            return new Observation(obsID,
                                   identifier,
                                   null,
                                   null,
                                   type,
                                   procedure,
                                   samplingTime,
                                   featureOfInterest,
                                   observedProperty, 
                                   null,
                                   result,
                                   null);
            
        } catch (ConstellationMetadataException | ParseException ex) {
            throw new DataStoreException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Result getResult(final String identifier, final QName resultModel) throws DataStoreException {
        try {
            if (resultModel.equals(OBSERVATION_QNAME)) {
                final Values values = loadData(Arrays.asList("var32", "var33", "var34", "var35", "var36", "var37", "var38", "var39",
                        "var40", "var41", "var42", "var43"), identifier);
                final int count = Integer.parseInt(values.getVariable("var32"));

                // encoding
                final String encodingID       = values.getVariable("var34");
                final String tokenSeparator   = values.getVariable("var35");
                final String decimalSeparator = values.getVariable("var36");
                final String blockSeparator   = values.getVariable("var37");
                final TextEncoderProperties encoding = new TextEncoderProperties(decimalSeparator, tokenSeparator, blockSeparator);

                //data block description
                final String blockId          = values.getVariable("var38");
                final String dataRecordId     = values.getVariable("var39");
                final List<Field> fields  = new ArrayList<>();
                final List<String> fieldNames = values.getVariables("var40");
                final List<String> fieldDef   = values.getVariables("var41");
                final List<String> type       = values.getVariables("var42");
                final List<String> uomCodes   = values.getVariables("var43", true);
                for(int i = 0; i < fieldNames.size(); i++) {
                    final String typeName   = type.get(i);
                    final String fieldName  = fieldNames.get(i);
                    final String definition = fieldDef.get(i);
                    String uomCode = null;
                    if (uomCodes.get(i) != null) {
                        uomCode = uomCodes.get(i);
                    }
                    FieldType ft = FieldType.QUANTITY;
                    if (typeName != null) {
                        if ("Quantity".equals(typeName)) {
                            ft = FieldType.QUANTITY;
                        } else if ("Time".equals(typeName)) {
                            ft = FieldType.TIME;
                        } else if ("Boolean".equals(typeName)) {
                            ft = FieldType.BOOLEAN;
                        } else if ("Text".equals(typeName)) {
                            ft = FieldType.TEXT;
                        } else {
                            LOGGER.severe("unexpected field type");
                        }
                    }
                    // what to do with definition?
                    fields.add(new Field(i, ft, fieldName, null, null, uomCode));
                }

                final String dataValues = values.getVariable("var33");
                return new ComplexResult(fields, encoding, dataValues, count);

            } else if (resultModel.equals(MEASUREMENT_QNAME)) {
                final Values values    = loadData(Arrays.asList("var77", "var78"), identifier);
                final String uomValue = values.getVariable("var78");
                final float val;
                if (uomValue != null) {
                    val = Float.parseFloat(uomValue);
                } else {
                    val = 0;
                }
                final String uomId     = values.getVariable("var77");
                /*final Values uomvalues = loadData(Arrays.asList("var79", "var80", "var81"), uomId);
                final UnitOfMeasureEntry uom = new UnitOfMeasureEntry(uomId,
                                                                      uomvalues.getVariable("var79"),
                                                                      uomvalues.getVariable("var80"),
                                                                      uomvalues.getVariable("var81"));*/
                final Field field = new Field(-11, FieldType.QUANTITY, null, null, null, uomId);
                return new MeasureResult(field, val);
            } else {
                throw new IllegalArgumentException("unexpected resultModel:" + resultModel);
            }
        } catch (ConstellationMetadataException ex) {
            throw new DataStoreException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemporalPrimitive getFeatureOfInterestTime(final String samplingFeatureName) throws DataStoreException {
        throw new DataStoreException("The Default generic implementation of SOS does not support GetFeatureofInterestTime");
    }

    /**
     * {@inheritDoc}
     */
    private boolean existProcedure(final String href) throws DataStoreException {
        try {
            final Values values = loadData(Arrays.asList("var02"));
            final List<String>  procedureNames = values.getVariables("var02");
            return procedureNames.contains(href);
        } catch (ConstellationMetadataException ex) {
            throw new DataStoreException(ex);
        }
    }

    @Override
    public Procedure getProcess(String identifier) throws DataStoreException {
        if (existProcedure(identifier)) {
            return new Procedure(identifier);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Geometry getSensorLocation(String sensorID) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet in this implementation.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Date, Geometry> getSensorLocations(String sensorID) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet in this implementation.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemporalGeometricPrimitive getTimeForProcedure(final String sensorID) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet in this implementation.");
    }

    @Override
    public Observation getTemplateForProcedure(String procedure) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet in this implementation.");
    }
}
