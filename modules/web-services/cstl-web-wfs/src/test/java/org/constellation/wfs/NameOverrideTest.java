package org.constellation.wfs;

import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.iso.Names;
import org.geotoolkit.feature.FeatureExt;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.feature.FeatureType;
import org.opengis.metadata.acquisition.GeometryType;
import org.opengis.util.GenericName;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class NameOverrideTest {

    /**
     * Ensure the decoration exposes a different name.
     */
    @Test
    public void replaceName() {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        builder.addAttribute(String.class).setName("an attribute");
        final FeatureType source = builder.setName("Source").build();

        final GenericName overridenName = Names.createLocalName("A namespace", ":", "random name");
        final FeatureType override = NameOverride.wrap(source, overridenName);

        Assert.assertEquals("Name has not been changed as expected", overridenName, override.getName());
    }

    /**
     * Check that decoration exposes the same properties and conventions as wrapped
     * feature type.
     */
    @Test
    public void preserveProperties() {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        builder.addAttribute(String.class).setName("an attribute");
        builder.addAttribute(Double.class).setName("another attribute");
        builder.addAttribute(GeometryType.LINEAR).setName("Geometry")
                .setCRS(CommonCRS.defaultGeographic())
                .addRole(AttributeRole.DEFAULT_GEOMETRY);
        final FeatureType source = builder.setName("Source").build();

        final FeatureType sourceCopy = new FeatureTypeBuilder(source).build();

        final GenericName overridenName = Names.createLocalName("A namespace", ":", "random name");
        final FeatureType override = NameOverride.wrap(source, overridenName);

        Assert.assertEquals("Source feature type should not be altered", sourceCopy, source);
        Assert.assertTrue("Original properties have been altered !", FeatureExt.sameProperties(source, override, true, false));
    }
}
