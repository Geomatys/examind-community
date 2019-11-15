package org.constellation.database.impl.repository;

import java.util.ArrayList;
import java.util.Date;
import org.constellation.dto.Sensor;
import org.constellation.database.api.jooq.tables.records.SensorRecord;
import org.constellation.dto.SensorReference;
import org.constellation.repository.SensorRepository;
import org.jooq.Field;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.constellation.database.api.jooq.Tables.DATA;
import static org.constellation.database.api.jooq.Tables.SENSOR;
import static org.constellation.database.api.jooq.Tables.SENSORED_DATA;
import static org.constellation.database.api.jooq.Tables.SENSOR_X_SOS;
import org.jooq.Condition;
import org.jooq.SelectConditionStep;
import org.springframework.context.annotation.DependsOn;

@Component
@DependsOn("database-initer")
public class JooqSensorRepository extends AbstractJooqRespository<SensorRecord, org.constellation.database.api.jooq.tables.pojos.Sensor> implements SensorRepository {

    private static final Field[] REFERENCE_FIELDS = new Field[]{
            SENSOR.ID.as("id"),
            SENSOR.IDENTIFIER.as("identifier")};


    public JooqSensorRepository() {
        super(org.constellation.database.api.jooq.tables.pojos.Sensor.class, SENSOR);
    }

    @Override
    public List<String> getLinkedSensors(Integer dataID) {
        return dsl.select(SENSOR.IDENTIFIER).from(SENSOR).join(SENSORED_DATA).onKey()
                .where(SENSORED_DATA.DATA.eq(dataID)).fetch(SENSOR.IDENTIFIER);
    }

    @Override
    public List<Integer> getLinkedDatas(Integer sensorID) {
        return dsl.select(DATA.ID).from(DATA).join(SENSORED_DATA).onKey()
                .where(SENSORED_DATA.SENSOR.eq(sensorID)).fetchInto(Integer.class);
    }

    @Override
    public List<Integer> getLinkedDataProviders(Integer sensorID) {
        return dsl.select(DATA.PROVIDER).from(DATA).join(SENSORED_DATA).onKey()
                .where(SENSORED_DATA.SENSOR.eq(sensorID)).fetchInto(Integer.class);
    }

