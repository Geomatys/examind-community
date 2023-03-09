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
import com.examind.sts.core.temporary.DataArrayResponseExt;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.constellation.sos.core.SOSworker;
import org.constellation.ws.CstlServiceException;
import org.geotoolkit.gml.xml.AbstractFeature;
import org.geotoolkit.gml.xml.FeatureCollection;
import org.geotoolkit.gml.xml.FeatureProperty;
import org.geotoolkit.gml.xml.LineString;
import org.geotoolkit.gml.xml.v321.PointType;
import org.geotoolkit.internal.geojson.binding.GeoJSONFeature;
import org.geotoolkit.internal.geojson.binding.GeoJSONGeometry;
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
        request.setResultFormat("dataArray");
        request.getExtraFlag().put("forMDS", "true");
        request.setCount(true);
        request.setTop(0);
        request.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:" + sensorId);
        DataArrayResponseExt resp = (DataArrayResponseExt) stsWorker.getObservations(request);
        return resp.getIotCount().toBigInteger().intValue();
    }

    public static String getMeasure(STSWorker stsWorker, String sensorId) throws CstlServiceException {
        GetObservations request = new GetObservations();
        request.setResultFormat("dataArray");
        request.getExtraFlag().put("forMDS", "true");
        request.setCount(false);
        request.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:" + sensorId);
        DataArrayResponseExt resp = (DataArrayResponseExt) stsWorker.getObservations(request);
        StringBuilder sb = new StringBuilder();
        for (DataArray array : resp.getValue()) {
            for (Object o : array.getDataArray()) {
                List l = (List) o;
                Date d = (Date) l.get(2);
                sb.append(sdf2.format(d));
                List values = (List) l.get(3);
                for (Object value : values) {
                    sb.append(",");
                    if (!value.equals(Double.NaN)) {
                        sb.append(value);
                    }
                }
                sb.append("@@");
            }
        }
        return sb.toString() + '\n';
    }

    public static String getMeasure(SOSworker sosWorker, String offeringId, String observedProperty, String foi) throws CstlServiceException {
        return getMeasure(sosWorker, offeringId, observedProperty, foi, false);
    }

    public static String getMeasure(SOSworker sosWorker, String offeringId, String observedProperty, String foi, boolean inputColumn) throws CstlServiceException {
        List<String> fois = foi != null ? Arrays.asList(foi) : null;
        GetResultResponseType gr = (GetResultResponseType) sosWorker.getResult(new GetResultType("2.0.0", "SOS", offeringId, observedProperty, null, null, fois));
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
                if (pt.getDirectPosition().getOrdinate(0) == lat &&
                    pt.getDirectPosition().getOrdinate(1) == lon) {

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
                String key = pt.getDirectPosition().getOrdinate(0) + "-" + pt.getDirectPosition().getOrdinate(1);
                if (alreadyFound.contains(key)) {
                    throw new IllegalStateException("duplicated feature of interest for coord:" + key);
                }
                alreadyFound.add(pt.getDirectPosition().getOrdinate(0) + "-" + pt.getDirectPosition().getOrdinate(1));
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
}
