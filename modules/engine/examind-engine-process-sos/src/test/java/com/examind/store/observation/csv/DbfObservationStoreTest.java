/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2023 Geomatys.
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
package com.examind.store.observation.csv;

import com.examind.store.observation.dbf.DbfObservationStore;
import com.examind.store.observation.dbf.DbfObservationStoreFactory;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.observation.model.ObservationDataset;
import org.geotoolkit.observation.model.ProcedureDataset;
import org.geotoolkit.observation.query.DatasetQuery;
import org.geotoolkit.observation.query.IdentifierQuery;
import org.geotoolkit.observation.query.ObservedPropertyQuery;
import org.geotoolkit.observation.query.ProcedureQuery;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalPrimitive;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DbfObservationStoreTest extends AbstractCsvStoreTest {

    protected static Path ltFile;
    protected static Path rtFile;

    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractCsvStoreTest.setUpClass();
        
        ltFile       = writeResourceFileInDir("lt-ts", "LakeTile_001.dbf");
        rtFile       = writeResourceFileInDir("rt-ts", "rivertile_001.dbf");
    }

    @Test
    public void dbfStoreTSTest() throws Exception {

        String sensorId = "urn:sensor:dbf:1";

        DbfObservationStoreFactory factory = new DbfObservationStoreFactory();
        ParameterValueGroup params = factory.getOpenParameters().createValue();
        params.parameter(DbfObservationStoreFactory.LOCATION).setValue(ltFile.toUri().toString());

        params.parameter(DbfObservationStoreFactory.DATE_COLUMN.getName().getCode()).setValue("TIME_STR");
        params.parameter(DbfObservationStoreFactory.MAIN_COLUMN.getName().getCode()).setValue("TIME_STR");

        params.parameter(DbfObservationStoreFactory.DATE_FORMAT.getName().getCode()).setValue("yyyy-MM-dd' 'HH:mm:ss");

        params.parameter(DbfObservationStoreFactory.FOI_COLUMN.getName().getCode()).setValue("PRIOR_ID");

        params.parameter(DbfObservationStoreFactory.OBS_PROP_COLUMN.getName().getCode()).setValue("HEIGHT");

        params.parameter(DbfObservationStoreFactory.OBSERVATION_TYPE.getName().getCode()).setValue("Timeserie");
        params.parameter(DbfObservationStoreFactory.PROCEDURE_ID.getName().getCode()).setValue(sensorId);
        params.parameter(DbfObservationStoreFactory.FILE_MIME_TYPE.getName().getCode()).setValue("application/dbase");

        DbfObservationStore store = factory.open(params);

        Set<String> procedureNames = store.getEntityNames(new ProcedureQuery());
        Assert.assertEquals(1, procedureNames.size());

        String result = procedureNames.iterator().next();
        Assert.assertEquals(sensorId, result);

        Set<String> phenomenonNames = store.getEntityNames(new ObservedPropertyQuery());
        Assert.assertTrue(phenomenonNames.contains("HEIGHT"));

        IdentifierQuery timeQuery = new IdentifierQuery(OMEntity.PROCEDURE, sensorId);
        TemporalPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        Period tp = (Period) time;
        Assert.assertEquals("2022-08-20T01:55:11" , format(tp.getBeginning()));
        Assert.assertEquals("2022-08-20T01:55:13" , format(tp.getEnding()));

        ObservationDataset results = store.getDataset(new DatasetQuery());
        Assert.assertEquals(1, results.procedures.size());
        Assert.assertEquals(0, results.procedures.get(0).spatialBound.getHistoricalLocations().size());

        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());
        Assert.assertEquals(1, procedures.size());

        ProcedureDataset pt = procedures.get(0);
        Assert.assertEquals(0, pt.spatialBound.getHistoricalLocations().size());

        time = pt.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("2022-08-20T01:55:11" , format(tp.getBeginning()));
        Assert.assertEquals("2022-08-20T01:55:13" , format(tp.getEnding()));

        // TODO verify foi
    }

    @Test
    public void dbfStoreTS2Test() throws Exception {

        String sensorId = "urn:sensor:dbf:2";

        DbfObservationStoreFactory factory = new DbfObservationStoreFactory();
        ParameterValueGroup params = factory.getOpenParameters().createValue();
        params.parameter(DbfObservationStoreFactory.LOCATION).setValue(rtFile.toUri().toString());

        params.parameter(DbfObservationStoreFactory.DATE_COLUMN.getName().getCode()).setValue("time");
        params.parameter(DbfObservationStoreFactory.MAIN_COLUMN.getName().getCode()).setValue("time");

        params.parameter(DbfObservationStoreFactory.LATITUDE_COLUMN.getName().getCode()).setValue("latitude");
        params.parameter(DbfObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode()).setValue("longitude");

        params.parameter(DbfObservationStoreFactory.FOI_COLUMN.getName().getCode()).setValue("node_id");

        params.parameter(DbfObservationStoreFactory.OBS_PROP_COLUMN.getName().getCode()).setValue("height,width");

        params.parameter(DbfObservationStoreFactory.OBSERVATION_TYPE.getName().getCode()).setValue("Timeserie");
        params.parameter(DbfObservationStoreFactory.PROCEDURE_ID.getName().getCode()).setValue(sensorId);
        params.parameter(DbfObservationStoreFactory.FILE_MIME_TYPE.getName().getCode()).setValue("application/dbase");

        DbfObservationStore store = factory.open(params);

        Set<String> procedureNames = store.getEntityNames(new ProcedureQuery());
        Assert.assertEquals(1, procedureNames.size());

        String result = procedureNames.iterator().next();
        Assert.assertEquals(sensorId, result);

        Set<String> phenomenonNames = store.getEntityNames(new ObservedPropertyQuery());
        Assert.assertTrue(phenomenonNames.contains("height"));
        Assert.assertTrue(phenomenonNames.contains("width"));

        IdentifierQuery timeQuery = new IdentifierQuery(OMEntity.PROCEDURE, sensorId);
        TemporalPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

        Period tp = (Period) time;
        Assert.assertEquals("2022-06-06T00:58:50.921" , format(sdf, tp.getBeginning()));
        Assert.assertEquals("2022-06-06T00:58:51.320" , format(sdf, tp.getEnding()));

        ObservationDataset results = store.getDataset(new DatasetQuery());
        Assert.assertEquals(1, results.procedures.size());
        Assert.assertEquals(21, results.procedures.get(0).spatialBound.getHistoricalLocations().size());

        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());
        Assert.assertEquals(1, procedures.size());

        ProcedureDataset pt = procedures.get(0);
        Assert.assertEquals(21, pt.spatialBound.getHistoricalLocations().size());

        time = pt.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("2022-06-06T00:58:50.921" , format(sdf, tp.getBeginning()));
        Assert.assertEquals("2022-06-06T00:58:51.320" , format(sdf, tp.getEnding()));

        // TODO verify foi
    }
}
