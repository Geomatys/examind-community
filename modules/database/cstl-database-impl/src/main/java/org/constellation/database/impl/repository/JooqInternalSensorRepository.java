/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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
package org.constellation.database.impl.repository;

import java.util.List;
import org.constellation.dto.InternalSensor;
import static org.constellation.database.api.jooq.tables.InternalSensor.INTERNAL_SENSOR;
import org.constellation.database.api.jooq.tables.records.InternalSensorRecord;
import org.constellation.repository.InternalSensorRepository;
import org.jooq.Select;
import org.jooq.UpdateSetFirstStep;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
@DependsOn("database-initer")
public class JooqInternalSensorRepository extends AbstractJooqRespository<InternalSensorRecord, org.constellation.database.api.jooq.tables.pojos.InternalSensor>  implements InternalSensorRepository {

    public JooqInternalSensorRepository() {
        super(org.constellation.database.api.jooq.tables.pojos.InternalSensor.class, INTERNAL_SENSOR);
    }

    @Override
    public InternalSensor findBySensorId(String sensorId) {
        return dsl.select().from(INTERNAL_SENSOR).where(INTERNAL_SENSOR.SENSOR_ID.eq(sensorId)).fetchOneInto(InternalSensor.class);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public InternalSensor update(InternalSensor sensor) {
        UpdateSetFirstStep<InternalSensorRecord> update = dsl.update(INTERNAL_SENSOR);
        update.set(INTERNAL_SENSOR.SENSOR_ID, sensor.getSensorId());
        update.set(INTERNAL_SENSOR.METADATA, sensor.getMetadata()).where(INTERNAL_SENSOR.ID.eq(sensor.getId())).execute();
        return sensor;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int create(InternalSensor sensor) {
        InternalSensorRecord metadataRecord = dsl.newRecord(INTERNAL_SENSOR);
        metadataRecord.setId(sensor.getId());
        metadataRecord.setSensorId(sensor.getSensorId());
        metadataRecord.setMetadata(sensor.getMetadata());

        metadataRecord.store();
        return metadataRecord.getId();
    }

    @Override
    public boolean existsById(Integer id) {
        return dsl.selectCount().from(INTERNAL_SENSOR)
                .where(INTERNAL_SENSOR.ID.eq(id))
                .fetchOne(0, Integer.class) > 0;
    }

    @Override
    public List<String> getSensorIds() {
        return dsl.select(INTERNAL_SENSOR.SENSOR_ID).from(INTERNAL_SENSOR).fetchInto(String.class);
    }

    @Override
    public int countSensors() {
        final Select query = dsl.select(INTERNAL_SENSOR.SENSOR_ID).from(INTERNAL_SENSOR);
        return dsl.fetchCount(query);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(Integer id) {
        return dsl.delete(INTERNAL_SENSOR).where(INTERNAL_SENSOR.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteAll() {
        return dsl.delete(INTERNAL_SENSOR).execute();
    }

}
