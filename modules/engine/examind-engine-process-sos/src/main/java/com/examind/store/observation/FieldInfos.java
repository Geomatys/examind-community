
package com.examind.store.observation;

import java.util.List;
import org.geotoolkit.observation.model.FieldType;
import static org.geotoolkit.observation.model.FieldType.PARAMETER;
import static org.geotoolkit.observation.model.FieldType.QUALITY;
import org.geotoolkit.observation.model.ObservationType;
import static org.geotoolkit.observation.model.ObservationType.PROFILE;

/**
 *
 * @author glegal
 */
public class FieldInfos {
    
    public final ObservationType observationType;
    public final boolean isProfile;
    public final List<MeasureField> measureFields;
    public final MeasureField mainProfileField;

    public FieldInfos(List<MeasureField> measureFields, MeasureField mainProfileField, ObservationType observationType) {
        this.observationType = observationType;
        this.isProfile = observationType.equals(PROFILE);
        this.mainProfileField = mainProfileField;
        this.measureFields = measureFields;
    }

    public MeasureField getExtraField(String fieldName, int index, FieldType type) {
        MeasureField field = null;
        for (MeasureField mf : measureFields) {
            if (mf.name.equals(fieldName)) {
                field = mf;
                break;
            }
        }
        if (field == null) throw new IllegalStateException("Unable to find a field named: " + fieldName);
        if (type  == null) throw new IllegalArgumentException("fieldtype must not be null");
        return switch (type) {
            case PARAMETER -> field.parameterFields.get(index);
            case QUALITY   -> field.qualityFields.get(index);
            default        -> throw new IllegalArgumentException("Only PARAMETER or QUALITY field type are expected");
        };
    }
    
    public MeasureBuilder newBuilder() {
        return new MeasureBuilder(this);
    }
}
