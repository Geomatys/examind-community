/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Geomatys
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

package org.constellation.sos;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.xml.LegacyNamespaces;
import org.apache.sis.parameter.DefaultParameterValueGroup;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStore;
import org.apache.sis.xml.XML;
import static org.constellation.provider.observationstore.ObservationStoreProviderWriteTest.assertEqualsMeasurement;
import static org.constellation.provider.observationstore.ObservationStoreProviderWriteTest.assertEqualsObservation;
import org.constellation.store.observation.db.SOSDatabaseObservationStore;
import org.constellation.store.observation.db.SOSDatabaseObservationStoreFactory;
import org.constellation.util.SQLUtilities;
import org.constellation.util.Util;
import org.geotoolkit.storage.AbstractReadingTests;
import org.geotoolkit.feature.xml.GMLConvention;
import org.geotoolkit.internal.sql.ScriptRunner;
import org.geotoolkit.nio.IOUtilities;
import static org.geotoolkit.observation.OMUtils.OBSERVATION_QNAME;
import static org.geotoolkit.observation.OMUtils.MEASUREMENT_QNAME;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.observation.xml.AbstractObservation;
import org.geotoolkit.sos.xml.ResponseModeType;
import org.geotoolkit.swe.xml.DataArrayProperty;
import org.geotoolkit.sos.xml.SOSMarshallerPool;
import org.geotoolkit.observation.model.ExtractionResult;
import org.geotoolkit.util.NamesExt;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.opengis.observation.Observation;
import org.opengis.util.GenericName;


/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class SOSDatabaseDataStoreTest extends AbstractReadingTests{

    private static DataSource ds;
    private static DataStore store;
    private static final Set<GenericName> names = new HashSet<>();
    private static final List<ExpectedResult> expecteds = new ArrayList<>();
    static {
        try {
            final String url = "jdbc:derby:memory:TestOM;create=true";
            ds = SQLUtilities.getDataSource(url);

            Connection con = ds.getConnection();

            final ScriptRunner exec = new ScriptRunner(con);
            String sql = IOUtilities.toString(Util.getResourceAsStream("org/constellation/om2/structure_observations.sql"));
            sql = sql.replace("$SCHEMA", "");
            exec.run(sql);
            exec.run(Util.getResourceAsStream("org/constellation/sql/sos-data-om2.sql"));

            DefaultParameterValueGroup parameters = (DefaultParameterValueGroup) SOSDatabaseObservationStoreFactory.PARAMETERS_DESCRIPTOR.createValue();
            parameters.getOrCreate(SOSDatabaseObservationStoreFactory.SGBDTYPE).setValue("derby");
            parameters.getOrCreate(SOSDatabaseObservationStoreFactory.DERBYURL).setValue(url);

            store = new SOSDatabaseObservationStore(parameters);

            final String nsOM = "http://www.opengis.net/sampling/1.0";
            final String nsGML = "http://www.opengis.net/gml";
            final GenericName name = NamesExt.create(nsOM, "SamplingPoint");
            names.add(name);

            final FeatureTypeBuilder featureTypeBuilder = new FeatureTypeBuilder();
            featureTypeBuilder.setName(name);
            featureTypeBuilder.setSuperTypes(GMLConvention.ABSTRACTFEATURETYPE_31);
            featureTypeBuilder.addAttribute(String.class).setName(nsGML, "description").setMinimumOccurs(0).setMaximumOccurs(1);
            featureTypeBuilder.addAttribute(String.class).setName(nsGML, "name").setMinimumOccurs(1).setMaximumOccurs(Integer.MAX_VALUE);
            featureTypeBuilder.addAttribute(String.class).setName(nsOM, "sampledFeature")
                    .setMinimumOccurs(0).setMaximumOccurs(Integer.MAX_VALUE).addCharacteristic(GMLConvention.NILLABLE_CHARACTERISTIC);
            featureTypeBuilder.addAttribute(Geometry.class).setName(nsOM, "position").setCRS(CRS.forCode("EPSG:27582")).addRole(AttributeRole.DEFAULT_GEOMETRY);

            int size = 6;
            GeneralEnvelope env = new GeneralEnvelope(CRS.forCode("EPSG:27582"));
            env.setRange(0, -30.711, 70800);
            env.setRange(1,    10.0, 2567987);

            final ExpectedResult res = new ExpectedResult(name,
                    featureTypeBuilder.build(), size, env);
            expecteds.add(res);

        } catch(Exception ex) {
            Logger.getLogger("org.constellation.store.observation.db").log(Level.SEVERE, "Error at test initialization.", ex);
        }
    }

    @Override
    protected DataStore getDataStore() {
        return store;
    }

    @Override
    protected Set<GenericName> getExpectedNames() {
        return names;
    }

    @Override
    protected List<ExpectedResult> getReaderTests() {
        return expecteds;
    }

    @Test
    public void readObservationByIdTest() throws Exception {
        Assert.assertTrue(store instanceof ObservationStore);
        ObservationStore omStore = (ObservationStore) store;
        ObservationReader reader = omStore.getReader();

        Assert.assertNotNull(reader);

        Observation obs = reader.getObservation("urn:ogc:object:observation:GEOM:2000", OBSERVATION_QNAME, ResponseModeType.INLINE, "2.0.0");
        Assert.assertTrue(obs instanceof AbstractObservation);
        AbstractObservation result = (AbstractObservation) obs;

        Assert.assertTrue(result.getResult() instanceof DataArrayProperty);
        DataArrayProperty resultDAP = (DataArrayProperty)result.getResult();

        String expectedValues = "2009-05-01T13:47:00.0,4.5@@"
                              + "2009-05-01T14:00:00.0,5.9@@"
                              + "2009-05-01T14:01:00.0,8.9@@"
                              + "2009-05-01T14:02:00.0,7.8@@"
                              + "2009-05-01T14:03:00.0,9.9@@";
        Assert.assertEquals(expectedValues, resultDAP.getDataArray().getValues());
    }

    @Test
    public void readerTest() throws Exception {
        Unmarshaller u = SOSMarshallerPool.getInstance().acquireUnmarshaller();
        u.setProperty(XML.METADATA_VERSION, LegacyNamespaces.VERSION_2007);

        Object o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/quality_sensor_observation.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation expected = (AbstractObservation) o;

        o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/quality_sensor_measurement.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation measExpected = (AbstractObservation) o;

        SOSMarshallerPool.getInstance().recycle(u);

        Assert.assertTrue(store instanceof ObservationStore);
        ObservationStore omStore = (ObservationStore) store;
        ObservationReader reader = omStore.getReader();

        Assert.assertNotNull(reader);

        Observation obs = reader.getObservation("urn:ogc:object:observation:GEOM:6001", OBSERVATION_QNAME, ResponseModeType.INLINE, "2.0.0");
        Assert.assertTrue(obs instanceof AbstractObservation);
        AbstractObservation result = (AbstractObservation) obs;

        assertEqualsObservation(expected, result);

        obs = reader.getObservation("urn:ogc:object:observation:GEOM:6001-2-1", MEASUREMENT_QNAME, ResponseModeType.INLINE, "2.0.0");
        Assert.assertTrue(obs instanceof AbstractObservation);
        result = (AbstractObservation) obs;

        assertEqualsMeasurement(measExpected, result, true);
    }

    @Test
    public void getProceduresTest() throws Exception {
        Assert.assertTrue(store instanceof ObservationStore);
        ObservationStore omStore = (ObservationStore) store;

        List<ExtractionResult.ProcedureTree> procedures = omStore.getProcedures();
        Assert.assertEquals(15, procedures.size());
    }
}
