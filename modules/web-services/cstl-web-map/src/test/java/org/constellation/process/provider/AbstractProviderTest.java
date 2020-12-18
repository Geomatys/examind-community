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
package org.constellation.process.provider;


import org.constellation.configuration.ConfigDirectory;
import org.constellation.exception.ConfigurationException;
import org.constellation.process.AbstractProcessTest;
import org.constellation.provider.DataProviders;
import org.constellation.provider.DataProviderFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IProviderBusiness;
import org.constellation.exception.ConstellationException;
import org.geotoolkit.nio.IOUtilities;

/**
 *
 * @author Quentin Boileau (Geomatys).
 */
public abstract class AbstractProviderTest extends AbstractProcessTest {

    protected static URI EMPTY_CSV;
    // dataStore service
    protected static DataProviderFactory DATASTORE_SERVICE;

    protected IProviderBusiness providerBusiness;

    protected AbstractProviderTest(final String processName) {
        super(processName);
    }

    @PostConstruct
    public void fillDatastoreService() {
        DATASTORE_SERVICE = DataProviders.getFactory("data-store");
        providerBusiness = SpringHelper.getBean(IProviderBusiness.class);
    }

    @BeforeClass
    public static void initFolder() throws Exception {

        final File configDirectory = ConfigDirectory.setupTestEnvironement("ProcessProviderTest").toFile();
        final File providerDirectory = new File(configDirectory, "provider");
        providerDirectory.mkdir();

        File csv = new File(configDirectory, "file.csv");
        IOUtilities.writeString("id;name", csv.toPath());
        EMPTY_CSV = csv.toURI();

    }

    @AfterClass
    public static void destroyFolder() {
        ConfigDirectory.shutdownTestEnvironement("ProcessProviderTest");
    }

    /**
     * Create a CSV provider for test purpose.
     * @param service
     * @param providerID
     * @return
     * @throws MalformedURLException
     */
    protected static ParameterValueGroup buildCSVProvider(final DataProviderFactory service, final String providerID,
                                                          final URI url, char separator) throws MalformedURLException {

        ParameterDescriptorGroup desc = service.getProviderDescriptor();

        if (desc != null) {
            final ParameterDescriptorGroup sourceDesc = desc;
            final ParameterValueGroup sourceValue = sourceDesc.createValue();
            sourceValue.parameter("id").setValue(providerID);

            final ParameterValueGroup choiceValue = sourceValue.groups("choice").get(0);
            final ParameterValueGroup csvValue = choiceValue.addGroup("geotk_csv");
            //csvValue.parameter("identifier").setValue("geotk_csv");
            csvValue.parameter("location").setValue(url);
            csvValue.parameter("separator").setValue(Character.valueOf(separator));

            return sourceValue;
        } else {
            //error
            return null;
        }
    }

    /**
     * Register a provider.
     * @param providerSource
     */
    protected Integer addProvider(String id,ParameterValueGroup providerSource) throws ConfigurationException {
        return providerBusiness.create(id, DATASTORE_SERVICE.getName(), providerSource);
    }

    /**
     * Un-register a provider
     * @param id
     */
    protected void removeProvider(String id) throws ConstellationException {
        providerBusiness.removeProvider(id);
    }
}
