/*
 *    Examind - An open source and standard compliant SDI
 *    http://www.examind.fr
 *
 * Copyright 2023 Geomatys.
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
package org.constellation.store.observation.db.model;

import java.sql.SQLException;
import javax.measure.IncommensurableException;
import javax.measure.UnconvertibleException;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.format.MeasurementParseException;
import org.apache.sis.measure.Units;
import org.geotoolkit.observation.model.FieldDataType;
import org.geotoolkit.observation.model.FieldType;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class InsertDbField extends DbField {

    private UnitConverter valueConverter;

    public InsertDbField(Integer index, FieldDataType dataType, String name, String label, String description, String uom, FieldType type, int tableNumber) {
        super(index, dataType, name, label, description, uom, type, tableNumber);
    }

    public InsertDbField(DbField original) {
        super(original, original.tableNumber);
    }

    public void setInputUom(String inputUom) throws SQLException {
        if (this.uom != null && inputUom != null &&
            !this.uom.equals(inputUom)) {
            try {
                Unit<?> fieldUOM = Units.valueOf(this.uom);
                Unit<?> inputUOM = Units.valueOf(inputUom);

                valueConverter = inputUOM.getConverterToAny(fieldUOM);
            } catch (IncommensurableException | UnconvertibleException | MeasurementParseException | IllegalStateException ex) {
                throw new SQLException("Error while looking for uom converter " + this.uom + " => " + inputUom + " for field: " + this.name, ex);
            }
        }
    }

    public Number convertValue(Number value) {
        if (valueConverter != null && value != null) {
            return valueConverter.convert(value);
        }
        return value;
    }
}
