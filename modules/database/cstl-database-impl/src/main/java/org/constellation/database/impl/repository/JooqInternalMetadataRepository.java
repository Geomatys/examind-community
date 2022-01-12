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
import static com.examind.database.api.jooq.Tables.INTERNAL_METADATA;
import org.constellation.dto.metadata.InternalMetadata;
import com.examind.database.api.jooq.tables.records.InternalMetadataRecord;
import org.constellation.repository.InternalMetadataRepository;
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
public class JooqInternalMetadataRepository extends AbstractJooqRespository<InternalMetadataRecord, com.examind.database.api.jooq.tables.pojos.InternalMetadata> implements InternalMetadataRepository {

    public JooqInternalMetadataRepository() {
        super(com.examind.database.api.jooq.tables.pojos.InternalMetadata.class, INTERNAL_METADATA);
    }

    @Override
    public InternalMetadata findByMetadataId(String metadataId) {
        return convertToDto(dsl.select()
                               .from(INTERNAL_METADATA)
                               .where(INTERNAL_METADATA.METADATA_ID.eq(metadataId))
                               .fetchOneInto(com.examind.database.api.jooq.tables.pojos.InternalMetadata.class));
    }

    @Override
    public boolean existsById(Integer id) {
        return dsl.selectCount().from(INTERNAL_METADATA)
                .where(INTERNAL_METADATA.ID.eq(id))
                .fetchOne(0, Integer.class) > 0;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public InternalMetadata update(InternalMetadata metadata) {
        UpdateSetFirstStep<InternalMetadataRecord> update = dsl.update(INTERNAL_METADATA);
        update.set(INTERNAL_METADATA.METADATA_ID, metadata.getMetadataId());
        update.set(INTERNAL_METADATA.METADATA_ISO, metadata.getMetadataIso()).where(INTERNAL_METADATA.ID.eq(metadata.getId())).execute();
        return metadata;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int create(InternalMetadata metadata) {
        InternalMetadataRecord metadataRecord = dsl.newRecord(INTERNAL_METADATA);
        metadataRecord.setId(metadata.getId());
        metadataRecord.setMetadataId(metadata.getMetadataId());
        metadataRecord.setMetadataIso(metadata.getMetadataIso());

        metadataRecord.store();
        return metadataRecord.getId();
    }

    @Override
    public List<String> getMetadataIds() {
        return dsl.select(INTERNAL_METADATA.METADATA_ID).from(INTERNAL_METADATA).fetchInto(String.class);
    }

    @Override
    public int countMetadata() {
        final Select query = dsl.select(INTERNAL_METADATA.METADATA_ID).from(INTERNAL_METADATA);
        return dsl.fetchCount(query);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(Integer id) {
        return dsl.delete(INTERNAL_METADATA).where(INTERNAL_METADATA.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteAll() {
         return dsl.delete(INTERNAL_METADATA).execute();
    }

    @Override
    public int delete(String metadataId) {
        return dsl.delete(INTERNAL_METADATA).where(INTERNAL_METADATA.METADATA_ID.eq(metadataId)).execute();
    }

    private InternalMetadata convertToDto(com.examind.database.api.jooq.tables.pojos.InternalMetadata dao) {
        if (dao != null) {
            return new InternalMetadata(dao.getId(),
                    dao.getMetadataId(),
                    dao.getMetadataIso());
        }
        return null;
    }
}
