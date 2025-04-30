/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
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

import com.examind.sts.core.STSWorker;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.constellation.api.CommonConstants.DATA_ARRAY;
import org.constellation.sos.core.SOSworker;
import org.constellation.ws.CstlServiceException;
import org.geotoolkit.gml.xml.AbstractFeature;
import org.geotoolkit.gml.xml.FeatureCollection;
import org.geotoolkit.gml.xml.FeatureProperty;
import org.geotoolkit.gml.xml.LineString;
import org.geotoolkit.gml.xml.v321.PointType;
import org.geotoolkit.gml.xml.v321.TimeInstantType;
import org.geotoolkit.gml.xml.v321.TimePeriodType;
import org.geotoolkit.internal.geojson.binding.GeoJSONFeature;
import org.geotoolkit.internal.geojson.binding.GeoJSONGeometry;
import org.geotoolkit.ogc.xml.v200.TemporalOpsType;
import org.geotoolkit.ogc.xml.v200.TimeDuringType;
import org.geotoolkit.ogc.xml.v200.TimeEqualsType;
import org.geotoolkit.sampling.xml.SamplingFeature;
import org.geotoolkit.sos.xml.Capabilities;
import org.geotoolkit.sos.xml.Contents;
import org.geotoolkit.sos.xml.ObservationOffering;
import org.geotoolkit.sos.xml.v200.GetCapabilitiesType;
import org.geotoolkit.sos.xml.v200.GetFeatureOfInterestType;
import org.geotoolkit.sos.xml.v200.GetResultResponseType;
import org.geotoolkit.sos.xml.v200.GetResultType;
import org.geotoolkit.sts.GetObservations;
import org.geotoolkit.sts.GetObservedProperties;
import org.geotoolkit.sts.GetObservedPropertyById;
import org.geotoolkit.sts.GetThingById;
import org.geotoolkit.sts.json.DataArray;
import org.geotoolkit.sts.json.DataArrayResponse;
import org.geotoolkit.sts.json.HistoricalLocation;
import org.geotoolkit.sts.json.ObservedPropertiesResponse;
import org.geotoolkit.sts.json.ObservedProperty;
import org.geotoolkit.sts.json.Thing;
import org.junit.Assert;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SosHarvesterTestUtils {

    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");

    public static List<String> getObservedProperties(STSWorker stsWorker, String sensorId) throws CstlServiceException {
        List<String> results = new ArrayList<>();
        GetObservedProperties request = new GetObservedProperties();
        request.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:" + sensorId);
        ObservedPropertiesResponse resp = stsWorker.getObservedProperties(request);
        for (ObservedProperty op : resp.getValue()) {
            results.add(op.getIotId());
        }
        return results;
    }
    
    public static Thing getThing(STSWorker stsWorker, String sensorId) throws CstlServiceException {
        GetThingById request = new GetThingById();
        request.setId(sensorId);
        return stsWorker.getThingById(request);
    }

    public static List<ObservedProperty> getFullObservedProperties(STSWorker stsWorker, String sensorId) throws CstlServiceException {
        List<ObservedProperty> results = new ArrayList<>();
        GetObservedProperties request = new GetObservedProperties();
        request.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:" + sensorId);
        ObservedPropertiesResponse resp = stsWorker.getObservedProperties(request);
        for (ObservedProperty op : resp.getValue()) {
            results.add(op);
        }
        return results;
    }

    public static ObservedProperty getObservedPropertyById(STSWorker stsWorker, String obsId) throws CstlServiceException {
        GetObservedPropertyById request = new GetObservedPropertyById();
        request.setId(obsId);
        return stsWorker.getObservedPropertyById(request);
    }

    public static Set<String> getQualityFieldNames(STSWorker stsWorker, String sensorId) throws CstlServiceException {
        GetObservations request = new GetObservations();
        request.setResultFormat(DATA_ARRAY);
        request.getExtraFlag().put("forMDS", "true");
        request.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:" + sensorId);
        DataArrayResponse resp = (DataArrayResponse) stsWorker.getObservations(request);
        Set<String> results = new HashSet<>();
        for (DataArray array : resp.getValue()) {
            int index = array.getComponents().indexOf("resultQuality");
            if (index != -1) {
                for (Object o : array.getDataArray()) {
                    List obs = (List) o;
                    List quals = (List) obs.get(index);
                    for (Object q : quals) {
                        Map qual = (Map) q;
                        results.add((String)qual.get("nameOfMeasure"));
                    }
                }
            }
        }
        return results;
    }
    
    
    public static Set<String> getParameterFieldNames(STSWorker stsWorker, String sensorId) throws CstlServiceException {
        GetObservations request = new GetObservations();
        request.setResultFormat(DATA_ARRAY);
        request.getExtraFlag().put("forMDS", "true");
        request.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:" + sensorId);
        DataArrayResponse resp = (DataArrayResponse) stsWorker.getObservations(request);
        Set<String> results = new HashSet<>();
        for (DataArray array : resp.getValue()) {
            int index = array.getComponents().indexOf("parameters");
            if (index != -1) {
                for (Object o : array.getDataArray()) {
                    List obs = (List) o;
                    Map quals = (Map) obs.get(index);
                    for (Object k : quals.keySet()) {
                        results.add((String) k);
                    }
                }
            }
        }
        return results;
    }

    public static void verifyAllObservedProperties(STSWorker stsWorker, String sensorId, List<String> expectedObsProp) throws CstlServiceException {
        List<String> obsProp = getObservedProperties(stsWorker, sensorId);
        boolean ok = obsProp.containsAll(expectedObsProp);
        String msg = "";
        if (!ok) {
            msg = sensorId + " observed properties missing:\n";
            for (String o : expectedObsProp) {
                if (!obsProp.contains(o)) {
                    msg = msg + o + '\n';
                }
            }
        }
        Assert.assertTrue(msg, ok);

        ok = expectedObsProp.containsAll(obsProp);
        msg = "";
        if (!ok) {
            msg = sensorId + " observed properties supplementary:\n";
            for (String o : obsProp) {
                if (!expectedObsProp.contains(o)) {
                    msg = msg + o + '\n';
                }
            }
        }
        Assert.assertTrue(msg, ok);
    }

    public static void verifyObservedProperties(STSWorker stsWorker, String sensorId, List<String> expectedObsProp) throws CstlServiceException {
        List<String> obsProp = getObservedProperties(stsWorker, sensorId);
        boolean ok = false;
        for (String expO : expectedObsProp) {
            if (obsProp.contains(expO)) {
                ok = true;
                break;
            }
        }
        String msg = "";
        if (!ok) {
            msg = sensorId + " observed properties missing:\n";
            for (String o : obsProp) {
                msg = msg + o + '\n';
            }
        }
        Assert.assertTrue(msg, ok);
    }

    public static Integer getNbMeasure(STSWorker stsWorker, String sensorId) throws CstlServiceException {
        GetObservations request = new GetObservations();
        request.setResultFormat(DATA_ARRAY);
        request.getExtraFlag().put("forMDS", "true");
        request.setCount(true);
        request.setTop(0);
        request.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:" + sensorId);
        DataArrayResponse resp = (DataArrayResponse) stsWorker.getObservations(request);
        return resp.getIotCount().toBigInteger().intValue();
    }

    public static String getMeasure(STSWorker stsWorker, String sensorId) throws CstlServiceException {
        GetObservations request = new GetObservations();
        request.setResultFormat(DATA_ARRAY);
        request.getExtraFlag().put("forMDS", "true");
        request.setCount(false);
        request.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:" + sensorId);
        DataArrayResponse resp = (DataArrayResponse) stsWorker.getObservations(request);
        StringBuilder sb = new StringBuilder();
        for (DataArray array : resp.getValue()) {
            for (Object o : array.getDataArray()) {
                List l = (List) o;
                Date d = (Date) l.get(2);
                sb.append(sdf2.format(d));
                List values = (List) l.get(3);
                for (Object value : values) {
                    sb.append(",");
                    if (value != null && !value.equals(Double.NaN)) {
                        sb.append(value);
                    }
                }
                sb.append("@@");
            }
        }
        return sb.toString() + '\n';
    }

    public static String getMeasure(SOSworker sosWorker, String offeringId, String observedProperty, String foi) throws Exception {
        return getMeasure(sosWorker, offeringId, observedProperty, foi, false);
    }

    public static String getMeasure(SOSworker sosWorker, String offeringId, String observedProperty, String foi, boolean inputColumn) throws Exception {
        return getMeasure(sosWorker, offeringId, observedProperty, foi, null, null, inputColumn);
    }

    public static String getMeasure(SOSworker sosWorker, String offeringId, String observedProperty, String foi, String timeStart, String timeEnd, boolean inputColumn) throws Exception {
        List<String> fois = foi != null ? Arrays.asList(foi) : null;
        List<TemporalOpsType> tFilters = new ArrayList<>();
        if (timeStart != null && timeEnd != null) {
            tFilters.add(new TimeDuringType("samplingTime", new TimePeriodType(foi, timeStart, timeEnd)));
        } else if (timeStart != null) {
            tFilters.add(new TimeEqualsType("samplingTime", new TimeInstantType("t", timeStart)));
        }
        GetResultResponseType gr = (GetResultResponseType) sosWorker.getResult(new GetResultType("2.0.0", "SOS", offeringId, observedProperty, tFilters, null, fois));
        if (inputColumn) {
            return gr.getResultValues().toString().replace("@@", "@@\n");
        } else {
            return gr.getResultValues().toString() + '\n';
        }
    }

    public static ObservationOffering getOffering(SOSworker worker, String sensorId) throws CstlServiceException {
        Capabilities capa        = worker.getCapabilities(new GetCapabilitiesType());
        Contents ct              = capa.getContents();
        ObservationOffering offp = null;
        for (ObservationOffering off : ct.getOfferings()) {
            if (off.getProcedures().contains(sensorId)) {
                offp = off;
            }
        }
        return offp;
    }

    public static int getNbOffering(SOSworker worker, int offset) throws CstlServiceException {
        Capabilities capa        = worker.getCapabilities(new GetCapabilitiesType());
        Contents ct              = capa.getContents();
        return ct.getOfferings().size() - offset;
    }

    public static List<SamplingFeature> getFeatureOfInterest(SOSworker worker, List<String> foids) throws CstlServiceException {
        List<SamplingFeature> results = new ArrayList<>();
        AbstractFeature o = worker.getFeatureOfInterest(new GetFeatureOfInterestType("2.0.0", "SOS", foids));
        if (o instanceof FeatureCollection fc) {
            for (FeatureProperty fp :  fc.getFeatureMember()) {
                if (fp.getAbstractFeature() instanceof SamplingFeature sp) {
                    results.add(sp);
                }
            }
        } else if (o instanceof SamplingFeature sf) {
            results.add(sf);
        }
        return results;
    }

    public static String verifySamplingFeatureLine(List<SamplingFeature> fois, int nbPoint) {
       String foi = null;
       for (SamplingFeature sp : fois) {
            if (sp.getGeometry() instanceof LineString ln) {
                if (ln.getPosList().getValue().size() == nbPoint*2) {
                    foi = sp.getId();
                }
            }
        }
        Assert.assertNotNull(foi);
        return foi;
    }

    public static String verifySamplingFeature(List<SamplingFeature> fois,  double lat, double lon) {
        return verifySamplingFeature(fois, null, lat, lon);
    }

    public static String verifySamplingFeature(List<SamplingFeature> fois, String id, double lat, double lon) {
       String foi = null;
       for (SamplingFeature sp : fois) {
            if ((id != null && sp.getId().equals(id)) || id == null)
            if (sp.getGeometry() instanceof PointType pt) {
                if (pt.getDirectPosition().getCoordinate(0) == lat &&
                    pt.getDirectPosition().getCoordinate(1) == lon) {

                    foi = sp.getId();
                }
            }
        }
        Assert.assertNotNull(foi);
        return foi;
    }

    public static String verifySamplingFeature(List<SamplingFeature> fois, String id) {
       String foi = null;
       for (SamplingFeature sp : fois) {
            if (sp.getId().equals(id)) {
                foi = sp.getId();
            }
        }
        Assert.assertNotNull(foi);
        return foi;
    }

    public static void verifySamplingFeatureNotSame(List<SamplingFeature> fois) {
       Set<String> alreadyFound = new HashSet<>();
       for (SamplingFeature sp : fois) {
            if (sp.getGeometry() instanceof PointType pt) {
                String key = pt.getDirectPosition().getCoordinate(0) + "-" + pt.getDirectPosition().getCoordinate(1);
                if (alreadyFound.contains(key)) {
                    throw new IllegalStateException("duplicated feature of interest for coord:" + key);
                }
                alreadyFound.add(pt.getDirectPosition().getCoordinate(0) + "-" + pt.getDirectPosition().getCoordinate(1));
            }
        }
    }

    public static void verifyHistoricalLocation(HistoricalLocation loc1, String date, double lat, double lon) throws ParseException {
        Assert.assertEquals(loc1.getTime().getTime(), sdf.parse(date).getTime());
        Assert.assertEquals(1, loc1.getLocations().size());
        Assert.assertTrue(loc1.getLocations().get(0).getLocation() instanceof GeoJSONFeature);
        GeoJSONFeature feat1 = (GeoJSONFeature) loc1.getLocations().get(0).getLocation();
        Assert.assertTrue(feat1.getGeometry() instanceof GeoJSONGeometry.GeoJSONPoint);
        GeoJSONGeometry.GeoJSONPoint pt1 = (GeoJSONGeometry.GeoJSONPoint) feat1.getGeometry();
        Assert.assertEquals(lat, pt1.getCoordinates()[0], 0.001);
        Assert.assertEquals(lon, pt1.getCoordinates()[1], 0.001);
    }

    public static String getCompositePhenomenon(ObservationOffering offp) {
        String observedProperty = null;
        for (String op : offp.getObservedProperties()) {
            if (op.startsWith("composite")) observedProperty = op;
        }
        return observedProperty;
    }
}
