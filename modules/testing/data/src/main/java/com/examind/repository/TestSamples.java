package com.examind.repository;

import java.util.ArrayList;
import java.util.Arrays;
import org.constellation.dto.CstlUser;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.constellation.dto.Data;
import org.constellation.dto.DataSet;
import org.constellation.dto.DataSource;
import org.constellation.dto.Layer;
import org.constellation.dto.MapContextDTO;
import org.constellation.dto.ProviderBrief;
import org.constellation.dto.Sensor;
import org.constellation.dto.Style;
import org.constellation.dto.UserWithRole;
import org.constellation.dto.metadata.Attachment;
import org.constellation.dto.metadata.MetadataBbox;
import org.constellation.dto.metadata.MetadataComplete;
import org.constellation.dto.process.ChainProcess;
import org.constellation.dto.process.Task;
import org.constellation.dto.process.TaskParameter;
import org.constellation.dto.service.Service;
import org.constellation.dto.thesaurus.Thesaurus;

public class TestSamples {

    public static UserWithRole newAdminUser() {
        UserWithRole user = new UserWithRole();
        user.setFirstname("olivier");
        user.setLastname("Nouguier");
        user.setLogin("olivier");
        user.setEmail("olvier.nouguier@gmail.com");
        user.setPassword("zozozozo");
        user.setActive(Boolean.TRUE);
        user.setLocale("fr");
        user.setRoles(Arrays.asList("admin"));
        return user;
    }

    public static UserWithRole newDataUser() {
        UserWithRole user = new UserWithRole();
        user.setFirstname("Pedro");
        user.setLastname("Ramirez");
        user.setLogin("pedra");
        user.setEmail("pedro.ramirez@gmail.com");
        user.setPassword("ramiramou");
        user.setActive(Boolean.TRUE);
        user.setLocale("fr");
        user.setRoles(Arrays.asList("data", "publisher"));
        return user;
    }

    public static UserWithRole newQuoteUser() {
        UserWithRole user = new UserWithRole();
        user.setFirstname("Pe'dro");
        user.setLastname("Rami''rez");
        user.setLogin("pe''dra");
        user.setEmail("pedro.'ramirez@gmail.';com");
        user.setPassword("rami'ramou");
        user.setActive(Boolean.FALSE);
        user.setLocale("f'r");
        user.setRoles(Arrays.asList("d'ata", "publishe'r"));
        user.setForgotPasswordUuid("uu'id");
        return user;
    }

    public static Data newData1(Integer ownerId, Integer providerId, Integer datasetId) {
        return newData(ownerId, providerId, datasetId, "test data 1", "", "COVERAGE", false, true, false, true, null);
    }

    public static Data newData2(Integer ownerId, Integer providerId, Integer datasetId) {
        return newData(ownerId, providerId, datasetId, "test data 2", "", "VECTOR", false, true, true, false, "point");
    }

    public static Data newData3(Integer ownerId, Integer providerId, Integer datasetId) {
        return newData(ownerId, providerId, datasetId, "test data 3", "", "VECTOR", true, true, false, false, "line");
    }

    public static Data newDataQuote(Integer ownerId, Integer providerId, Integer datasetId) {
        return newData(ownerId, providerId, datasetId, "bla bloiu '; quote", "bla '; select * from admin.datas;", "'VECTOR'", false, true, false, false, "'; drop table admin.datas;'");
    }

    public static Data newData5(Integer ownerId, Integer providerId, Integer datasetId) {
        return newData(ownerId, providerId, datasetId, "test data 5 ", "", "COVERAGE", true, false, false, true, null);
    }

    public static Data newData(Integer ownerId, Integer providerId, Integer datasetId, String name, String namespace, 
            String type, boolean hidden, boolean included, boolean sensorable, boolean rendered, String subType) {
        Data data = new Data();
        data.setDate(new Date());
        data.setName(name);
        data.setNamespace(namespace);
        data.setOwnerId(ownerId);
        data.setType(type);
        data.setSubtype(subType);
        data.setProviderId(providerId);
        data.setHidden(hidden);
        data.setDatasetId(datasetId);

        // by default value setted
        data.setIncluded(included);
        data.setSensorable(sensorable);

        data.setRendered(rendered);
        return data;
    }

    public static Service newService(Integer ownerId) {
        Service service = new Service();
        service.setDate(new Date());
        service.setConfig(null);
        service.setIdentifier("default");
        service.setOwner(ownerId);
        service.setType("wms");
        service.setStatus("running");
        service.setVersions("1.0.0");
        service.setImpl("impl1");
        return service;
    }

