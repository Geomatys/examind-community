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

package org.constellation.generic.database;

import org.constellation.dto.service.config.generic.Automatic;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geotoolkit.test.xml.DocumentComparator;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.dto.contact.AccessConstraint;
import org.constellation.dto.contact.Contact;
import org.constellation.dto.contact.Details;
import org.constellation.dto.service.config.sos.ObservationFilter;
import org.constellation.dto.ParameterValues;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Node;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class GenericConfigurationXMLBindingTest {

    private MarshallerPool pool;
    private Unmarshaller unmarshaller;
    private Marshaller   marshaller;

    @Before
    public void setUp() throws JAXBException {
        pool = GenericDatabaseMarshallerPool.getInstance();
        unmarshaller = pool.acquireUnmarshaller();
        marshaller   = pool.acquireMarshaller();
    }

    @After
    public void tearDown() throws JAXBException {
        if (unmarshaller != null) {
            pool.recycle(unmarshaller);
        }
        if (marshaller != null) {
            pool.recycle(marshaller);
        }
    }


    @Test
    public void genericJsonMarshalingTest() throws Exception {

        Automatic config = new Automatic("FILESYSTEM", "data_dir_value");
        config.putParameter("testParam", "paramValue");
        config.setProfile("discovery");
        config.setIndexType("lucene");

        ObjectMapper mapper = new ObjectMapper();
        String expresult = "{\"type\":\"Automatic\",\"bdd\":null,\"thesaurus\":null,\"configurationDirectory\":null,"
                         + "\"format\":\"FILESYSTEM\",\"name\":null,\"profile\":\"discovery\",\"dataDirectory\":\"data_dir_value\",\"enableThread\":null,\"enableCache\":null,"
                         + "\"indexOnlyPublishedMetadata\":null,\"noIndexation\":null,\"harvester\":null,\"identifierDirectory\":null,"
                         + "\"customparameters\":{\"testParam\":\"paramValue\"},\"queries\":null,\"filterQueries\":null,\"indexType\":\"lucene\"}";
        String result = mapper.writeValueAsString(config);
        assertEquals(expresult, result);

    }

    @Test
    public void sosConfigMarshalingTest() throws Exception {

        SOSConfiguration sosConfig = new SOSConfiguration();

        Automatic config2 = new Automatic("FILESYSTEM", null);
        config2.setName("coriolis");
        sosConfig.getExtensions().add(config2);

        StringWriter sw = new StringWriter();
        marshaller.marshal(sosConfig, sw);

        String result =  sw.toString();
        String expResult =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"            + '\n' +
        "<ns2:SOSConfiguration xmlns:ns2=\"http://www.constellation.org/config\">" + '\n' +
        "    <ns2:extensions format=\"FILESYSTEM\" name=\"coriolis\">"             + '\n' +
        "        <customparameters/>"                                              + '\n' +
        "        <indexType>lucene-node</indexType>"                               + '\n' +
        "    </ns2:extensions>"                                                    + '\n' +
        "    <ns2:parameters/>"                                                    + '\n' +
        "</ns2:SOSConfiguration>" + '\n';

        final DocumentComparator comparator = new DocumentComparator(expResult, result);
        comparator.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
        comparator.compare();
    }

    /*
     *
     */
    @Test
    public void providerSourceUnMarshalingTest() throws Exception {
        String xml = "<?xml version='1.0' encoding='UTF-8'?>" + '\n'
                + "<source xmlns=\"http://www.geotoolkit.org/parameter\">" + '\n'
                + "   <id>shp-tasmania</id>" + '\n'
                + "   <shapefileFolder>" + '\n'
                + "     <path>/home/guilhem/shapefile/Tasmania_shp</path>" + '\n'
                + "     <namespace>shp</namespace>" + '\n'
                + "   </shapefileFolder>" + '\n'
                + "  <Layer>" + '\n'
                + "    <name>tasmania_cities</name>" + '\n'
                + "    <style>PointCircleBlack12</style>" + '\n'
                + "   </Layer>" + '\n'
                + "   <Layer>" + '\n'
                + "     <name>tasmania_roads</name>" + '\n'
                + "     <style>LineRed2</style>" + '\n'
                + "   </Layer>" + '\n'
                + " </source>";

        Object obj = unmarshaller.unmarshal(new StringReader(xml));

        assertTrue(obj instanceof JAXBElement);
        obj = ((JAXBElement)obj).getValue();

        assertTrue(obj instanceof Node);
        // TODO: check content
    }

    @Test
    public void serviceMarshalingTest() throws Exception {

        final Contact ctc = new Contact("firstname", "lastname", "org1", "pos1", "0600", "0800", "test@jj.com", "adr1", "city1", "state1", "34000", "france", "url1", null, null);
        final AccessConstraint cstr = new AccessConstraint("fees1", "constraint1", 5, 200, 300);
        final Details service = new Details("name1", "id1", Arrays.asList("kw1", "kw2"), "desc1", Arrays.asList("1.0.0", "2.0.0"), ctc, cstr, false, "FR");

        StringWriter sw = new StringWriter();
        marshaller.marshal(service, sw);

        String result =  sw.toString();
        String expResult =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
        "<ns2:details xmlns:ns2=\"http://www.constellation.org/config\">\n" +
        "  <ns2:description>desc1</ns2:description>\n" +
        "  <ns2:identifier>id1</ns2:identifier>\n" +
        "  <ns2:keywords>kw1</ns2:keywords>\n" +
        "  <ns2:keywords>kw2</ns2:keywords>\n" +
        "  <ns2:lang>FR</ns2:lang>\n" +
        "  <ns2:name>name1</ns2:name>\n" +
        "  <ns2:serviceConstraints>\n" +
        "    <ns2:accessConstraint>constraint1</ns2:accessConstraint>\n" +
        "    <ns2:fees>fees1</ns2:fees>\n" +
        "    <ns2:layerLimit>5</ns2:layerLimit>\n" +
        "    <ns2:maxHeight>300</ns2:maxHeight>\n" +
        "    <ns2:maxWidth>200</ns2:maxWidth>\n" +
        "  </ns2:serviceConstraints>\n" +
        "  <ns2:serviceContact>\n" +
        "    <ns2:address>adr1</ns2:address>\n" +
        "    <ns2:city>city1</ns2:city>\n" +
        "    <ns2:country>france</ns2:country>\n" +
        "    <ns2:email>test@jj.com</ns2:email>\n" +
        "    <ns2:fax>0800</ns2:fax>\n" +
        "    <ns2:firstname>firstname</ns2:firstname>\n" +
        "    <ns2:fullname>firstname lastname</ns2:fullname>\n" +
        "    <ns2:lastname>lastname</ns2:lastname>\n" +
        "    <ns2:organisation>org1</ns2:organisation>\n" +
        "    <ns2:phone>0600</ns2:phone>\n" +
        "    <ns2:position>pos1</ns2:position>\n" +
        "    <ns2:state>state1</ns2:state>\n" +
        "    <ns2:url>url1</ns2:url>\n" +
        "    <ns2:zipCode>34000</ns2:zipCode>\n" +
        "  </ns2:serviceContact>\n" +
        "  <ns2:transactional>false</ns2:transactional>\n" +
        "  <ns2:versions>1.0.0</ns2:versions>\n" +
        "  <ns2:versions>2.0.0</ns2:versions>\n" +
        "</ns2:details>" + '\n';

        final DocumentComparator comparator = new DocumentComparator(expResult, result);
        comparator.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
        comparator.compare();
    }

    @Test
    public void parameterValuesMarshalingTest() throws Exception {
        final ParameterValues values = new ParameterValues();
        values.getValues().put("providerId", "test");
        StringWriter sw = new StringWriter();
        marshaller.marshal(values, sw);
        // TODO: check values
    }

    @Test
    public void ObservationFilterMarshalingTest() throws Exception {
        final ObservationFilter values = new ObservationFilter();
        values.setSensorID("senord:1");
        values.setObservedProperty(Arrays.asList("phen1", "phen2"));
        values.setStart(new Date(System.currentTimeMillis()));
        values.setEnd(new Date(System.currentTimeMillis() + 10000));
        StringWriter sw = new StringWriter();
        marshaller.marshal(values, sw);
        // TODO:check values
    }
}
