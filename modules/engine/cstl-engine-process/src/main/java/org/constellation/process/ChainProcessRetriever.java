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


package org.constellation.process;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.identification.DefaultServiceIdentification;
import org.constellation.admin.SpringHelper;
import org.constellation.exception.ConstellationException;
import org.constellation.business.IProcessBusiness;
import org.constellation.dto.process.ChainProcess;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.processing.chain.ChainProcessDescriptor;
import org.geotoolkit.processing.chain.model.Chain;
import org.geotoolkit.processing.chain.model.ChainMarshallerPool;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.identification.Identification;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ChainProcessRetriever {

    public ChainProcessRetriever() {
    }

    public static List<ProcessDescriptor> getChainDescriptors(String factory) throws ConstellationException {
        final List<ProcessDescriptor> results = new ArrayList<>();
        final IProcessBusiness processBusiness = SpringHelper.getBean(IProcessBusiness.class);
        if (processBusiness != null) {
            List<ChainProcess> chainModels = processBusiness.getChainModels();
            for (ChainProcess chainModel : chainModels) {
                final Chain chain = convertToGeotk(chainModel);
                Identification registryId;
                if (factory == null) {
                    registryId = buildIdentification(chain.getName());
                } else {
                    registryId = buildIdentification(factory);
                }
                final ProcessDescriptor desc = new ChainProcessDescriptor(chain, registryId);
                results.add(desc);
            }
        }
        return results;
    }

    public static List<ProcessDescriptor> getChainDescriptors() throws ConstellationException {
        return getChainDescriptors(null);
    }

    public static List<Chain> getChainModels() throws ConstellationException {
        final List<Chain> results = new ArrayList<>();
        final IProcessBusiness processBusiness = SpringHelper.getBean(IProcessBusiness.class);
        if (processBusiness != null) {
            List<ChainProcess> chains = processBusiness.getChainModels();
            for (ChainProcess chain : chains) {
                results.add(convertToGeotk(chain));
            }
        }
        return results;
    }

    public static ChainProcess convertToDto(Chain chain) throws ConstellationException {
        final String code = chain.getName();
        String config = null;
        try {
            final Marshaller m = ChainMarshallerPool.getInstance().acquireMarshaller();
            final StringWriter sw = new StringWriter();
            m.marshal(chain, sw);
            ChainMarshallerPool.getInstance().recycle(m);
            config = sw.toString();
        } catch (JAXBException ex) {
            throw new ConstellationException("Unable to marshall chain configuration",ex);
        }
        final ChainProcess process = new ChainProcess();

        process.setAuth("examind-dynamic");
        process.setCode(code);
        process.setConfig(config);
        return process;
    }

    public static Chain convertToGeotk(ChainProcess chain) throws ConstellationException {
        try {
            final Unmarshaller u = ChainMarshallerPool.getInstance().acquireUnmarshaller();
            final Chain c = (Chain) u.unmarshal(new StringReader(chain.getConfig()));
            ChainMarshallerPool.getInstance().recycle(u);
            return c;
        } catch (JAXBException ex) {
            throw new ConstellationException("Unable to unmarshall chain configuration:" + chain.getId(), ex);
        }
    }

    private static Identification buildIdentification(final String name) {
        final DefaultServiceIdentification ident = new DefaultServiceIdentification();
        final Identifier id = new DefaultIdentifier(name);
        final DefaultCitation citation = new DefaultCitation(name);
        citation.setIdentifiers(Collections.singleton(id));
        ident.setCitation(citation);
        return ident;
    }

}