    public static Service newService2(Integer ownerId) {
        Service service = new Service();
        service.setDate(new Date());
        service.setConfig(null);
        service.setIdentifier("test");
        service.setOwner(ownerId);
        service.setType("wms");
        service.setStatus("running");
        service.setVersions("1.0.0");
        service.setImpl("impl1");
        return service;
    }

    public static Service newServiceQuote(Integer ownerId) {
        Service service = new Service();
        service.setDate(new Date());
        service.setConfig("' drop table admin.service;'");
        service.setIdentifier("te'; delete * from st");
        service.setOwner(ownerId);
        service.setType("wms");
        service.setStatus("run''ning");
        service.setVersions("1.0.'0");
        service.setImpl("impl''3");
        return service;
    }

    public static ProviderBrief newProvider(Integer ownerId) {
        ProviderBrief provider = new ProviderBrief();
        provider.setIdentifier("provider-test");
        provider.setImpl("immmmp");
        provider.setOwner(ownerId);
        provider.setType("coverage");
        provider.setParent("");
        provider.setConfig("<root />");
        return provider;
    }

    public static ProviderBrief newProvider2(Integer ownerId) {
        ProviderBrief provider = new ProviderBrief();
        provider.setIdentifier("provider-test2");
        provider.setImpl("immmmp");
        provider.setOwner(ownerId);
        provider.setType("VECTOR");
        provider.setParent("");
        provider.setConfig("<root />");
        return provider;
    }

    public static ProviderBrief newProviderQuote(Integer ownerId) {
        ProviderBrief provider = new ProviderBrief();
        provider.setIdentifier("provider-'; drop table admin.provider; 'test3");
        provider.setImpl("i'mmmmp");
        provider.setOwner(ownerId);
        provider.setType("'VECTOR'");
        provider.setParent("");
        provider.setConfig("<ro';'ot />");
        return provider;
    }

    public static Layer newLayer(Integer ownerId, Integer dataId, Integer serviceId) {
        Layer layer = new Layer();
        layer.setAlias("layerAlias");
        layer.setDataId(dataId);
        layer.setDate(new Date());
        layer.setConfig(null);
        layer.setName("testlayer");
        layer.setNamespace("");
        layer.setOwnerId(ownerId);
        layer.setService(serviceId);
        layer.setTitle("layer tiltle");
        return layer;
    }

    public static Layer newLayer2(Integer ownerId, Integer dataId, Integer serviceId) {
        Layer layer = new Layer();
        layer.setAlias("layer'; delete from admin.layer;'Alias");
        layer.setDataId(dataId);
        layer.setDate(new Date());
        layer.setConfig(null);
        layer.setName("test'l'ayer");
        layer.setNamespace("test' nmsp");
        layer.setOwnerId(ownerId);
        layer.setService(serviceId);
        layer.setTitle("layer' 'tiltle");
        return layer;
    }

    public static Layer newLayer3(Integer ownerId, Integer dataId, Integer serviceId) {
        Layer layer = new Layer();
        layer.setAlias(null);
        layer.setDataId(dataId);
        layer.setDate(new Date());
        layer.setConfig(null);
        layer.setName("testlayer3");
        layer.setNamespace("");
        layer.setOwnerId(ownerId);
        layer.setService(serviceId);
        layer.setTitle("layer tiltle");
        return layer;
    }

    public static Layer newLayer4(Integer ownerId, Integer dataId, Integer serviceId) {
        Layer layer = new Layer();
        layer.setAlias(null);
        layer.setDataId(dataId);
        layer.setDate(new Date());
        layer.setConfig(null);
        layer.setName("test'l'ayer4;");
        layer.setNamespace("test' nmsp");
        layer.setOwnerId(ownerId);
        layer.setService(serviceId);
        layer.setTitle("layer' 'tiltle");
        return layer;
    }

    public static Task newTask(Integer ownerId, String uuid, int tpId) {
        Task task = new Task();
        task.setDateEnd(System.currentTimeMillis() + 10000);
        task.setDateStart(System.currentTimeMillis());
        task.setIdentifier(uuid);
        task.setMessage("some msg");
        task.setProgress(50d);
        task.setState("RUNNING");
        task.setType("internal");
        task.setTaskParameterId(tpId);
        return task;
    }

    public static Task newTaskQuote(Integer ownerId, String uuid, int tpId) {
        Task task = new Task();
        task.setDateEnd(null);
        task.setDateStart(System.currentTimeMillis());
        task.setIdentifier(uuid);
        task.setMessage("some'delete * from adins.task;'msg");
        task.setProgress(50d);
        task.setState("RUNN''ING");
        task.setType("int''ernal");
        task.setTaskParameterId(tpId);
        return task;
    }

