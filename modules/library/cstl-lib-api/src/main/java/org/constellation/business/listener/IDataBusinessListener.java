package org.constellation.business.listener;

import org.constellation.dto.Data;

/**
 * Listener that make extension point for project based on Examind that want to
 * add/change some behavior in Examind DataBusiness.
 *
 * @author Quentin Boileau (Geomatys)
 */
public interface IDataBusinessListener {

    /**
     * Called after create new data entry.
     * @param newData
     */
    void postDataCreate(Data newData);

    /**
     * Called before delete a data in Data table.
     * @param removedData
     */
    void preDataDelete(Data removedData);

    /**
     * Called after delete a data in Data table.
     * @param removedData
     */
    void postDataDelete(Data removedData);
}
