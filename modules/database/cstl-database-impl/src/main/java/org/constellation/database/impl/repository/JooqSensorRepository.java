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
import static org.constellation.database.api.jooq.Tables.PROVIDER_X_SOS;
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
    public List<String> getDataLinkedSensors(Integer dataID) {
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
    public boolean isLinkedDataToSensor(Integer dataId, Integer sensorID) {
        return dsl.selectCount().from(SENSORED_DATA)
                .where(SENSORED_DATA.SENSOR.eq(sensorID))
                .and(SENSORED_DATA.DATA.eq(dataId))
                .fetchOne(0, Integer.class) > 0;
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
    public List<Sensor> findByServiceId(Integer serviceId, String sensorType) {
        List<Sensor> results = new ArrayList<>();
        Condition typeFilter = null;
        if (sensorType != null) {
            typeFilter = SENSOR.TYPE.eq(sensorType);
        }

        // look for fully linked sensor providers.
        List<Integer> pids =
        dsl.select(PROVIDER_X_SOS.PROVIDER_ID)
           .from(PROVIDER_X_SOS)
           .where(PROVIDER_X_SOS.SOS_ID.eq(serviceId))
           .and(PROVIDER_X_SOS.ALL_SENSOR.eq(Boolean.TRUE))
           .fetchInto(Integer.class);

        if (!pids.isEmpty()) {
            SelectConditionStep step = dsl.select().from(SENSOR).where(SENSOR.PROVIDER_ID.in(pids));
            if (typeFilter != null) {
                step = step.and(SENSOR.TYPE.eq(sensorType));
            }
            results.addAll(convertIntoSensorDto(step.fetchInto(org.constellation.database.api.jooq.tables.pojos.Sensor.class)));
        }

        // look for individually linked sensors.
        SelectConditionStep step = dsl.select().from(SENSOR_X_SOS, SENSOR)
                .where(SENSOR_X_SOS.SOS_ID.eq(serviceId)).and(SENSOR_X_SOS.SENSOR_ID.eq(SENSOR.ID));

        if (typeFilter != null) {
            step = step.and(SENSOR.TYPE.eq(sensorType));
        }
        results.addAll(convertIntoSensorDto(step.fetchInto(org.constellation.database.api.jooq.tables.pojos.Sensor.class)));

        return results;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteAll() {
        dsl.delete(SENSOR_X_SOS).execute();
        return dsl.delete(SENSOR).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(Integer id) {
        return dsl.delete(SENSOR).where(SENSOR.ID.eq(id)).execute();
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
                .set(SENSOR.OM_TYPE, sensor.getOmType())
                .where(SENSOR.ID.eq(sensor.getId()))
                .execute();
    }

    @Override
    public boolean existsById(Integer sensorId) {
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
    public void linkSensorToService(int sensorID, int servID) {
        dsl.insertInto(SENSOR_X_SOS).set(SENSOR_X_SOS.SENSOR_ID, sensorID).set(SENSOR_X_SOS.SOS_ID, servID).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void unlinkSensorFromService(int sensorID, int servID) {
        dsl.delete(SENSOR_X_SOS).where(SENSOR_X_SOS.SENSOR_ID.eq(sensorID)).and(SENSOR_X_SOS.SOS_ID.eq(servID)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void unlinkSensorFromAllServices(int sensorID) {
        dsl.delete(SENSOR_X_SOS).where(SENSOR_X_SOS.SENSOR_ID.eq(sensorID)).execute();
    }

    @Override
    public boolean isLinkedSensorToService(int sensorID, int servID) {
         // look for sensor individually linked to the service.
        boolean linked = dsl.selectCount().from(SENSOR_X_SOS)
                .where(SENSOR_X_SOS.SENSOR_ID.eq(sensorID))
                .and(SENSOR_X_SOS.SOS_ID.eq(servID))
                .fetchOne(0, Integer.class) > 0;

        if (linked) {
            return true;
        }

        // look for sensor in a fully linked sensor provider.
        return dsl.selectCount()
                     .from(SENSOR, PROVIDER_X_SOS)
                     .where(SENSOR.ID.eq(sensorID))
                     .and(SENSOR.PROVIDER_ID.eq(PROVIDER_X_SOS.PROVIDER_ID))
                     .and(PROVIDER_X_SOS.SOS_ID.eq(servID))
                     .and(PROVIDER_X_SOS.ALL_SENSOR.eq(true))
                    .fetchOneInto(Integer.class) > 0;
    }
    
    @Override
    public List<Integer> getLinkedServices(Integer sensorID) {
        return dsl.select(SENSOR_X_SOS.SOS_ID).from(SENSOR_X_SOS)
                .where(SENSOR_X_SOS.SENSOR_ID.eq(sensorID))
                .fetchInto(Integer.class);
    }

    @Override
    public int getLinkedSensorCount(int serviceId) {
        int count = 0;
        // look for fully linked sensor providers.
        List<Integer> pids =
        dsl.select(PROVIDER_X_SOS.PROVIDER_ID)
           .from(PROVIDER_X_SOS)
           .where(PROVIDER_X_SOS.SOS_ID.eq(serviceId))
           .and(PROVIDER_X_SOS.ALL_SENSOR.eq(Boolean.TRUE))
           .fetchInto(Integer.class);

        if (!pids.isEmpty()) {
            count = count + dsl.selectCount().from(SENSOR).where(SENSOR.PROVIDER_ID.in(pids)).fetchOneInto(Integer.class);
        }

        // look for individually linked sensors.
        count = count + dsl.selectCount()
                           .from(SENSOR_X_SOS, SENSOR)
                           .where(SENSOR_X_SOS.SOS_ID.eq(serviceId))
                           .and(SENSOR_X_SOS.SENSOR_ID.eq(SENSOR.ID))
                           .fetchOneInto(Integer.class);
        return count;
    }

    @Override
    public List<String> findIdentifierByServiceId(int serviceId, String sensorType) {
        List<String> results = new ArrayList<>();
        Condition typeFilter = null;
        if (sensorType != null) {
            typeFilter = SENSOR.TYPE.eq(sensorType);
        }

        // look for fully linked sensor providers.
        List<Integer> pids =
        dsl.select(PROVIDER_X_SOS.PROVIDER_ID)
           .from(PROVIDER_X_SOS)
           .where(PROVIDER_X_SOS.SOS_ID.eq(serviceId))
           .and(PROVIDER_X_SOS.ALL_SENSOR.eq(Boolean.TRUE))
           .fetchInto(Integer.class);

        if (!pids.isEmpty()) {
            SelectConditionStep step = dsl.select(SENSOR.IDENTIFIER).from(SENSOR).where(SENSOR.PROVIDER_ID.in(pids));
            if (typeFilter != null) {
                step = step.and(SENSOR.TYPE.eq(sensorType));
            }
            results.addAll(step.fetchInto(String.class));
        }

        // look for individually linked sensors.
        SelectConditionStep step = dsl.select(SENSOR.IDENTIFIER).from(SENSOR_X_SOS, SENSOR)
                .where(SENSOR_X_SOS.SOS_ID.eq(serviceId)).and(SENSOR_X_SOS.SENSOR_ID.eq(SENSOR.ID));

        if (typeFilter != null) {
            step = step.and(SENSOR.TYPE.eq(sensorType));
        }
        results.addAll(step.fetchInto(String.class));

        return results;
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
            sensorDTO.setOmType(sensor.getOmType());
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
