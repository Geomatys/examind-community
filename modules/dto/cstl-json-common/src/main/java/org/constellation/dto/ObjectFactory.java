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
package org.constellation.dto;

import org.constellation.dto.service.config.csw.BriefNodeList;
import org.constellation.dto.service.config.csw.BriefNode;
import org.constellation.dto.service.config.sos.ObservationFilter;
import org.constellation.dto.metadata.MetadataLists;
import org.constellation.dto.service.config.wxs.LayerList;
import org.constellation.dto.service.config.wxs.Layer;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.dto.contact.AccessConstraint;
import org.constellation.dto.contact.Contact;
import org.constellation.dto.contact.Details;
import org.constellation.dto.service.config.csw.HarvestTasks;
import org.constellation.dto.service.config.csw.HarvestTask;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.dto.service.config.wps.ProcessContext;
import org.constellation.dto.service.ServiceReport;
import org.constellation.dto.service.InstanceReport;
import org.constellation.dto.service.Instance;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;
import org.constellation.dto.AcknowlegementType;
import org.constellation.dto.BandDescription;
import org.constellation.dto.CoverageDataDescription;
import org.constellation.dto.DataBrief;
import org.constellation.dto.ExceptionReport;
import org.constellation.dto.FeatureDataDescription;
import org.constellation.dto.MailingProperties;
import org.constellation.dto.ParameterValues;
import org.constellation.dto.PropertyDescription;
import org.constellation.dto.SimpleValue;
import org.constellation.dto.StringList;
import org.constellation.dto.StringMap;
import org.constellation.dto.StringTreeNode;
import org.constellation.dto.StyleBrief;

/**
 * @author Benjamin Garcia (Geomatys)
 */
@XmlRegistry
public class ObjectFactory {

    public static final QName SOURCE_QNAME = new QName("http://www.geotoolkit.org/parameter", "source");
    public static final QName LAYER_QNAME  = new QName("http://www.geotoolkit.org/parameter", "Layer");
    public static final QName INPUT_QNAME  = new QName("http://www.geotoolkit.org/parameter", "input");


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.constellation.dto
     *
     */
    public ObjectFactory() {
    }

    public MailingProperties createMailingProperties() {
        return new MailingProperties();
    }

    /**
     * Create an instance of {@link AcknowlegementType }
     *
     */
    public AcknowlegementType createAcknowlegementType() {
        return new AcknowlegementType();
    }

    /**
     * Create an instance of {@link SOSConfiguration }
     *
     */
    public SOSConfiguration createSOSConfiguration() {
        return new SOSConfiguration();
    }

    /**
     * Create an instance of {@link HarvestTasks }
     *
     */
    public HarvestTasks createHarvestTasks() {
        return new HarvestTasks();
    }

    /**
     * Create an instance of {@link HarvestTask }
     *
     */
    public HarvestTask createHarvestTask() {
        return new HarvestTask();
    }

    public LayerContext createLayerContext() {
        return new LayerContext();
    }

    public ProcessContext createProcessContext() {
        return new ProcessContext();
    }

    public LayerList createLayerList() {
        return new LayerList();
    }

    public Layer createLayer() {
        return new Layer();
    }

    public InstanceReport createInstanceReport() {
        return new InstanceReport();
    }

    public ServiceReport createServiceReport() {
        return new ServiceReport();
    }

    public Instance createInstance() {
        return new Instance();
    }

    public ExceptionReport createExceptionReport() {
        return new ExceptionReport();
    }

    public StringList createSimpleList(){
        return new StringList();
    }

    public BriefNode createSimpleBriefNode(){
        return new BriefNode();
    }

    public BriefNodeList createSimpleBriefNodeList(){
        return new BriefNodeList();
    }

    public StringMap createSimpleMap(){
        return new StringMap();
    }

    public StringTreeNode createStringTreeNode(){
        return new StringTreeNode();
    }

    public StyleBrief createStyleBrief() {
        return new StyleBrief();
    }

    public DataBrief createDataBrief() {
        return new DataBrief();
    }

    @XmlElementDecl(namespace = "http://www.geotoolkit.org/parameter", name = "source")
    public JAXBElement<Object> createSource(Object value) {
        return new JAXBElement<>(SOURCE_QNAME, Object.class, null, value);
    }

    @XmlElementDecl(namespace = "http://www.geotoolkit.org/parameter", name = "Layer")
    public JAXBElement<Object> createLayer(Object value) {
        return new JAXBElement<>(LAYER_QNAME, Object.class, null, value);
    }

    @XmlElementDecl(namespace = "http://www.geotoolkit.org/parameter", name = "input")
    public JAXBElement<Object> createInput(Object value) {
        return new JAXBElement<>(LAYER_QNAME, Object.class, null, value);
    }

    public Details createService(){
        return new Details();
    }

    public MetadataLists createMetadataLists() {
        return new MetadataLists();
    }

    public CoverageDataDescription createCoverageDataDescription(){
        return new CoverageDataDescription();
    }

    public FeatureDataDescription createFeatureDataDescription(){
        return new FeatureDataDescription();
    }

    public BandDescription createBandDescription(){
        return new BandDescription();
    }

    public PropertyDescription createPropertyDescription(){
        return new PropertyDescription();
    }

    public AccessConstraint createAccessConstraint(){
        return new AccessConstraint();
    }

    public Contact getContact(){
        return new Contact();
    }

    public ParameterValues createParameterValues(){
        return new ParameterValues();
    }

    public ObservationFilter createObservationFilter(){
        return new ObservationFilter();
    }

    public SimpleValue createSimpleValue() {
        return new SimpleValue();
    }
}
