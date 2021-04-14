package org.constellation.provider.datastore;

import org.apache.sis.storage.Resource;

/**
 * Mark this object as a proxy for a given SIS resource.
 *
 * WARNING: This is a workaround. When decorating resources to override their metadata, a dynamic proxy is used. It
 * allows to directly expose all interfaces from source. However, it does not exposes classes. To access capabilities
 * of a specific class that is not backed by an interface, it is needed to get back original resource. This is the
 * purpose of this interface: allow user to retrieve the origin resource if (s)he needs it.
 *
 * To conserve metadata override and avoid dependence on specific implementations, it is recommended to expose an
 * interface for your public functionalities and not rely on this interface if you can avoid it.
 */
public interface ResourceProxy {

    /**
     *
     * @return The resource that is decorated (if any). Should never be null.
     */
    Resource getOrigin();
}
