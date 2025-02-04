
package com.examind.store.observation.csv;

import static com.examind.store.observation.csv.AbstractCsvStoreTest.format;
import static com.examind.store.observation.csv.AbstractCsvStoreTest.writeResourceFileInDir;
import com.examind.store.observation.csvsplitted.CsvSplittedObservationStore;
import com.examind.store.observation.csvsplitted.CsvSplittedObservationStoreFactory;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.geotoolkit.data.csv.CSVProvider;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.observation.model.ObservationDataset;
import org.geotoolkit.observation.model.Phenomenon;
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
 * @author glegal
 */
public class CsvSplittedObservationStoreTest extends AbstractCsvStoreTest {
    
    private static Path splittedTsFile;
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractCsvStoreTest.setUpClass();
        
        writeResourceFileInDir("splitted-ts", "splitted/ts/sensors.csv", "sensors.csv");
        writeResourceFileInDir("splitted-ts", "splitted/ts/variables.csv", "variables.csv");
        splittedTsFile = writeResourceFileInDir("splitted-ts", "splitted/ts/observations.csv", "observations.csv");
    }
    
    @Test
    public void csvSpittedStoreTSTest() throws Exception {
        CsvSplittedObservationStoreFactory factory = new CsvSplittedObservationStoreFactory();
        ParameterValueGroup params = factory.getOpenParameters().createValue();
        params.parameter(CsvSplittedObservationStoreFactory.LOCATION).setValue(splittedTsFile.toUri().toString());

        params.parameter(CsvSplittedObservationStoreFactory.DATE_COLUMN.getName().getCode()).setValue("observation_time");
        params.parameter(CsvSplittedObservationStoreFactory.MAIN_COLUMN.getName().getCode()).setValue("observation_time");

        params.parameter(CsvSplittedObservationStoreFactory.DATE_FORMAT.getName().getCode()).setValue("yyyy-MM-dd'T'hh:mm:ss'Z'");

        params.parameter(CsvSplittedObservationStoreFactory.LATITUDE_COLUMN.getName().getCode()).setValue("station_latitude");
        params.parameter(CsvSplittedObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode()).setValue("station_longitude");

        params.parameter(CsvSplittedObservationStoreFactory.FILE_MIME_TYPE.getName().getCode()).setValue("csv");

        params.parameter(CsvSplittedObservationStoreFactory.RESULT_COLUMN.getName().getCode()).setValue("result_value");

        params.parameter(CsvSplittedObservationStoreFactory.OBS_PROP_COLUMN.getName().getCode()).setValue("obsprop_id");
        params.parameter(CsvSplittedObservationStoreFactory.OBS_PROP_NAME_COLUMN.getName().getCode()).setValue("obsprop_name");
        params.parameter(CsvSplittedObservationStoreFactory.OBS_PROP_DESC_COLUMN.getName().getCode()).setValue("obsprop_desc");

        params.parameter(CsvSplittedObservationStoreFactory.OBSERVATION_TYPE.getName().getCode()).setValue("Timeserie");
        
        params.parameter(CsvSplittedObservationStoreFactory.PROCEDURE_COLUMN.getName().getCode()).setValue("station_id");
        params.parameter(CsvSplittedObservationStoreFactory.PROCEDURE_NAME_COLUMN.getName().getCode()).setValue("station_name");
        params.parameter(CsvSplittedObservationStoreFactory.PROCEDURE_DESC_COLUMN.getName().getCode()).setValue("station_description");
        params.parameter(CsvSplittedObservationStoreFactory.PROCEDURE_PROPERTIES_COLUMN.getName().getCode()).setValue("network_code");
        params.parameter(CsvSplittedObservationStoreFactory.PROCEDURE_PROPERTIES_MAP_COLUMN.getName().getCode()).setValue("station_metadata");
        
        params.parameter(CsvSplittedObservationStoreFactory.UOM_COLUMN.getName().getCode()).setValue("unit");


        params.parameter(CSVProvider.SEPARATOR.getName().getCode()).setValue(Character.valueOf(','));

        CsvSplittedObservationStore store = factory.open(params);

        Set<String> procedureNames = store.getEntityNames(new ProcedureQuery());
        Assert.assertEquals(1, procedureNames.size());
        
        String sensorId = "EXIN0006";
        Assert.assertTrue(procedureNames.contains(sensorId));

        Set<String> phenomenonNames = store.getEntityNames(new ObservedPropertyQuery());
        Assert.assertTrue(phenomenonNames.contains("TEMP"));
        Assert.assertTrue(phenomenonNames.contains("DRYT"));
        
        List<Phenomenon> phenomenons = store.getPhenomenons(new ObservedPropertyQuery(true));
        Assert.assertEquals(1, phenomenons.size());
        Assert.assertTrue(phenomenons.get(0) instanceof CompositePhenomenon);
        CompositePhenomenon composite = (CompositePhenomenon) phenomenons.get(0);
        Assert.assertEquals(2, composite.getComponent().size());
        Assert.assertEquals("DRYT", composite.getComponent().get(0).getId());
        Assert.assertEquals("DRY BULB TEMPERATURE", composite.getComponent().get(0).getName());
        Assert.assertEquals("The degree of hotness of the atmosphere.", composite.getComponent().get(0).getDescription());

        Assert.assertEquals("TEMP", composite.getComponent().get(1).getId());
        Assert.assertEquals("SEA TEMPERATURE", composite.getComponent().get(1).getName());
        Assert.assertEquals("The degree of hotness of the water column expressed against a standard scale. Includes both IPTS-68 and ITS-90 scales.", composite.getComponent().get(1).getDescription());

        IdentifierQuery timeQuery = new IdentifierQuery(OMEntity.PROCEDURE, sensorId);
        TemporalPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        Period tp = (Period) time;
        Assert.assertEquals("2021-09-01T00:00:36" , format(tp.getBeginning()));
        Assert.assertEquals("2022-03-22T00:06:26" , format(tp.getEnding()));

        ObservationDataset results = store.getDataset(new DatasetQuery());
        Assert.assertEquals(1, results.procedures.size());
        ProcedureDataset proc = results.procedures.get(0);
        Assert.assertEquals(sensorId, proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());

        Assert.assertEquals(1, procedures.size());
        proc = procedures.get(0);
        Assert.assertEquals(sensorId, proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("2021-09-01T00:00:36" , format(tp.getBeginning()));
        Assert.assertEquals("2022-03-22T00:06:26" , format(tp.getEnding()));
    }
}
