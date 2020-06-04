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

    public static Data newData1(Integer ownerId, Integer providerId, Integer datasetId) {
        Data data = new Data();
        data.setDate(new Date());
        data.setName("test data 1");
        data.setNamespace("");
        data.setOwnerId(ownerId);
        data.setType("raster");
        data.setProviderId(providerId);
        data.setHidden(false);
        data.setDatasetId(datasetId);

        // by default value setted
        data.setIncluded(true);
        data.setSensorable(false);
        return data;
    }

    public static Data newData2(Integer ownerId, Integer providerId, Integer datasetId) {
        Data data = new Data();
        data.setDate(new Date());
        data.setName("test data 2");
        data.setNamespace("");
        data.setOwnerId(ownerId);
        data.setType("vector");
        data.setProviderId(providerId);
        data.setHidden(false);
        data.setDatasetId(datasetId);

        // by default value setted
        data.setIncluded(true);
        data.setSensorable(false);
        return data;
    }

    public static Data newData3(Integer ownerId, Integer providerId, Integer datasetId) {
        Data data = new Data();
        data.setDate(new Date());
        data.setName("test data 3");
        data.setNamespace("");
        data.setOwnerId(ownerId);
        data.setType("vector");
        data.setProviderId(providerId);
        data.setHidden(true);
        data.setDatasetId(datasetId);

        // by default value setted
        data.setIncluded(true);
        data.setSensorable(false);
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

    public static Task newTask(Integer ownerId, String uuid) {
        Task task = new Task();
        task.setDateEnd(System.currentTimeMillis() + 10000);
        task.setDateStart(System.currentTimeMillis());
        task.setIdentifier(uuid);
        task.setMessage("some msg");
        task.setProgress(50d);
        task.setState("RUNNING");
        task.setType("internal");
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

    public static Style newStyle(Integer ownerId, String name) {
        Style style = new Style();
        style.setBody("somrbody");;
        style.setDate(new Date(System.currentTimeMillis()));
        style.setOwnerId(ownerId);
        style.setType("vector");
        style.setIsShared(Boolean.TRUE);
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

    public static MapContextDTO newMapcontext(CstlUser owner, String name) {
        MapContextDTO mp = new MapContextDTO();
        mp.setCrs("CRS:84");
        mp.setDescription("desc");
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

    public static TaskParameter newTaskParameter(Integer ownerId, String auth, String code) {
        TaskParameter tp = new TaskParameter();
        tp.setDate(System.currentTimeMillis());
        tp.setInputs("");
        tp.setName("dd");
        tp.setOwner(ownerId);
        tp.setProcessAuthority(auth);
        tp.setProcessCode(code);
        tp.setTrigger("");
        tp.setTriggerType("");
        tp.setType("");
        return tp;
    }

    public static List<String> adminRoles() {
        return Collections.singletonList("cstl-admin");
    }

}
