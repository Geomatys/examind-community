package org.constellation.provider.datastore;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntSupplier;

import org.opengis.metadata.Metadata;
import org.opengis.util.GenericName;

import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;

import org.constellation.business.IMetadataBusiness;
import org.constellation.dto.metadata.Attachment;
import org.constellation.dto.metadata.GroupStatBrief;
import org.constellation.dto.metadata.MetadataBrief;
import org.constellation.dto.metadata.MetadataLightBrief;
import org.constellation.dto.metadata.MetadataLists;
import org.constellation.dto.metadata.MetadataWithState;
import org.constellation.dto.metadata.OwnerStatBrief;
import org.constellation.dto.metadata.RootObj;
import org.constellation.dto.metadata.User;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Node;

import static org.constellation.provider.datastore.DataStoreHandle.METADATA_GETTER_NAME;
import static org.constellation.provider.datastore.DataStoreHandle.createProxy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProxyTest {

    private static final Metadata DEFAULT_METADATA;
    public static final IntSupplier FIX_ID = () -> 0;

    static {
        final MetadataBuilder builder = new MetadataBuilder();
        builder.addAuthor("Examind");
        DEFAULT_METADATA = builder.build(true);
    }

    @Test
    public void ensureMethodNameIsValid() {
        try {
            final Method method = Resource.class.getMethod(METADATA_GETTER_NAME);
            Assert.assertTrue(
                    "Metadata getter does not return a GeoAPI metadata !",
                    Metadata.class.isAssignableFrom(method.getReturnType())
            );
        } catch (NoSuchMethodException e) {
            fail("Cannot find any metadata getter using method name: "+METADATA_GETTER_NAME);
        }
    }

    @Test
    public void ensureInterfacesAreKept() {
        final Resource proxy = createProxy(FIX_ID, new MockResource(), new MockMetadataBusiness());
        assertTrue("Resource interfaces have not been preserved", proxy instanceof MustBePreserved);
        assertEquals("Interface behavior should be kept intact", new MustBePreserved(){}.testMethod(), ((MustBePreserved)proxy).testMethod());
    }

    @Test
    public void ensureNeitherCloneableNorSerializable() {
        final Resource proxy = createProxy(FIX_ID, new MockResource(), new MockMetadataBusiness());
        assertFalse("Proxy should not be cloneable", proxy instanceof Cloneable);
        // This test is impossible : all proxy instances are serializable : https://docs.oracle.com/javase/8/docs/technotes/guides/reflection/proxy.html#serial
        //assertFalse("Proxy should not be serializable", proxy instanceof Serializable);
    }

    @Test
    public void ensureMetadataFallback() throws DataStoreException {
        final Resource proxy = createProxy(FIX_ID, new MockResource(), new MockMetadataBusiness());
        final Metadata md = proxy.getMetadata();
        assertTrue("Origin metadata has not been returned when no override is available", md == DEFAULT_METADATA);
    }

    @Test
    public void ensureMetadataIsOverriden() throws DataStoreException {
        final MetadataBuilder builder = new MetadataBuilder();
        builder.addAuthor("Not Examind");
        final DefaultMetadata newMetadata = builder.build(true);
        final MockMetadataBusiness mdBiz = new MockMetadataBusiness();
        mdBiz.toReturn = newMetadata;
        final Resource proxy = createProxy(FIX_ID, new MockResource(), mdBiz);
        final Metadata md = proxy.getMetadata();
        assertTrue("Returned metadata does not come from metadata business", md == newMetadata);
    }

    @Test
    public void errors_from_proxified_resource_should_not_be_wrapped() {
        final Resource proxy = createProxy(FIX_ID, new MockResource(), new MockMetadataBusiness());
        try {
            proxy.addListener(StoreEvent.class, null);
            fail("Wrapped resource error should have been propagated");
        } catch (UnsupportedOperationException e) {
            // Expected behavior
        } catch (Exception e) {
            throw new AssertionError("Raised error is not the one expected", e);
        }

        final String errorMsg = "this is a unit test for propagation of errors";
        try {
            ((CheckedExceptionVerification)proxy).throwError(errorMsg);
            fail("Wrapped resource error should have been propagated");
        } catch (DataStoreException e) {
            assertEquals(errorMsg, e.getMessage());
        } catch (Exception e) {
            throw new AssertionError("Raised error is not the one expected", e);
        }
    }

    @Test
    public void test_equality() {
        final MockResource r1 = new MockResource();
        final MockMetadataBusiness mdB1 = new MockMetadataBusiness();
        final Resource p1 = createProxy(FIX_ID, r1, mdB1);
        assertEquals(p1, p1);
        assertNotEquals(p1, r1);

        final Resource p1Bis = createProxy(FIX_ID, r1, mdB1);
        assertEquals(p1, p1Bis);

        final Resource p2 = createProxy(FIX_ID, r1, new MockMetadataBusiness());
        assertNotEquals(p1, p2);

        final Resource p3 = createProxy(FIX_ID, new MockResource(), mdB1);
        assertNotEquals(p1, p3);
    }

    @Test
    public void ensure_origin_is_reachable() {
        final MockResource r1 = new MockResource();
        final Resource proxy = createProxy(FIX_ID, r1, new MockMetadataBusiness());
        assertTrue("A resource decoration should implement ResourceProxy interface", proxy instanceof ResourceProxy);
        assertEquals("Asking the proxy for the origin resource should properly return it", r1, ((ResourceProxy) proxy).getOrigin());
    }

    private interface MustBePreserved {
        default String testMethod() {
            return "OK !";
        }
    }

    /**
     * Used for testing proper propagation or errors through proxy (see {@link #errors_from_proxified_resource_should_not_be_wrapped()}.
     */
    private interface CheckedExceptionVerification {
        default void throwError(String errorMessage) throws DataStoreException {
            throw new DataStoreException(errorMessage);
        }
    }

    private class MockResource implements Resource, MustBePreserved, CheckedExceptionVerification, Cloneable, Serializable {

        @Override
        public Optional<GenericName> getIdentifier() throws DataStoreException {
            return Optional.empty();
        }

        @Override
        public Metadata getMetadata() throws DataStoreException {
            return DEFAULT_METADATA;
        }

        @Override
        public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }
    }

    private static class MockMetadataBusiness implements IMetadataBusiness {

        private Metadata toReturn;

        @Override
        public MetadataBrief searchFullMetadata(String metadataId, boolean includeService, boolean onlyPublished, Integer providerID) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public boolean existInternalMetadata(String metadataID, boolean includeService, boolean onlyPublished, Integer providerID) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public boolean existMetadataTitle(String title) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public List<String> getMetadataIds(boolean includeService, boolean onlyPublished, Integer providerID, String type) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public int getMetadataCount(boolean includeService, boolean onlyPublished, Integer providerID, String type) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public List<MetadataBrief> getByProviderId(int providerID, String type) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public MetadataLightBrief updateMetadata(String metadataId, Object metadataObj, Integer dataID, Integer datasetID, Integer mapcontextID, Integer owner, Integer providerId, String type) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public MetadataLightBrief updateMetadata(String metadataId, Object metadataObj, Integer dataID, Integer datasetID, Integer mapcontextID, Integer owner, Integer providerId, String type, String templateName, boolean hidden) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public boolean updatePartialMetadata(String metadataId, Map<String, Object> properties, Integer providerId) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public List<String> getLinkedMetadataIDs(String cswIdentifier, boolean partial, boolean includeService, boolean onlyPublished, String type) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public int getLinkedMetadataCount(String cswIdentifier, boolean partial, boolean includeService, boolean onlyPublished, String type) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void linkMetadataIDToCSW(String metadataId, String cswIdentifier) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void unlinkMetadataIDToCSW(String metadataId, String cswIdentifier) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public boolean isLinkedMetadataToCSW(String metadataID, String cswID, boolean partial, boolean includeService, boolean onlyPublished) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public boolean isLinkedMetadataToCSW(String metadataID, String cswID) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public MetadataLists getMetadataCodeLists() {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public Object getMetadata(int id) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public Object getMetadata(String metadataId) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public MetadataLightBrief getMetadataPojo(String metadataId) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public MetadataBrief getMetadataPojo(int metadataId) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public Node getMetadataNode(String metadataId) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public String getMetadataXml(int id) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public MetadataBrief getMetadataById(int id) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void updatePublication(int id, boolean newStatus) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void updatePublication(List<Integer> ids, boolean newStatus) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void updateHidden(int id, boolean newStatus) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void updateHidden(List<Integer> ids, boolean newStatus) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void updateProfile(Integer id, String newProfile) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void updateValidation(int id, boolean newStatus) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void updateOwner(int id, int newOwner) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void updateOwner(List<Integer> ids, int newOwner) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void deleteMetadata(int id) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public boolean deleteMetadata(String metadataID) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void deleteDataMetadata(int dataId) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void deleteDatasetMetadata(int datasetId) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void deleteMapContextMetadata(int mapContextId) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void deleteMetadata(List<Integer> ids) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void deleteAllMetadata() throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public Integer getCompletionForDataset(int datasetId) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public Object getIsoMetadataForData(int dataId) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public List<Object> getIsoMetadatasForData(int dataId) throws ConfigurationException {
            final Metadata defCopy = toReturn;
            return (defCopy == null) ? Collections.EMPTY_LIST : Collections.singletonList(defCopy);
        }

        @Override
        public Object getIsoMetadataForService(int serviceId) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public Object getIsoMetadataForDataset(int datasetId) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void updateCSWIndex(List<MetadataWithState> metadatas, boolean update) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public MetadataLightBrief duplicateMetadata(int id, String newTitle, String newType) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public int countTotal(Map<String, Object> filterMap) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public int[] countInCompletionRange(Map<String, Object> filterMap) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public int countPublished(boolean status, Map<String, Object> filterMap) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public Map<String, Integer> getProfilesCount(Map<String, Object> filterMap, String dataType) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public List<String> getProfilesMatchingType(String dataType) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public int countValidated(boolean status, Map<String, Object> filterMap) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public Object unmarshallMetadata(String metadata) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public Object unmarshallMetadata(Path metadata) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public String marshallMetadata(Object metadata) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void askForValidation(int metadataID) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void askForValidation(List<Integer> ids, String metadataLink, boolean sendEmails) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void denyValidation(int metadataID, String comment) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void denyValidation(MetadataBrief metadata, String comment, String metadataLink) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void acceptValidation(int metadataID) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void acceptValidation(MetadataBrief metadata, String metadataLink) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public Map.Entry<Integer, List<MetadataBrief>> filterAndGetBrief(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public List<MetadataLightBrief> filterAndGetWithoutPagination(Map<String, Object> filterMap) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public List<OwnerStatBrief> getOwnerStatBriefs(Map<String, Object> filter) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public List<GroupStatBrief> getGroupStatBriefs(Map<String, Object> filter) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public List<User> getUsers() {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public User getUser(int id) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public Object getMetadataFromFile(Path metadataFile) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public boolean isSpecialMetadataFormat(Path metadataFile) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public Object getMetadataFromSpecialFormat(Path metadataFile) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public Attachment getMetadataAttachment(int attachmentID) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public List<Attachment> getMetadataAttachmentByFileName(String fileName) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public int createMetadataAttachment(InputStream stream, String fileName) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void linkMetadataAtachment(int metadataID, int attchmentId) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void unlinkMetadataAtachment(int metadataID, int attchmentId) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void linkMetadataData(int metadataID, int dataId) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void unlinkMetadataData(int metadataID, int dataId) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void linkMetadataDataset(int metadataID, int datasetId) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void unlinkMetadataDataset(int metadataID, int datasetId) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void linkMetadataMapContext(int metadataID, int contextId) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void unlinkMetadataMapContext(int metadataID, int contextId) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public int addMetadataAtachment(int metadataID, URI path, String fileName) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public int addMetadataAtachment(int metadataID, InputStream content, String fileName) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void deleteFromProvider(int identifier) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public Integer getDefaultInternalProviderID() throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public String getJsonDatasetMetadata(int datasetId, boolean prune, boolean override) throws ConstellationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public String getJsonDataMetadata(int data, boolean prune, boolean override) throws ConstellationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void mergeDatasetMetadata(int datasetId, RootObj metadataValues) throws ConstellationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void mergeDataMetadata(int dataId, RootObj metadataValues) throws ConstellationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public Map<String, Integer> getStats(Map<String, Object> filter) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void updateSharedProperty(List<Integer> ids, boolean shared) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void updateSharedProperty(int id, boolean shared) throws ConfigurationException {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public List<MetadataLightBrief> getMetadataBriefForData(int dataId) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public List<MetadataLightBrief> getMetadataBriefForDataset(int datasetId) {
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 23/03/2020
        }

        @Override
        public void linkMetadataIDsToCSW(List<String> metadataIds, String cswIdentifier) throws ConstellationException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void unlinkMetadataIDsToCSW(List<String> metadataIds, String cswIdentifier) throws ConstellationException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