    public static MetadataComplete newMetadata(Integer ownerId, String identifier, Integer dataId, Integer datasetId, Integer serviceId) {
        MetadataComplete metadata = new MetadataComplete();
        List<MetadataBbox> bboxes = new ArrayList<>();
        metadata.setBboxes(bboxes);
        metadata.setComment("com");
        metadata.setDateCreation(System.currentTimeMillis());
        metadata.setDatestamp(System.currentTimeMillis());
        metadata.setIsHidden(false);
        metadata.setIsPublished(true);
        metadata.setIsShared(false);
        metadata.setIsValidated(true);
        metadata.setLevel("COMPLETE");
        metadata.setMdCompletion(53);
        metadata.setMetadataId(identifier);
        metadata.setOwner(ownerId);
        metadata.setProfile("profile_import");
        metadata.setResume("some resumte");
        metadata.setTitle("ttle");
        metadata.setType("DOC");

        // association
        metadata.setDataId(dataId);
        metadata.setDatasetId(datasetId);
        metadata.setServiceId(serviceId);
        return metadata;
    }

    public static MetadataComplete newMetadataQuote(Integer ownerId, String identifier) {
        MetadataComplete metadata = new MetadataComplete();
        List<MetadataBbox> bboxes = new ArrayList<>();
        metadata.setBboxes(bboxes);
        metadata.setComment("';'");
        metadata.setDateCreation(System.currentTimeMillis());
        metadata.setDatestamp(System.currentTimeMillis());
        metadata.setIsHidden(false);
        metadata.setIsPublished(true);
        metadata.setIsShared(false);
        metadata.setIsValidated(true);
        metadata.setLevel("COMPLETE");
        metadata.setMdCompletion(76);
        metadata.setMetadataId(identifier);
        metadata.setOwner(ownerId);
        metadata.setProfile("profile'; delete * from * 'import");
        metadata.setResume("'; DROP table admin.metadata;'");
        metadata.setTitle("tt'le");
        metadata.setType("DOC");
        return metadata;
    }

    public static DataSource newDataSource() {
        DataSource datasource = new DataSource();
        datasource.setAnalysisState("PENDING");
        datasource.setDateCreation(System.currentTimeMillis());
        datasource.setFormat("image/tiff");
        datasource.setPermanent(Boolean.TRUE);
        datasource.setReadFromRemote(Boolean.FALSE);
        datasource.setStoreId("worldfile");
        datasource.setType("file");
        datasource.setUrl("file:///home/test");
        datasource.setUsername("michel");
        datasource.setPwd("m1ch3l");
        return datasource;
    }

    public static DataSource newDataSourceQuote() {
        DataSource datasource = new DataSource();
        datasource.setAnalysisState("PENDING");
        datasource.setDateCreation(System.currentTimeMillis());
        datasource.setFormat("image'; DROP TABLE admin.datasource;'/tiff");
        datasource.setPermanent(Boolean.TRUE);
        datasource.setReadFromRemote(Boolean.FALSE);
        datasource.setStoreId("tes't");
        datasource.setType("file");
        datasource.setUrl("file:///home/tes't");
        datasource.setUsername("tes't");
        datasource.setPwd("tes't");
        return datasource;
    }

    public static DataSet newDataSet(Integer ownerId, String identifier) {
        DataSet dataset = new DataSet();
        dataset.setIdentifier(identifier);
        dataset.setDate(System.currentTimeMillis());
        dataset.setOwnerId(ownerId);
        dataset.setType("test");
        return dataset;
    }

    public static Sensor newSensor(Integer ownerId, String identifier) {
        Sensor sensor = new Sensor();
        sensor.setIdentifier(identifier);
        sensor.setDate(new Date(System.currentTimeMillis()));
        sensor.setOwner(ownerId);
        sensor.setType("System");
        sensor.setProfile("profile_import");
        sensor.setOmType("timeseries");
        return sensor;
    }

    public static Sensor newSensorQuote(Integer ownerId, String identifier) {
        Sensor sensor = new Sensor();
        sensor.setIdentifier(identifier);
        sensor.setDate(new Date(System.currentTimeMillis()));
        sensor.setOwner(ownerId);
        sensor.setType("Syst'; delete all;'em");
        sensor.setProfile("prof''ile_import");
        sensor.setOmType("tim'eser'ies");
        return sensor;
    }

    public static Style newStyle(Integer ownerId, String name) {
        Style style = new Style();
        style.setBody("somrbody");;
        style.setDate(new Date(System.currentTimeMillis()));
        style.setOwnerId(ownerId);
        style.setType("VECTOR");
        style.setIsShared(Boolean.TRUE);
        style.setProviderId(1);
        style.setName(name);
        return style;
    }

