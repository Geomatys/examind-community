

package org.constellation.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.apache.sis.internal.jaxb.gco.Multiplicity;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.util.logging.Logging;
import org.constellation.exception.ConfigurationException;
import org.constellation.util.StoreUtilities;
import org.geotoolkit.coverage.io.GridCoverageReader;
import org.geotoolkit.feature.catalog.FeatureAttributeImpl;
import org.geotoolkit.feature.catalog.FeatureCatalogueImpl;
import org.geotoolkit.feature.catalog.FeatureTypeImpl;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.util.NamesExt;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.FeatureType;
import org.opengis.feature.catalog.FeatureCatalogue;
import org.opengis.feature.catalog.PropertyType;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.content.Band;
import org.opengis.metadata.content.ContentInformation;
import org.opengis.metadata.content.CoverageDescription;
import org.opengis.metadata.content.RangeDimension;
import org.opengis.metadata.identification.Identification;
import org.opengis.util.GenericName;
import org.opengis.util.LocalName;
import org.opengis.util.MemberName;
import org.opengis.util.NameFactory;
import org.opengis.util.TypeName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ISO19110Builder {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.provider");

    public static FeatureCatalogue createCatalogueFromProvider(final int providerID) throws ConfigurationException {
        DataProvider provider = DataProviders.getProvider(providerID);
        DataStore store = provider.getMainStore();
        try {
            return createCatalogueFromResources(DataStores.flatten(store, true));
        } catch (DataStoreException ex) {
            LOGGER.log(Level.WARNING, "error while generating featureCatalogue from feature Store", ex);
        }
        return null;
    }

    public static FeatureCatalogue createCatalogueForData(final int providerID, final QName dataName) throws ConfigurationException {
        DataProvider provider = DataProviders.getProvider(providerID);
        DataStore store = provider.getMainStore();
        GenericName name = NamesExt.create(dataName.getNamespaceURI(), dataName.getLocalPart());
        try {
            Resource rs = StoreUtilities.findResource(store, name.toString());
            if (rs != null) {
                return createCatalogueFromResources(Arrays.asList(rs));
            } else {
                LOGGER.log(Level.WARNING, "Unable to generate a ISO 19110 metadata for resource: {0}", name);
            }
        } catch (DataStoreException ex) {
            LOGGER.log(Level.WARNING, "error while generating featureCatalogue from feature Store", ex);
        }
        return null;
    }

    public static FeatureCatalogue createCatalogueFromResources(final Collection<? extends Resource> ressources) throws DataStoreException {
        final FeatureCatalogueImpl catalogue = new FeatureCatalogueImpl();
        // assign generated identifier
        catalogue.setId("fc-" + UUID.randomUUID().toString());
        for (Resource rs : ressources) {
            final FeatureTypeImpl featureType;
            if (rs instanceof GridCoverageResource) {
                featureType = createCatalogueFeatureFromCoverageResource((GridCoverageResource) rs);
            } else if (rs instanceof FeatureSet) {
                featureType = createCatalogueFeatureFromFeatureSet((FeatureSet)rs);
            } else {
                featureType = null;
                LOGGER.info("Unable to extract feature catalogue metadata from ressource:" + rs);
            }
            if (featureType != null) {
                catalogue.getFeatureType().add(featureType);
            }
        }
        return catalogue;
    }

    public static FeatureTypeImpl createCatalogueFeatureFromFeatureSet(final FeatureSet fs) throws DataStoreException {
        final FeatureType ft = fs.getType();
        final FeatureTypeImpl featureType = new FeatureTypeImpl();
        final LocalName localName         = ft.getName().tip();
        featureType.setTypeName(localName);
        if (ft.getDescription() != null) {
            featureType.setDefinition(ft.getDescription().toString());
        }

        final List<PropertyType> attributes = new ArrayList<>();
        for (org.opengis.feature.PropertyType desc : ft.getProperties(true)) {
            if (desc.getName().toString().startsWith("@")) {
                continue;
            }
            final LocalName attName = desc.getName().tip();
            final String code = desc.getName().toString();
            final TypeName type;
            final int lower;
            final int upper;
            if (desc instanceof AttributeType) {
                AttributeType att = (AttributeType) desc;
                lower = att.getMinimumOccurs();
                upper = att.getMaximumOccurs();
                type = getTypeNameFromSimpleClass(att.getValueClass());
            } else if (desc instanceof FeatureAssociationRole) {
                FeatureAssociationRole far = (FeatureAssociationRole) desc;
                lower = far.getMinimumOccurs();
                upper = far.getMaximumOccurs();
                try {
                    type = getTypeNameFromName(far.getValueType().getName());
                } catch (IllegalStateException ex) {
                    // voir tache jira GEOTK-709
                    LOGGER.log(Level.FINE, "Error while resolving attribute : " + code, ex);
                    continue;
                }
            } else {
                continue;
            }

            final NumberRange range = new NumberRange<>(Integer.class, lower, true, upper, true);
            final Multiplicity multi = new Multiplicity(range);
            final FeatureAttributeImpl att = new FeatureAttributeImpl(null, attName, null, multi, null, null, code, null, type);
            attributes.add(att);
        }
        featureType.setCarrierOfCharacteristics(attributes);
        return featureType;
    }

    public static FeatureTypeImpl createCatalogueFeatureFromCoverageResource(final GridCoverageResource cr) throws DataStoreException {

        Metadata meta = cr.getMetadata();
        if (cr instanceof org.geotoolkit.storage.coverage.GridCoverageResource) {
            final GridCoverageReader gcr = (GridCoverageReader) ((org.geotoolkit.storage.coverage.GridCoverageResource) cr).acquireReader();
            meta = gcr.getMetadata();
            ((org.geotoolkit.storage.coverage.GridCoverageResource) cr).recycle(gcr);
        }

        final FeatureTypeImpl featureType = new FeatureTypeImpl();
        final LocalName localName         = cr.getIdentifier().get().tip();
        featureType.setTypeName(localName);
        if (!meta.getIdentificationInfo().isEmpty()) {
            final Identification ident = meta.getIdentificationInfo().iterator().next();
            if (ident.getAbstract() != null) {
                featureType.setDefinition(ident.getAbstract().toString());
            }
        }
        final List<PropertyType> attributes = new ArrayList<>();

        for (ContentInformation content : meta.getContentInfo()) {
            if (content instanceof CoverageDescription) {
                final CoverageDescription covDesc = (CoverageDescription) content;

                for (RangeDimension dim : covDesc.getDimensions()) {
                    final MemberName identifier = dim.getSequenceIdentifier();
                    final LocalName attName     = identifier.head();
                    final String code           = attName.toString();
                    final TypeName type         = identifier.getAttributeType();
                    Multiplicity multi = null;
                    if (dim instanceof Band) {
                        final Band dimBand = (Band)dim;
                        int lower = -1;
                        if (dimBand.getMinValue() != null) {
                            lower = dimBand.getMinValue().intValue();
                        }
                        Integer upper = null;
                        if (dimBand.getMaxValue() != null) {
                            upper = dimBand.getMaxValue().intValue();
                        }
                        final NumberRange range = new NumberRange<>(Integer.class, lower, true, upper, true);
                        multi = new Multiplicity(range);
                    }

                    final FeatureAttributeImpl att = new FeatureAttributeImpl(null, attName, null, multi, null, null, code, null, type);
                    attributes.add(att);
                }
            }
        }

        featureType.setCarrierOfCharacteristics(attributes);
        return featureType;
    }

    public static TypeName getTypeNameFromName(final GenericName name) {
        final String ns = NamesExt.getNamespace(name);
        final NameFactory nameFactory = new DefaultNameFactory();
        return nameFactory.createTypeName(name.scope(), name.tip().toString());
    }

    public static TypeName getTypeNameFromSimpleClass(final Class c) {
        final NameFactory nameFactory = new DefaultNameFactory();
        final String className;
        if (c.equals(String.class)) {
            className = "CharacterString";
        } else {
            className = c.getSimpleName();
        }
        return nameFactory.createTypeName(null, className);
    }
}
