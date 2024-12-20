/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * (C) 2014, Geomatys
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package org.constellation.admin;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.sis.storage.image.WorldFileStoreProvider;
import org.apache.sis.parameter.DefaultParameterValueGroup;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.util.ComparisonMode;
import org.constellation.api.ProviderType;
import org.constellation.business.IProviderBusiness;
import org.constellation.exception.ConfigurationException;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Date: 18/09/14
 * Time: 10:28
 *
 * @author Alexis Manin (Geomatys)
 */
public class ProviderBusinessTest extends AbstractBusinessTest {

    @Test
    public void createFromDataStoreProvider() throws ConfigurationException, IOException, URISyntaxException {
        final String id = "myProvider";
        final DataStoreProvider cvgFactory = new WorldFileStoreProvider();
        final ParameterValueGroup config = cvgFactory.getOpenParameters().createValue();
        Path path = Files.createTempDirectory("ProviderBusinessTest");
        final URI dataPath = path.toUri();
        config.parameter(WorldFileStoreProvider.LOCATION).setValue(dataPath);
        Integer p = providerBusiness.create(id, IProviderBusiness.SPI_NAMES.DATA_SPI_NAME, config);
        // TODO : Re-activate when auto-generated equals will be done.
        //Assert.assertEquals("Created provider must be equal to read one.", p, pBusiness.getProvider(id));

        final DataProvider provider = DataProviders.getProvider(p);
        final ParameterValueGroup readConf =
                provider.getSource().groups("choice").get(0).groups(config.getDescriptor().getName().getCode()).get(0);

        // Disabled test because it uses deprecated geotk parameter implementation. We just check registered URL instead.
//        Assert.assertTrue("Written and read configuration must be equal." +
//                "\nExpected : \n"+config +
//                "\nFound : \n"+readConf, CRS.equalsApproximatively(config, readConf));
        Assert.assertEquals("Registered URL must be the same as read one.",
                dataPath, readConf.parameter(WorldFileStoreProvider.LOCATION).getValue());
    }

    @Test
    public void createFromProviderFactory() throws ConfigurationException, IOException {
        // Create data store configuration
        final String id = "myProvider2";
        final DataStoreProvider cvgFactory = new WorldFileStoreProvider();
        final ParameterValueGroup config = cvgFactory.getOpenParameters().createValue();

        Path path = Files.createTempDirectory("ProviderBusinessTest");
        final URI dataPath = path.toUri();
        config.parameter(WorldFileStoreProvider.LOCATION).setValue(dataPath);

        // Embed data store configuration into provider one.
        final DataProviderFactory factory = DataProviders.getFactory(
                ProviderBusiness.SPI_NAMES.DATA_SPI_NAME.name);
        final ParameterValueGroup providerConf = factory.getProviderDescriptor().createValue();
        providerConf.parameter("id").setValue(id);
        providerConf.parameter("providerType").setValue(ProviderBusiness.SPI_NAMES.DATA_SPI_NAME.name);
        final ParameterValueGroup choice =
                providerConf.groups("choice").get(0).addGroup(config.getDescriptor().getName().getCode());
        org.apache.sis.parameter.Parameters.copy(config, choice);

        Integer read = providerBusiness.storeProvider(id, ProviderType.LAYER, factory.getName(), providerConf);
        // TODO : Re-activate when auto-generated equals will be done.
        //Assert.assertEquals("Created provider must be equal to read one.", p, read);

        final DataProvider provider = DataProviders.getProvider(read);
        Assert.assertTrue("Written and read configuration must be equal.",
                ((DefaultParameterValueGroup)providerConf).equals(provider.getSource(),ComparisonMode.IGNORE_METADATA));
    }

}
