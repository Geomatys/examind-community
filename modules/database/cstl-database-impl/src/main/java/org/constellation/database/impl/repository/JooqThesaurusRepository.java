/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015 Geomatys.
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.constellation.dto.thesaurus.Thesaurus;
import org.constellation.database.api.jooq.tables.records.ThesaurusRecord;
import org.constellation.repository.ThesaurusRepository;
import org.springframework.stereotype.Component;
import static org.constellation.database.api.jooq.Tables.THESAURUS;
import static org.constellation.database.api.jooq.Tables.THESAURUS_LANGUAGE;
import static org.constellation.database.api.jooq.Tables.THESAURUS_X_SERVICE;
import org.constellation.database.api.jooq.tables.records.ThesaurusLanguageRecord;
import org.jooq.Record;
import org.springframework.context.annotation.DependsOn;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
@DependsOn("database-initer")
public class JooqThesaurusRepository extends AbstractJooqRespository<ThesaurusRecord, org.constellation.database.api.jooq.tables.pojos.Thesaurus> implements ThesaurusRepository {

    public JooqThesaurusRepository() {
        super(org.constellation.database.api.jooq.tables.pojos.Thesaurus.class, THESAURUS);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Integer create(Thesaurus thesaurus) {
        ThesaurusRecord record = dsl.newRecord(THESAURUS);
        record.setCreationDate(thesaurus.getCreationDate() != null ? thesaurus.getCreationDate().getTime() : null);
        record.setDefaultlang(thesaurus.getDefaultLang());
        record.setDescription(thesaurus.getDescription());
        record.setName(thesaurus.getName());
        record.setSchemaname(thesaurus.getSchemaName());
        record.setState(thesaurus.isState());
        record.setUri(thesaurus.getUri());
        record.setVersion(thesaurus.getVersion());
        record.store();

        updateLanguages(record.getId(), thesaurus.getLangs());

        return record.into(org.constellation.database.api.jooq.tables.pojos.Thesaurus.class).getId();
    }

    private void updateLanguages(int thesaurusID, List<String> language) {
        dsl.delete(THESAURUS_LANGUAGE).where(THESAURUS_LANGUAGE.THESAURUS_ID.eq(thesaurusID)).execute();
        if (language != null) {
            for (String lang : language) {
                ThesaurusLanguageRecord record = dsl.newRecord(THESAURUS_LANGUAGE);
                record.setThesaurusId(thesaurusID);
                record.setLanguage(lang);
                record.store();
            }
        }
    }

    private List<String> getLanguages(int thesaurusID) {
        return dsl.select(THESAURUS_LANGUAGE.LANGUAGE).from(THESAURUS_LANGUAGE)
                   .where(THESAURUS_LANGUAGE.THESAURUS_ID.eq(thesaurusID))
                   .fetchInto(String.class);
    }

    @Override
    public Thesaurus getByUri(String uri) {
        final Record one = dsl.select().from(THESAURUS).where(THESAURUS.URI.eq(uri)).fetchOne();
        if (one == null) return null;
        final Thesaurus th = convertToDto(one.into(org.constellation.database.api.jooq.tables.pojos.Thesaurus.class));
        th.setLangs(getLanguages(th.getId()));
        return th;
    }

    @Override
    public Thesaurus getByName(String name) {
        final Record one = dsl.select().from(THESAURUS).where(THESAURUS.NAME.eq(name)).fetchOne();
        if (one == null) return null;
        final Thesaurus th = convertToDto(one.into(org.constellation.database.api.jooq.tables.pojos.Thesaurus.class));
        th.setLangs(getLanguages(th.getId()));
        return th;
    }

    @Override
    public Thesaurus get(int id) {
        Record one = dsl.select().from(THESAURUS).where(THESAURUS.ID.eq(id)).fetchOne();
        if (one == null) return null;
        final Thesaurus th = convertToDto(one.into(org.constellation.database.api.jooq.tables.pojos.Thesaurus.class));
        th.setLangs(getLanguages(th.getId()));
        return th;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void update(Thesaurus thesaurus) {
        dsl.update(THESAURUS)
                .set(THESAURUS.CREATION_DATE, thesaurus.getCreationDate() != null ? thesaurus.getCreationDate().getTime() : null)
                .set(THESAURUS.DEFAULTLANG,   thesaurus.getDefaultLang())
                .set(THESAURUS.DESCRIPTION,   thesaurus.getDescription())
                .set(THESAURUS.NAME,          thesaurus.getName())
                .set(THESAURUS.SCHEMANAME,    thesaurus.getSchemaName())
                .set(THESAURUS.STATE,         thesaurus.isState())
                .set(THESAURUS.URI,           thesaurus.getUri())
                .set(THESAURUS.VERSION,       thesaurus.getVersion())
                .where(THESAURUS.ID.eq(thesaurus.getId())).execute();
        updateLanguages(thesaurus.getId(), thesaurus.getLangs());
    }

    @Override
    public List<Thesaurus> getAll() {
        final List<Thesaurus> ths = findAll();
        for (Thesaurus th : ths) {
            th.setLangs(getLanguages(th.getId()));
        }
        return ths;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(final int id) {
        dsl.delete(THESAURUS_LANGUAGE).where(THESAURUS_LANGUAGE.THESAURUS_ID.eq(id)).execute();
        return dsl.delete(THESAURUS).where(THESAURUS.ID.eq(id)).execute();
    }

    @Override
    public List<Thesaurus> getLinkedThesaurus(int id) {
        List<Integer> ids =  dsl.select(THESAURUS_X_SERVICE.THESAURUS_ID)
                                .from(THESAURUS_X_SERVICE)
                                .where(THESAURUS_X_SERVICE.SERVICE_ID.eq(id))
                                .fetchInto(Integer.class);
        final List<Thesaurus> results = new ArrayList<>();
        for (Integer thID : ids) {
            results.add(get(thID));
        }
        return results;
    }

    @Override
    public List<String> getLinkedThesaurusUri(int id) {
        return dsl.select(THESAURUS.URI)
                  .from(THESAURUS_X_SERVICE)
                  .where(THESAURUS_X_SERVICE.SERVICE_ID.eq(id))
                  .and(THESAURUS_X_SERVICE.THESAURUS_ID.eq(THESAURUS.ID))
                  .fetchInto(String.class);
    }


    public List<Thesaurus> findAll() {
        return convertListToDto(dsl.select().from(THESAURUS).fetchInto(org.constellation.database.api.jooq.tables.pojos.Thesaurus.class));
    }

    private List<Thesaurus> convertListToDto(List<org.constellation.database.api.jooq.tables.pojos.Thesaurus> daos) {
        List<Thesaurus> results = new ArrayList<>();
        for (org.constellation.database.api.jooq.tables.pojos.Thesaurus dao : daos) {
            results.add(convertToDto(dao));
        }
        return results;
    }
    public Thesaurus convertToDto(org.constellation.database.api.jooq.tables.pojos.Thesaurus dao) {
        if (dao != null) {
            return new Thesaurus(
                    dao.getId(),
                    dao.getUri(),
                    dao.getName(),
                    new Date(dao.getCreationDate()),
                    dao.getDescription(),
                    null,
                    dao.getDefaultlang(),
                    dao.getSchemaname(),
                    dao.getState(),
                    dao.getVersion());
        }
        return null;
    }

    public org.constellation.database.api.jooq.tables.pojos.Thesaurus convertToDao(Thesaurus dto) {
        if (dto != null) {
            return new org.constellation.database.api.jooq.tables.pojos.Thesaurus(
                    dto.getId(),
                    dto.getUri(),
                    dto.getName(),
                    dto.getDescription(),
                    dto.getCreationDate().getTime(),
                    false,
                    dto.getDefaultLang(),
                    dto.getVersion(),
                    dto.getSchemaName());
        }
        return null;
    }
}
