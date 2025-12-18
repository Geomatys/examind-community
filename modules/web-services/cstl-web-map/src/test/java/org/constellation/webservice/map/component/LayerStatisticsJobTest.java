package org.constellation.webservice.map.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.constellation.admin.SpringHelper;
import org.constellation.business.*;
import org.constellation.dto.contact.Details;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.map.layerstats.LayerStatistics;
import org.constellation.repository.StyleRepository;
import org.constellation.repository.StyledLayerRepository;
import org.constellation.test.SpringContextTest;
import org.constellation.test.utils.TestEnvironment;
import org.geotoolkit.nio.IOUtilities;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.style.Style;

import static org.constellation.map.layerstats.LayerStatisticsUtils.getMapper;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import static org.constellation.test.utils.TestResourceUtils.writeResourceDataFile;

/**
 * Test the statistics for styles and layers.
 *
 * @author Estelle Idée (Geomatys)
 */
public class LayerStatisticsJobTest extends SpringContextTest {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.webservice.map.component");
    @Autowired
    private IServiceBusiness serviceBusiness;
    @Autowired
    private IDatasetBusiness datasetBusiness;
    @Autowired
    private ILayerBusiness layerBusiness;
    @Autowired
    private IDatasourceBusiness datasourceBusiness;
    @Autowired
    private IProviderBusiness providerBusiness;
    @Autowired
    private IStyleBusiness styleBusiness;
    @Autowired
    private StyleRepository styleRepository;
    @Autowired
    private ILayerStatisticsJob layerStatisticsJob;

    private static boolean initialized = false;


    private static Path DATA_DIRECTORY;

    private static Integer layerId;
    private static Integer styleId;

    private static Style style;
    private static final String styleName = "Med_KBA_Tunisia-sld";

    @Autowired
    private StyledLayerRepository styledLayerRepository;


    @BeforeClass
    public static void setUpClass() throws Exception {

        final Path configDir = Paths.get("target");
        DATA_DIRECTORY = configDir.resolve("data" + UUID.randomUUID());
        Files.createDirectories(DATA_DIRECTORY);

        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/statistics/Med_KBA_Tunisia-sld.xml", "Med_KBA_Tunisia-sld.xml");
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/statistics/Med_KBA_Tunisia_modified-sld.xml", "Med_KBA_Tunisia_modified-sld.xml");
    }

    @PostConstruct
    public void setUp() throws Exception {
        if (!initialized) {
            // clean up
            serviceBusiness.deleteAll();
            datasetBusiness.removeAllDatasets();
            styleBusiness.deleteAll();
            providerBusiness.removeAll();
            datasourceBusiness.deleteAll();

            // Add style to database
            final Path stylePath = DATA_DIRECTORY.resolve("Med_KBA_Tunisia-sld.xml");
            final Path styleFileName = stylePath.getFileName();
            final String styleName = IOUtilities.filenameWithoutExtension(styleFileName);
            style = styleBusiness.parseStyle(styleName, stylePath, styleFileName.toString());
            styleId = styleBusiness.createStyle("sld", style);

            // Create dataset
            final int userId = 1;

            final TestEnvironment.TestResources testResource = initDataDirectory();
            final TestEnvironment.DataImport data = testResource.createProviders(TestEnvironment.TestResource.STATISTICS_SHAPEFILES, providerBusiness, null).datas().get(0);
            final int dataId = data.id;

            // Link data to style
            styleBusiness.linkToData(styleId, dataId);

            // Create WMS service and add layer
            Details metadata = new Details();
            metadata.setIdentifier("service");
            metadata.setName("service");
            metadata.setVersions(List.of("1.3.0"));
            metadata.setLang("en");
            final int serviceId = serviceBusiness.create("wms", "service", null, metadata, userId);

            layerId = layerBusiness.add(dataId, "Med_KBA_Tunisia", null, "Med_KBA_Tunisia", null, serviceId, null);

            // Check that the style and the layer have been linked together.
            final List<org.constellation.dto.Style> layerStylesIds = styleRepository.findByLayer(layerId);
            Assert.assertEquals(layerStylesIds.size(), 1);
            Assert.assertEquals(layerStylesIds.get(0).getId(), styleId);

            styledLayerRepository.updateActivateStats(styleId, layerId, true);

            initialized = true;
        } else {
            styleBusiness.updateStyle(styleId, null, style);
            styleBusiness.updateActivateStatsForLayerAndStyle(styleId, layerId, true);
        }
        layerStatisticsJob.syncUpdateStyledLayerStatistics(styleId, layerId);
    }