    public static Style newStyleQuote(Integer ownerId, String name) {
        Style style = new Style();
        style.setBody("somr'; drop table admin.style;'body");;
        style.setDate(new Date(System.currentTimeMillis()));
        style.setOwnerId(ownerId);
        style.setType("VE''CTOR");
        style.setIsShared(Boolean.FALSE);
        style.setProviderId(1);
        style.setName(name);
        return style;
    }

    public static Attachment newAttachment() {
        Attachment att = new Attachment();
        att.setUri("/usr/local");
        att.setFilename("fnmae");
        att.setContent(new byte[10]);
        return att;
    }

    public static Attachment newAttachmentQuote() {
        Attachment att = new Attachment();
        att.setUri("/usr/l'ocal");
        att.setFilename("fn'mae");
        att.setContent(new byte[10]);
        return att;
    }


    public static MapContextDTO newMapcontext(CstlUser owner, String name, String desc) {
        MapContextDTO mp = new MapContextDTO();
        mp.setCrs("CRS:84");
        mp.setDescription(desc);
        mp.setEast(1.1);
        mp.setKeywords("bla");
        mp.setName(name);
        mp.setNorth(1.1);
        mp.setOwner(owner.getId());
        mp.setSouth(1.1);
        mp.setUserOwner(owner.getLogin());
        mp.setWest(1.1);
        return mp;
    }

    public static MapContextDTO newMapcontext2(CstlUser owner, String name) {
        MapContextDTO mp = new MapContextDTO();
        mp.setCrs("CRS''84");
        mp.setDescription("desc'; delete * from mp;'");
        mp.setEast(1.1);
        mp.setKeywords("bl';'a");
        mp.setName(name);
        mp.setNorth(1.1);
        mp.setOwner(owner.getId());
        mp.setSouth(1.1);
        mp.setUserOwner(owner.getLogin());
        mp.setWest(1.1);
        return mp;
    }

    public static Thesaurus newThesaurus() {
        Thesaurus th = new Thesaurus();
        th.setUri("urn:th");
        th.setCreationDate(new  Date(System.currentTimeMillis()));
        th.setDefaultLang("en");
        th.setDescription("desc");
        th.setLangs(Arrays.asList("en", "fr"));
        th.setName("thesau 1");
        th.setSchemaName("tho");
        th.setState(true);
        th.setVersion("1.0.0");
        return th;
    }

    public static Thesaurus newThesaurusQuote() {
        Thesaurus th = new Thesaurus();
        th.setUri("ur''n:th");
        th.setCreationDate(new  Date(System.currentTimeMillis()));
        th.setDefaultLang("e'n");
        th.setDescription("de''sc");
        th.setLangs(Arrays.asList("en", "fr"));
        th.setName("thesau'; drop table admin.thesaurus; '1");
        th.setSchemaName("t'ho");
        th.setState(true);
        th.setVersion("1.0'.0");
        return th;
    }

    public static TaskParameter newTaskParameter(Integer ownerId, String auth, String code) {
        TaskParameter tp = new TaskParameter();
        tp.setDate(System.currentTimeMillis());
        tp.setInputs("");
        tp.setName("dd");
        tp.setOwner(ownerId);
        tp.setProcessAuthority(auth);
        tp.setProcessCode(code);
        tp.setTrigger(null);
        tp.setTriggerType(null);
        tp.setType("");
        return tp;
    }

    public static TaskParameter newTaskParameterQuote(Integer ownerId, String auth, String code) {
        TaskParameter tp = new TaskParameter();
        tp.setDate(System.currentTimeMillis());
        tp.setInputs("ss'; delete * from admin.taskParameter;'");
        tp.setName("na'med");
        tp.setOwner(ownerId);
        tp.setProcessAuthority(auth);
        tp.setProcessCode(code);
        tp.setTrigger("trig'ger");
        tp.setTriggerType("trig'ger'type");
        tp.setType("t'ype");
        return tp;
    }

    public static List<String> adminRoles() {
        return Collections.singletonList("cstl-admin");
    }

    public static ChainProcess newChainProcess() {
        ChainProcess cp = new ChainProcess();
        cp.setAuth("test");
        cp.setCode("001");
        cp.setConfig("<xml></xml>");
        return cp;
    }

    public static ChainProcess newChainProcessQuote() {
        ChainProcess cp = new ChainProcess();
        cp.setAuth("tes't");
        cp.setCode("'001'");
        cp.setConfig("<xml>'; delete * FROM admin.chain_process;</xml>");
        return cp;
    }

}
