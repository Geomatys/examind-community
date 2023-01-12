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

package org.constellation.sos.io.filesystem;

import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.sensor.SensorReader;
import org.geotoolkit.sml.xml.AbstractSensorML;
import org.geotoolkit.sml.xml.SensorMLMarshallerPool;
import org.geotoolkit.util.StringUtilities;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.constellation.api.CommonConstants.SENSORML_100_FORMAT_V100;
import static org.constellation.api.CommonConstants.SENSORML_100_FORMAT_V200;
import static org.constellation.api.CommonConstants.SENSORML_101_FORMAT_V100;
import static org.constellation.api.CommonConstants.SENSORML_101_FORMAT_V200;

/**
 *
 * @author Guilhem Legal (geomatys)
 */
public class FileSensorReader implements SensorReader {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.sos.io.filesystem");

    /**
     * A JAXB unmarshaller used to unmarshall the xml files.
     */
    private static final MarshallerPool MARSHALLER_POOL = SensorMLMarshallerPool.getInstance();

    /**
     * The directory where the data file are stored
     */
    private final Path dataDirectory;

    private final Map<String, List<String>> acceptedSensorMLFormats = new HashMap<>();

    public FileSensorReader(final Path directory, final Map<String, Object> properties) throws DataStoreException  {
        //we initialize the unmarshaller
        dataDirectory  = directory;
        if (dataDirectory == null) {
            throw new DataStoreException("The sensor data directory is null");
        } else if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                throw new DataStoreException("unable to build the directory:" + dataDirectory.toAbsolutePath().toString());
            }
        }
        final String smlFormats100 = (String) properties.get("smlFormats100");
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

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractSensorML getSensor(final String sensorId) throws DataStoreException {
        final String fileName = sensorId.replace(':', 'µ') + ".xml";
        Path sensorFile = dataDirectory.resolve(fileName);
        if (Files.exists(sensorFile)){
            try (InputStream is = Files.newInputStream(sensorFile)) {
                final Unmarshaller unmarshaller = MARSHALLER_POOL.acquireUnmarshaller();
                Object unmarshalled = unmarshaller.unmarshal(is);
                MARSHALLER_POOL.recycle(unmarshaller);
                if (unmarshalled instanceof JAXBElement jb) {
                    unmarshalled = jb.getValue();
                }
                if (unmarshalled instanceof AbstractSensorML asml) {
                    return asml;
                } else {
                    throw new DataStoreException("The form unmarshalled is not a sensor");
                }
            } catch (JAXBException ex) {
                throw new DataStoreException("JAXBException while unmarshalling the sensor", ex);
            } catch (IOException ex) {
                throw new DataStoreException("IOException while reading the sensor file", ex);
            }
        } else {
            LOGGER.log(Level.INFO, "the file: {0} does not exist", sensorFile.toAbsolutePath().toString());
            throw new DataStoreException("this sensor is not registered in the database:" + sensorId);
            // here we loose the exception code and locator: TODO => INVALID_PARAMETER_VALUE, "procedure");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getSensorNames() throws DataStoreException{
        final List<String> result = new ArrayList<>();
        if (Files.isDirectory(dataDirectory)) {

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDirectory)) {
                for (Path sensorFile : stream) {
                    String sensorID = sensorFile.getFileName().toString();
                    if (!sensorID.endsWith("~")) {
                        final int suffixPos = sensorID.indexOf(".xml");
                        if (suffixPos != -1) {
                            sensorID = sensorID.substring(0, suffixPos);
                            sensorID = sensorID.replace('µ', ':');
                            result.add(sensorID);
                        }
                    }
                }
            } catch (IOException e) {
                throw new DataStoreException("Error during data directory scanning", e);
            }
        }
        return result;
    }

    @Override
    public void removeFromCache(String sensorID) {
        // do nothing no cache
    }

    @Override
    public int getSensorCount() throws DataStoreException{
        return getSensorNames().size();
    }
}