    @AfterClass
    public static void tearDownClass() {
        initialized = false;
        try {
            IServiceBusiness sb = SpringHelper.getBean(IServiceBusiness.class).orElse(null);
            if (sb != null) {
                sb.deleteAll();
            }
            IDatasetBusiness dsetb = SpringHelper.getBean(IDatasetBusiness.class).orElse(null);
            if (dsetb != null) {
                dsetb.removeAllDatasets();
            }
            IStyleBusiness stb = SpringHelper.getBean(IStyleBusiness.class).orElse(null);
            if (stb != null) {
                stb.deleteAll();
            }
            IProviderBusiness pb = SpringHelper.getBean(IProviderBusiness.class).orElse(null);
            if (pb != null) {
                pb.removeAll();
            }
            IDatasourceBusiness dsb = SpringHelper.getBean(IDatasourceBusiness.class).orElse(null);
            if (dsb != null) {
                dsb.deleteAll();
            }
            File derbyLog = new File("derby.log");
            if (derbyLog.exists()) {
                derbyLog.delete();
            }
            File mappingFile = new File("mapping.properties");
            if (mappingFile.exists()) {
                mappingFile.delete();
            }
            IOUtilities.deleteSilently(DATA_DIRECTORY);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    @Test
    public void statisticsCreatedLayerFromDataTest() throws ConstellationException, JsonProcessingException {
        final String extraInfo = styleBusiness.getExtraInfoForStyleAndLayer(styleId, layerId);
        final ObjectMapper mapper = getMapper();
        final LayerStatistics statistics = mapper.readValue(extraInfo, LayerStatistics.class);

        Assert.assertTrue(statistics.getTotalCount() == 232);
        Assert.assertEquals(statistics.getSeries().size(), 4);
        Assert.assertEquals(statistics.getTypes(), Arrays.asList(LayerStatistics.StatisticsType.POLYGON, LayerStatistics.StatisticsType.POLYGON, LayerStatistics.StatisticsType.SIMPLE, LayerStatistics.StatisticsType.LINE));
        Assert.assertEquals(statistics.getSeries(), Arrays.asList("4", "9", "6", "10"));
        Assert.assertEquals(statistics.getCounts(), Arrays.asList(4L, 2L, 4L, 3L));
        Assert.assertEquals(statistics.getColors(), Arrays.asList("#f50707", "#44e92c", "#eb62f1", "#f23535"));
        Assert.assertEquals(statistics.getSizes(), Arrays.asList(null, null, "15", null));
        Assert.assertEquals(statistics.getStrokeColors(), Arrays.asList(null, null, "#4a44e7", "#f23535"));
        Assert.assertEquals(statistics.getStrokeWidths(), Arrays.asList(null, null, "1.0", "1"));

    }

    @Test
    public void statisticsStyleModifiedTest() throws ConstellationException, JsonProcessingException {
        // Add style to database
        final Path stylePath = DATA_DIRECTORY.resolve("Med_KBA_Tunisia_modified-sld.xml");
        Style style = styleBusiness.parseStyle(styleName, stylePath, styleName + ".xml");
        styleBusiness.updateStyle(styleId, null, style);

        layerStatisticsJob.syncUpdateStyledLayerStatistics(styleId, layerId);
        final String extraInfo = styleBusiness.getExtraInfoForStyleAndLayer(styleId, layerId);
        final ObjectMapper mapper = getMapper();
        final LayerStatistics statistics = mapper.readValue(extraInfo, LayerStatistics.class);

        Assert.assertTrue(statistics.getTotalCount() == 232);
        Assert.assertEquals(statistics.getSeries().size(), 4);
        Assert.assertEquals(statistics.getTypes(), Arrays.asList(LayerStatistics.StatisticsType.POLYGON, LayerStatistics.StatisticsType.SIMPLE, LayerStatistics.StatisticsType.LINE, LayerStatistics.StatisticsType.POLYGON));
        Assert.assertEquals(statistics.getSeries(), Arrays.asList("9", "6", "PWA = 10", "25"));
        Assert.assertEquals(statistics.getCounts(), Arrays.asList(2L, 4L, 3L, 3L));
        Assert.assertEquals(statistics.getColors(), Arrays.asList("#44e92c", "#eb62f1", "#f23535", "#e61919"));
        Assert.assertEquals(statistics.getSizes(), Arrays.asList(null, "15", null, null));
        Assert.assertEquals(statistics.getStrokeColors(), Arrays.asList(null, "#4a44e7", "#f23535", null));
        Assert.assertEquals(statistics.getStrokeWidths(), Arrays.asList(null, "1.0", "1", null));

    }

    @Test
    public void statisticsLinkStyleToExistingLayerTest() throws ConstellationException, JsonProcessingException {
        // Add style to database
        final Path stylePath = DATA_DIRECTORY.resolve("Med_KBA_Tunisia_modified-sld.xml");
        final Path styleFileName = stylePath.getFileName();
        final String styleName = IOUtilities.filenameWithoutExtension(styleFileName);
        Style style = styleBusiness.parseStyle(styleName, stylePath, styleFileName.toString());
        final Integer newStyleId = styleBusiness.createStyle("sld", style);

        styleBusiness.linkToLayer(newStyleId, layerId);
        styledLayerRepository.updateActivateStats(newStyleId, layerId, true);
        layerStatisticsJob.syncUpdateStyledLayerStatistics(newStyleId, layerId);

        final String extraInfo = styleBusiness.getExtraInfoForStyleAndLayer(newStyleId, layerId);
        final ObjectMapper mapper = getMapper();
        final LayerStatistics statistics = mapper.readValue(extraInfo, LayerStatistics.class);

        Assert.assertTrue(statistics.getTotalCount() == 232);
        Assert.assertEquals(statistics.getSeries().size(), 4);
        Assert.assertEquals(statistics.getTypes(), Arrays.asList(LayerStatistics.StatisticsType.POLYGON, LayerStatistics.StatisticsType.SIMPLE, LayerStatistics.StatisticsType.LINE, LayerStatistics.StatisticsType.POLYGON));
        Assert.assertEquals(statistics.getSeries(), Arrays.asList("9", "6", "PWA = 10", "25"));
        Assert.assertEquals(statistics.getCounts(), Arrays.asList(2L, 4L, 3L, 3L));
        Assert.assertEquals(statistics.getColors(), Arrays.asList("#44e92c", "#eb62f1", "#f23535", "#e61919"));
        Assert.assertEquals(statistics.getSizes(), Arrays.asList(null, "15", null, null));
        Assert.assertEquals(statistics.getStrokeColors(), Arrays.asList(null, "#4a44e7", "#f23535", null));
        Assert.assertEquals(statistics.getStrokeWidths(), Arrays.asList(null, "1.0", "1", null));

    }

    @Test
    public void statisticsDeactivateStatisticsTest() {
        styledLayerRepository.updateActivateStats(styleId, layerId, false);

        boolean error = false;
        try {
            styleBusiness.getExtraInfoForStyleAndLayer(styleId, layerId);
        } catch (ConstellationException e) {
            error = true;
        }

        Assert.assertTrue(error);
    }
}

