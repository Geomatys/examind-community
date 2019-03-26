package org.constellation.repository;

import java.util.List;


/**
 *
 */
public interface StyledLayerRepository {

    List<Integer> findByLayer(int layerId);
}
