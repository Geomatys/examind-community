/*
 *    Examind community - An open source and standard compliant SDI
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
package org.constellation.store.observation.db.sensor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.storage.DataStoreException;
import static org.constellation.api.CommonConstants.SENSORML_100_FORMAT_V100;
import static org.constellation.api.CommonConstants.SENSORML_100_FORMAT_V200;
import static org.constellation.api.CommonConstants.SENSORML_101_FORMAT_V100;
import static org.constellation.api.CommonConstants.SENSORML_101_FORMAT_V200;
import org.constellation.store.observation.db.OM2BaseReader;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.sensor.SensorReader;
import org.geotoolkit.sml.xml.AbstractClassification;
import org.geotoolkit.sml.xml.AbstractIdentification;
import org.geotoolkit.sml.xml.AbstractSensorML;
import org.geotoolkit.sml.xml.Component;
import org.geotoolkit.sml.xml.SmlXMLFactory;
import org.geotoolkit.util.StringUtilities;

/**
 *
 *  @author Guilhem Legal (Geomatys)
 */
public class OM2SensorReader extends OM2BaseReader implements SensorReader {
    
    protected final DataSource source;
    
    private final Map<String, List<String>> acceptedSensorMLFormats = new HashMap<>();
    
    private static final String SML_VERSION = "1.0.1";
    
    private static final AbstractClassification PROFILE_CLASSIF = SmlXMLFactory.createClassification(SML_VERSION, "data-type", "Profile", "http://sensorml.com/ont/swe/property/Profile");
    private static final AbstractClassification TIMESERIE_CLASSIF = SmlXMLFactory.createClassification(SML_VERSION, "data-type", "Timeseries", "http://sensorml.com/ont/swe/property/Timeseries");
    
    
    public OM2SensorReader(final DataSource source, final Map<String, Object> properties) throws DataStoreException {
        super(properties, true);
        this.source = source;final String smlFormats100 = (String) properties.get("smlFormats100");
        if (smlFormats100 != null) {
            acceptedSensorMLFormats.put("1.0.0", StringUtilities.toStringList(smlFormats100));
        } else {
            acceptedSensorMLFormats.put("1.0.0", Arrays.asList(SENSORML_100_FORMAT_V100,
                                                               SENSORML_101_FORMAT_V100));
        }
        
        final String smlFormats200 = (String) properties.get("smlFormats200");
        if (smlFormats200 != null) {
            acceptedSensorMLFormats.put("2.0.0", StringUtilities.toStringList(smlFormats200));
        } else {
            acceptedSensorMLFormats.put("2.0.0", Arrays.asList(SENSORML_100_FORMAT_V200,
                                                               SENSORML_101_FORMAT_V200));
        }
    }
    
    @Override
    public Map<String, List<String>> getAcceptedSensorMLFormats() {
        return acceptedSensorMLFormats;
    }

    @Override
    public AbstractSensorML getSensor(String sensorID) throws DataStoreException {
        // TODO hard coded SML for now until procedure table will have a metadata column
        Procedure process;
        try (Connection c = source.getConnection()) {
            process = getProcess(sensorID, c);
            if (process == null) return null;
        } catch (SQLException ex) {
            throw new DataStoreException(ex);
        }
        final String procedureOMType = (String) process.getProperties().getOrDefault("type", "timeseries");

        AbstractIdentification ident = SmlXMLFactory.createIdentification(SML_VERSION, sensorID);
        AbstractClassification classif = "profile".equals(procedureOMType) ? PROFILE_CLASSIF : TIMESERIE_CLASSIF;
        Component compo = SmlXMLFactory.createComponent(SML_VERSION, ident, classif);
        compo.setName(new DefaultIdentifier(process.getName()));
        compo.setDescription(process.getDescription());
        return SmlXMLFactory.createAbstractSensorML(SML_VERSION, compo);
    }

    @Override
    public Collection<String> getSensorNames() throws DataStoreException {
        List<String> results = new ArrayList<>();
        try(final Connection c               = source.getConnection();
            final Statement currentStatement = c.createStatement();
            final ResultSet result           = currentStatement.executeQuery("SELECT pr.\"id\" FROM \"" + schemaPrefix + "om\".\"procedures\" pr")) {
            while (result.next()) {
                results.add(result.getString(1));
            }
        } catch (SQLException ex) {
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage());
        }
        return results;
    }

    @Override
    public int getSensorCount() throws DataStoreException {
        try(final Connection c               = source.getConnection();
            final Statement currentStatement = c.createStatement();
            final ResultSet result           = currentStatement.executeQuery("SELECT COUNT(pr.\"id\") FROM \"" + schemaPrefix + "om\".\"procedures\" pr")) {
            if (result.next()) {
                return result.getInt(1);
            }
            throw new DataStoreException("the count request does not return anything!");
        } catch (SQLException ex) {
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage());
        }
    }
    
    @Override
    public void removeFromCache(String sensorID) {
        // do nothing
    }

    @Override
    public void destroy() {
        // do nothing
    }
    
}
