package com.examind.ogc.api.rest.common.dto;

import jakarta.xml.bind.annotation.XmlRegistry;

/**
 *
 * @author guilhem
 */
@XmlRegistry
public class ObjectFactory {

    /**
     * Create a new ObjectFactory that can be used to create new instances of
     * schema derived classes for package: net.opengis.wfs
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link LandingPage}
     *
     * @return
     */
    public LandingPage createLandingPage() {
        return new LandingPage();
    }

    /**
     * Create an instance of {@link Conformance}
     *
     * @return
     */
    public Conformance createConformance() {
        return new Conformance();
    }

    /**
     * Create an instance of {@link Collections}
     *
     * @return
     */
    public Collections createCollections() {
        return new Collections();
    }

    /**
     * Create an instance of {@link Collection}
     *
     * @return
     */
    public Collection createCollection() {
        return new Collection();
    }
}
