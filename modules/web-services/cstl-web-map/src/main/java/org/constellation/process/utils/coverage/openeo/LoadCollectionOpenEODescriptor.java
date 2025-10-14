package org.constellation.process.utils.coverage.openeo;

import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Process descriptor for loading a collection in OpenEO.
 *
 * @author Quentin BIALOTA (Geomatys)
 */
public class LoadCollectionOpenEODescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "coverage.openeo.load";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Load a collection.");

    public static final String SERVICE_NAME = "serviceId";
    private static final String SERVICE_REMARKS = "The service Id where the coverage is";
    public static final ParameterDescriptor<String> SERVICE = BUILDER
            .addName(SERVICE_NAME)
            .setRemarks(SERVICE_REMARKS)
            .setRequired(true)
            .create(String.class, null);

    public static final String EXTERNAL_STAC_URL_NAME = "external_stac_url";
    private static final String EXTERNAL_STAC_URL_REMARKS = "The external STAC URL to use instead of using internal Examind catalog.";
    public static final ParameterDescriptor<String> EXTERNAL_STAC_URL = BUILDER
            .addName(EXTERNAL_STAC_URL_NAME)
            .setRemarks(EXTERNAL_STAC_URL_REMARKS)
            .setRequired(false)
            .create(String.class, null);

    public static final String EXTERNAL_STAC_CUSTOM_PROCESS_NAME = "external_stac_custom_process";
    private static final String EXTERNAL_STAC_CUSTOM_PROCESS_REMARKS = "The external STAC Custom Process to use to load external STAC data.";
    public static final ParameterDescriptor<String> EXTERNAL_STAC_CUSTOM_PROCESS = BUILDER
            .addName(EXTERNAL_STAC_CUSTOM_PROCESS_NAME)
            .setRemarks(EXTERNAL_STAC_CUSTOM_PROCESS_REMARKS)
            .setRequired(false)
            .create(String.class, null);

    public static final String COVERAGE_LAYER_NAME = "id";
    private static final String COVERAGE_LAYER_REMARKS = "The collection Id to load";
    public static final ParameterDescriptor<String> COVERAGE_LAYER = BUILDER
            .addName(COVERAGE_LAYER_NAME)
            .setRemarks(COVERAGE_LAYER_REMARKS)
            .setRequired(true)
            .create(String.class, null);

    public static final String SPATIAL_EXTENT_NAME = "spatial_extent";
    private static final String SPATIAL_EXTENT_REMARKS =
            "Limits the data to load from the collection to the specified bounding box.\n" +
                    "\nSet this parameter to `null` to set no limit for the " +
                    "spatial extent. Be careful with this when loading large datasets!";
    public static final ParameterDescriptor<Envelope> SPATIAL_EXTENT = BUILDER
            .addName(SPATIAL_EXTENT_NAME)
            .setRemarks(SPATIAL_EXTENT_REMARKS)
            .setRequired(false)
            .create(Envelope.class, null);

    public static final String TEMPORAL_EXTENT_NAME = "temporal_extent";
    private static final String TEMPORAL_EXTENT_REMARKS =
        "Limits the data to load from the collection to the specified left-closed temporal interval. " +
                "The interval has to be specified as an array with exactly two elements:\n\n" +
                "1. The first element is the start of the temporal interval. " +
                "The specified instance in time is **included** in the interval.\n" +
                "2. The second element is the end of the temporal interval. The specified instance in time is **excluded** " +
                "from the interval.\n\n" +
//                "Also supports open intervals by setting one of the boundaries to `null`, but never both.\n\n" +
                "Set this parameter to `null` to set no limit for the temporal extent. Be careful with " +
                "this when loading large datasets!";
    public static final ParameterDescriptor<String[]> TEMPORAL_EXTENT = BUILDER
            .addName(TEMPORAL_EXTENT_NAME)
            .setRemarks(TEMPORAL_EXTENT_REMARKS)
            .setRequired(false)
            .create(String[].class, null);

    public static final String BANDS_NAME = "bands";
    private static final String BANDS_REMARKS = "Only adds the specified band into the data cube.\n\n" +
            "This parameter expects an array of integers or strings, where each integer specifies a band by its " +
            "band index (1-based) or name. For example, to load the first and third band of a collection, " +
            "set this parameter to `[\"1\", \"3\"]`. If you want to load a band named `test` set `[\"test\"]`. \n\n" +
            "Set this parameter to `null` to load all bands of the collection. Be careful with this when loading large datasets!";
    public static final ParameterDescriptor<String[]> BANDS = BUILDER
            .addName(BANDS_NAME)
            .setRemarks(BANDS_REMARKS)
            .setRequired(false)
            .create(String[].class, null);

    public static final String PROPERTIES_NAME = "properties";
    private static final String PROPERTIES_REMARKS = "Limits the data by metadata properties to include only data in the data cube " +
            "which all given conditions return true for (AND operation) \n\n. Specify key-value-pairs with the key being the name of " +
            "the queryable (for STAC APIs implementing the Filter Extension) or the metadata property name (for static " +
            "STAC catalogs or STAC APIs implementing the Query Extension). Queryables can be retrieved from the Queryables" +
            " endpoint of the STAC API and are often but not always aligned with the metadata property names. The value must " +
            "be a condition (user-defined process) to be evaluated against a STAC API or static catalog. This parameter throws " +
            "a PropertiesUnsupported exception if querying is not implemented for static STAC catalogs or STAC APIs that " +
            "don't support the Filter or Query Extension.";
    public static final ParameterDescriptor<Map> PROPERTIES = BUILDER
            .addName(PROPERTIES_NAME)
            .setRemarks(PROPERTIES_REMARKS)
            .setRequired(false)
            .create(Map.class, null);

    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(SERVICE, EXTERNAL_STAC_URL, EXTERNAL_STAC_CUSTOM_PROCESS, COVERAGE_LAYER, SPATIAL_EXTENT, TEMPORAL_EXTENT, BANDS, PROPERTIES);

    public static final String OUTPUT_NAME = "result";
    private static final String OUTPUT_REMARKS = "A GridCoverage containing the data of the loaded collection. " +
            "This object is a data cube that will be use for further processing.";
    public static final ParameterDescriptor<GridCoverage> OUTPUT = BUILDER
            .addName(OUTPUT_NAME)
            .setRemarks(OUTPUT_REMARKS)
            .setRequired(true)
            .create(GridCoverage.class, null);

    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
            .createGroup(OUTPUT);

    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public LoadCollectionOpenEODescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    public static final ProcessDescriptor INSTANCE = new LoadCollectionOpenEODescriptor();

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new LoadCollectionOpenEOProcess(this, input);
    }
}

