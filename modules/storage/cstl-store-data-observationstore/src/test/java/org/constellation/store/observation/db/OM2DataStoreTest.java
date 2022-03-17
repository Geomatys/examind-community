/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2010-2014 Geomatys.
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

package org.constellation.store.observation.db;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.parameter.DefaultParameterValueGroup;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStore;
import org.constellation.test.utils.SpringTestRunner;
import org.constellation.util.SQLUtilities;
import org.constellation.util.Util;
import org.geotoolkit.storage.AbstractReadingTests;
import org.geotoolkit.internal.sql.ScriptRunner;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.util.NamesExt;
import org.junit.runner.RunWith;
import org.locationtech.jts.geom.Geometry;
import org.opengis.util.GenericName;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;


/**
 *
 * @author Guilhem Legal (Geomatys)
 * @module pending
 */
@ActiveProfiles({"standard"})
@ContextConfiguration("classpath:/cstl/spring/test-no-hazelcast.xml")
@RunWith(SpringTestRunner.class)
public class OM2DataStoreTest extends AbstractReadingTests{

    private static DataSource ds;
    private static DataStore store;
    private static Set<GenericName> names = new HashSet<>();
    private static List<ExpectedResult> expecteds = new ArrayList<>();
    private static boolean configured = false;

    @PostConstruct
    public void setUpClass() throws Exception {
        if (!configured) {
            try{
                final String url = "jdbc:derby:memory:TestOM2;create=true";
                ds = SQLUtilities.getDataSource(url);

                Connection con = ds.getConnection();

                final ScriptRunner exec = new ScriptRunner(con);
                String sql = IOUtilities.toString(Util.getResourceAsStream("org/constellation/om2/structure_observations.sql"));
                sql = sql.replace("$SCHEMA", "");
                exec.run(sql);
                exec.run(Util.getResourceAsStream("org/constellation/sql/sos-data-om2.sql"));

                DefaultParameterValueGroup parameters = (DefaultParameterValueGroup) OM2FeatureStoreFactory.PARAMETERS_DESCRIPTOR.createValue();
                parameters.getOrCreate(OM2FeatureStoreFactory.IDENTIFIER).setValue("om2");
                parameters.getOrCreate(OM2FeatureStoreFactory.DBTYPE).setValue("OM2");
                parameters.getOrCreate(OM2FeatureStoreFactory.SGBDTYPE).setValue("derby");
                parameters.getOrCreate(OM2FeatureStoreFactory.DERBYURL).setValue(url);

                OM2FeatureStoreFactory factory = new OM2FeatureStoreFactory();

                store =  factory.open(parameters);

                final String nsCstl = "http://constellation.org/om2";
                final String nsGML = "http://www.opengis.net/gml";
                final GenericName name = NamesExt.create(nsCstl, "Sensor");
                names.add(name);

                final FeatureTypeBuilder featureTypeBuilder = new FeatureTypeBuilder();
                featureTypeBuilder.setName(name);
                featureTypeBuilder.addAttribute(String.class).setName(nsCstl, "id").addRole(AttributeRole.IDENTIFIER_COMPONENT);
                featureTypeBuilder.addAttribute(Geometry.class).setName(nsCstl, "position").addRole(AttributeRole.DEFAULT_GEOMETRY);

                int size = 14;
                GeneralEnvelope env = new GeneralEnvelope(CRS.forCode("EPSG:27582"));
                env.setRange(0, 5.0, 2846612.385024995);
                env.setRange(1, -1628761.942286251, 1731368);

                final ExpectedResult res = new ExpectedResult(name,
                        featureTypeBuilder.build(), size, env);
                expecteds.add(res);
                configured = true;

            } catch(Exception ex) {
                Logger.getLogger("org.constellation.store.observation.db").log(Level.SEVERE, "Error at test initialization.", ex);
            }
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
}