    @Override
    public Sensor findByIdentifier(String identifier) {
        return convertIntoSensorDto(dsl.select().from(SENSOR).where(SENSOR.IDENTIFIER.eq(identifier)).fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Sensor.class));
    }

    @Override
    public Integer findIdByIdentifier(String identifier) {
        return dsl.select(SENSOR.ID).from(SENSOR).where(SENSOR.IDENTIFIER.eq(identifier)).fetchOneInto(Integer.class);
    }

    @Override
    public Sensor findById(Integer id) {
        return convertIntoSensorDto(dsl.select().from(SENSOR).where(SENSOR.ID.eq(id)).fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Sensor.class));
    }

    @Override
    public List<Sensor> getChildren(String parent) {
        return convertIntoSensorDto(dsl.select().from(SENSOR).where(SENSOR.PARENT.eq(parent)).fetchInto(org.constellation.database.api.jooq.tables.pojos.Sensor.class));
    }

    @Override
    public List<Sensor> findAll() {
        return convertIntoSensorDto(dsl.select().from(SENSOR).fetchInto(org.constellation.database.api.jooq.tables.pojos.Sensor.class));
    }

    @Override
    public List<Sensor> findByProviderId(int providerId) {
        return convertIntoSensorDto(dsl.select().from(SENSOR).where(SENSOR.PROVIDER_ID.eq(providerId)).fetchInto(org.constellation.database.api.jooq.tables.pojos.Sensor.class));
    }

    @Override
    public List<Sensor> findByServiceId(Integer id) {
        return convertIntoSensorDto(dsl.select().from(SENSOR)
                .join(SENSOR_X_SOS).on(SENSOR_X_SOS.SENSOR_ID.eq(SENSOR.ID))
                .where(SENSOR_X_SOS.SOS_ID.eq(id))
                .fetchInto(org.constellation.database.api.jooq.tables.pojos.Sensor.class));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteAll() {
        dsl.delete(SENSOR_X_SOS).execute();
        dsl.delete(SENSOR).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void delete(String identifier) {
        dsl.delete(SENSOR).where(SENSOR.IDENTIFIER.eq(identifier)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void delete(String sensorid, Integer providerId) {
        dsl.delete(SENSOR).where(SENSOR.IDENTIFIER.eq(sensorid)).and(SENSOR.PROVIDER_ID.eq(providerId)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteFromProvider(Integer providerId) {
        dsl.delete(SENSOR_X_SOS).where(SENSOR_X_SOS.SENSOR_ID.in(dsl.select(SENSOR.ID).from(SENSOR).where(SENSOR.PROVIDER_ID.eq(providerId)))).execute();
        dsl.delete(SENSOR).where(SENSOR.PROVIDER_ID.eq(providerId)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void linkDataToSensor(Integer dataId , Integer sensorId) {
        dsl.insertInto(SENSORED_DATA).set(SENSORED_DATA.DATA, dataId).set(SENSORED_DATA.SENSOR, sensorId).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void unlinkDataToSensor(Integer dataId, Integer sensorId) {
        dsl.delete(SENSORED_DATA).where(SENSORED_DATA.DATA.eq(dataId)).and(SENSORED_DATA.SENSOR.eq(sensorId)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Integer create(Sensor sensor) {
        SensorRecord sensorRecord = dsl.newRecord(SENSOR);
        sensorRecord.from(sensor);
        sensorRecord.store();
        return sensorRecord.getId();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void update(Sensor sensor) {
        dsl.update(SENSOR)
                .set(SENSOR.IDENTIFIER, sensor.getIdentifier())
                .set(SENSOR.OWNER, sensor.getOwner())
                .set(SENSOR.PARENT, sensor.getParent())
                .set(SENSOR.TYPE, sensor.getType())
                .set(SENSOR.DATE, sensor.getDate() != null ? sensor.getDate().getTime() : null)
                .set(SENSOR.PROVIDER_ID, sensor.getProviderId())
                .set(SENSOR.PROFILE, sensor.getProfile())
                .where(SENSOR.ID.eq(sensor.getId()))
                .execute();
    }

    @Override
    public boolean existsById(int sensorId) {
        return dsl.selectCount().from(SENSOR)
                .where(SENSOR.ID.eq(sensorId))
                .fetchOne(0, Integer.class) > 0;
    }

    @Override
    public boolean existsByIdentifier(String sensorId) {
        return dsl.selectCount().from(SENSOR)
                .where(SENSOR.IDENTIFIER.eq(sensorId))
                .fetchOne(0, Integer.class) > 0;
    }

    @Override
    public List<SensorReference> fetchByDataId(int dataId) {
        return dsl.select(REFERENCE_FIELDS).from(SENSOR)
                .join(SENSORED_DATA).on(SENSORED_DATA.SENSOR.eq(SENSOR.ID))
                .where(SENSORED_DATA.DATA.eq(dataId))
                .fetchInto(SensorReference.class);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void linkSensorToSOS(int sensorID, int sosID) {
        dsl.insertInto(SENSOR_X_SOS).set(SENSOR_X_SOS.SENSOR_ID, sensorID).set(SENSOR_X_SOS.SOS_ID, sosID).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void unlinkSensorFromSOS(int sensorID, int sosID) {
        dsl.delete(SENSOR_X_SOS).where(SENSOR_X_SOS.SENSOR_ID.eq(sensorID)).and(SENSOR_X_SOS.SOS_ID.eq(sosID)).execute();
    }

    @Override
    public boolean isLinkedSensorToSOS(int sensorID, int sosID) {
        return dsl.selectCount().from(SENSOR_X_SOS)
                .where(SENSOR_X_SOS.SENSOR_ID.eq(sensorID))
                .and(SENSOR_X_SOS.SOS_ID.eq(sosID))
                .fetchOne(0, Integer.class) > 0;
    }

    @Override
    public List<Integer> getLinkedServices(Integer sensorID) {
        return dsl.select(SENSOR_X_SOS.SOS_ID).from(SENSOR_X_SOS)
                .where(SENSOR_X_SOS.SENSOR_ID.eq(sensorID))
                .fetchInto(Integer.class);
    }

    @Override
    public int getLinkedSensorCount(int serviceId) {
        return dsl.selectCount().from(SENSOR_X_SOS)
                .where(SENSOR_X_SOS.SOS_ID.eq(serviceId))
                .fetchOne(0, Integer.class);
    }

    @Override
    public List<String> getLinkedSensorIdentifiers(int serviceId, String sensorType) {
        Condition typeFilter = null;
        if (sensorType != null) {
            typeFilter = SENSOR.TYPE.eq(sensorType);
        }
        SelectConditionStep step = dsl.select(SENSOR.IDENTIFIER).from(SENSOR_X_SOS, SENSOR)
                .where(SENSOR_X_SOS.SOS_ID.eq(serviceId)).and(SENSOR_X_SOS.SENSOR_ID.eq(SENSOR.ID));

        if (typeFilter != null) {
            step = step.and(SENSOR.TYPE.eq(sensorType));
        }
        return step.fetchInto(String.class);
    }

    private List<Sensor> convertIntoSensorDto(final List<org.constellation.database.api.jooq.tables.pojos.Sensor> sensors) {
        List<Sensor> results = new ArrayList<>();
        for (org.constellation.database.api.jooq.tables.pojos.Sensor sensor : sensors) {
            results.add(convertIntoSensorDto(sensor));
        }
        return results;
    }

    private Sensor convertIntoSensorDto(final org.constellation.database.api.jooq.tables.pojos.Sensor sensor) {
        if (sensor != null) {
            final Sensor sensorDTO = new Sensor();
            sensorDTO.setOwner(sensor.getOwner());
            sensorDTO.setDate(new Date(sensor.getDate()));
            sensorDTO.setId(sensor.getId());
            sensorDTO.setIdentifier(sensor.getIdentifier());
            sensorDTO.setType(sensor.getType());
            sensorDTO.setProviderId(sensor.getProviderId());
            sensorDTO.setParent(sensor.getParent());
            sensorDTO.setProfile(sensor.getProfile());
            return sensorDTO;
        }
        return null;
    }
}
