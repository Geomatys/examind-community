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
package org.constellation.util;

import java.util.IdentityHashMap;
import java.util.Map;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.identification.Identification;

public final class StoreUtilities {
    private StoreUtilities() {
    }

    /**
     * Searches for a resource identified by the given identifier.
     * The given identifier should match the following metadata element of a resource:
     *
     * <blockquote>{@link Resource#getMetadata() metadata} /
     * {@link org.apache.sis.metadata.iso.DefaultMetadata#getIdentificationInfo() identificationInfo} /
     * {@link org.apache.sis.metadata.iso.identification.AbstractIdentification#getCitation() citation} /
     * {@link org.apache.sis.metadata.iso.citation.DefaultCitation#getIdentifiers() identifier}</blockquote>
     *
     * Implementation may also accept aliases for convenience. For example if the full name of a resource
     * is {@code "foo:bar"}, then this method may accept {@code "bar"} as a synonymous of {@code "foo:bar"}
     * provided that it does not introduce ambiguity.
     *
     * <p>The default implementation verifies if above criterion matches to this {@code DataStore}
     * (which is itself a resource), then iterates recursively over {@link Aggregate} components
     * if this data store is an aggregate.
     * If a match is found without ambiguity, the associated resource is returned.
     * Otherwise an exception is thrown. Subclasses are encouraged to override this method with a more efficient
     * implementation.</p>
     *
     * @param  identifier  identifier of the resource to fetch. Must be non-null.
     * @return resource associated to the given identifier (never {@code null}).
     * @throws IllegalNameException if no resource is found for the given identifier, or if more than one resource is found.
     * @throws DataStoreException if another kind of error occurred while searching resources.
     *
     * @deprecated The difference between this method and  DataStore#findResource(String) is that this method
     *             gives precedence to metadata instead than {@code resource.getIdentifier()}. New code should use
     *             the SIS method instead.
     */
    @Deprecated
    public static Resource findResource(final org.apache.sis.storage.DataStore store, final String identifier) throws DataStoreException {
        ArgumentChecks.ensureNonEmpty("identifier", identifier);
        final Resource resource = findResource(store, identifier, store, new IdentityHashMap<>());
        if (resource != null) {
            return resource;
        }
        return store.findResource(identifier);
    }

    /**
     * Recursively searches for a resource identified by the given identifier.
     * This is the implementation of {@link #findResource(DataStore, String)}.
     *
     * @param  identifier  identifier of the resource to fetch.
     * @param  candidate   a resource to compare against the identifier.
     * @param  visited     resources visited so-far, for avoiding never-ending loops if cycles exist.
     * @return resource associated to the given identifier, or {@code null} if not found.
     */
    private static Resource findResource(final org.apache.sis.storage.DataStore store, final String identifier, final Resource candidate,
            final Map<Resource,Boolean> visited) throws DataStoreException
    {
        if (candidate != null && visited.put(candidate, Boolean.TRUE) == null) {
            final Metadata metadata = candidate.getMetadata();
            if (metadata != null) {
                for (final Identification identification : metadata.getIdentificationInfo()) {
                    if (identification != null) {                                                   // Paranoiac check.
                        if (Citations.identifierMatches(identification.getCitation(), identifier)) {
                            return candidate;
                        }
                    }
                }
            }
            if (candidate instanceof Aggregate) {
                Resource result = null;
                for (final Resource child : ((Aggregate) candidate).components()) {
                    final Resource match = findResource(store, identifier, child, visited);
                    if (match != null) {
                        if (result == null) {
                            result = match;
                        } else {
                            throw new IllegalNameException(Resources.forLocale(store.getLocale())
                                    .getString(Resources.Keys.ResourceIdentifierCollision_2, store.getDisplayName(), identifier));
                        }
                    }
                }
                return result;
            }
        }
        return null;
    }
}
