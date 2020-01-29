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

import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStore;
import org.constellation.store.observation.db.SOSDatabaseObservationStoreFactory;
import org.constellation.util.Util;
import org.geotoolkit.storage.AbstractReadingTests;
import org.geotoolkit.feature.xml.GMLConvention;
import org.geotoolkit.internal.sql.DefaultDataSource;
import org.geotoolkit.internal.sql.ScriptRunner;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.util.NamesExt;
import org.locationtech.jts.geom.Geometry;
import org.opengis.util.GenericName;


/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class SOSDatabaseDataStoreTest extends AbstractReadingTests{

    private static DefaultDataSource ds;
    private static DataStore store;
    private static final Set<GenericName> names = new HashSet<>();
    private static final List<ExpectedResult> expecteds = new ArrayList<>();
    static{
        try{
            final String url = "jdbc:derby:memory:TestOM;create=true";
            ds = new DefaultDataSource(url);

            Connection con = ds.getConnection();

            final ScriptRunner exec = new ScriptRunner(con);
            String sql = IOUtilities.toString(Util.getResourceAsStream("org/constellation/om2/structure_observations.sql"));
            sql = sql.replace("$SCHEMA", "");
            exec.run(sql);
            exec.run(getResourceAsStream("org/constellation/sql/sos-data-om2.sql"));

            final Map params = new HashMap<>();
            params.put("dbtype", "OM");
            params.put(SOSDatabaseObservationStoreFactory.SGBDTYPE.getName().toString(), "derby");
            params.put(SOSDatabaseObservationStoreFactory.DERBYURL.getName().toString(), url);

            store = DataStores.open(params);

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
            env.setRange(1, 134.196, 2567987);

            final ExpectedResult res = new ExpectedResult(name,
                    featureTypeBuilder.build(), size, env);
            expecteds.add(res);

        }catch(Exception ex){
            ex.printStackTrace();
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

    public static InputStream getResourceAsStream(final String url) {
        final ClassLoader cl = getContextClassLoader();
        return cl.getResourceAsStream(url);
    }

    public static ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }
}
