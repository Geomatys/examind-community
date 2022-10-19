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
package org.constellation.thesaurus.component;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

import static org.apache.commons.lang3.StringUtils.isBlank;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.dto.thesaurus.Thesaurus;
import org.constellation.repository.ThesaurusRepository;
import org.constellation.thesaurus.api.IThesaurusBusiness;
import org.constellation.thesaurus.api.ThesaurusException;
import org.constellation.thesaurus.io.sql.ThesaurusDatabaseWriter;
import org.geotoolkit.skos.xml.SkosMarshallerPool;
import org.geotoolkit.thw.model.ISOLanguageCode;
import org.geotoolkit.thw.model.WriteableThesaurus;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to manage Thesaurus through
 * @author Quentin Boileau (Geomatys)
 */
@Service("thesaurusService")
@DependsOn("dataSource")
public class ThesaurusBusiness implements IThesaurusBusiness {

    protected static final Logger LOGGER = Logger.getLogger("org.constellation.thesaurus.component");

    /**
     * Geosud Datasource registered in geosud-ds context.
     */
    @Autowired
    @Qualifier("dataSource")
    protected DataSource dataSource;

    @Autowired
    protected ThesaurusRepository thesaurusRepository;

    /**
     * Return all loaded thesaurus.
     * @return Map where key is thesaurus URI and value Thesaurus DTO object
     */
    @Override
    public Map<String, Thesaurus> listThesaurus() {
        List<Thesaurus> thesaurus = thesaurusRepository.getAll();
        Map<String, Thesaurus> results = new HashMap<>();
        for (Thesaurus thw : thesaurus) {
            results.put(thw.getUri(), thw);
        }
        return results;
    }

    @Override
    public Optional<Thesaurus> getThesaurusByURI(String thesaurusURI) {
        return Optional.ofNullable(thesaurusRepository.getByUri(thesaurusURI));
    }

    @Override
    public Optional<String> getThesaurusURIByName(String thesaurusName) {
        final Thesaurus th = thesaurusRepository.getByName(thesaurusName);
        if (th != null) {
            return Optional.of(th.getUri());
        }
        return Optional.empty();
    }

    @Override
    public ThesaurusDatabaseWriter createThesaurusWriter(String thesaurusURI) throws ThesaurusException {
        final Thesaurus th = thesaurusRepository.getByUri(thesaurusURI);
        if (th != null) {
            return new ThesaurusDatabaseWriter(dataSource, th.getSchemaName(), false);
        }
        throw new ThesaurusException("Unknown thesaurus URI : " + thesaurusURI);
    }

    /**
     * Create new Thesaurus from DTO object.
     * @param thesaurus
     * @throws ThesaurusException
     */
    @Override
    @Transactional
    public ThesaurusDatabaseWriter createNewThesaurus(Thesaurus thesaurus) throws ThesaurusException {
        // Generate URI if needed.
        if (isBlank(thesaurus.getUri())) {
            thesaurus.setUri(UUID.randomUUID().toString());
        }

        if (thesaurus.getSchemaName() == null) {
            thesaurus.setSchemaName(generateSchemaName(thesaurus.getName()));
        }

        // Set creation date.
        thesaurus.setCreationDate(new Date());

        // Parse languages codes.
        ISOLanguageCode defaultLang = null;
        List<ISOLanguageCode> languages = new ArrayList<>();
        if (thesaurus.getDefaultLang() == null) {
            if (thesaurus.getLangs() != null && !thesaurus.getLangs().isEmpty()) {
                defaultLang = ISOLanguageCode.fromCode(thesaurus.getLangs().get(0));
            }
        } else {
            defaultLang = ISOLanguageCode.fromCode(thesaurus.getDefaultLang());
        }

        if (thesaurus.getLangs() == null || thesaurus.getLangs().isEmpty()) {
            if (defaultLang != null) {
                languages.add(defaultLang);
            }
        } else {
            languages = Lists.transform(thesaurus.getLangs(), new Function<String, ISOLanguageCode>() {
                @Override
                public ISOLanguageCode apply(String langCode) {
                    return ISOLanguageCode.fromCode(langCode);
                }
            });
        }

        // Create the thesaurus instance.
        ThesaurusDatabaseWriter thesaurusW = new ThesaurusDatabaseWriter(dataSource,
                thesaurus.getSchemaName(),
                false,
                thesaurus.getUri(),
                thesaurus.getName(),
                thesaurus.getDescription(),
                languages,
                defaultLang);
        try {
            thesaurusW.store();
            thesaurusRepository.create(thesaurus);
            return thesaurusW;
        } catch (SQLException ex) {
            throw new ThesaurusException("SQL exception while storing new thesaurus", ex);
        } catch (IOException ex) {
            throw new ThesaurusException("IO exception while storing new thesaurus", ex);
        }
    }


    @Override
    @Transactional
    public void deleteThesaurus(String thesaurusURI) throws ThesaurusException {
        try (WriteableThesaurus thesaurus = createThesaurusWriter(thesaurusURI)) {
            thesaurus.delete();
            final Thesaurus th = thesaurusRepository.getByUri(thesaurusURI);
            thesaurusRepository.delete(th.getId());
        } catch (SQLException e) {
            throw new ThesaurusException(e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    //  Private utility methods
    // -------------------------------------------------------------------------

    /**
     * Generate thesaurus database schema
     * @param thesaurusName
     * @return
     */
    private static String generateSchemaName(String thesaurusName) {
        return "th_" + thesaurusName.replaceAll("\\W", "").toLowerCase();
    }

    private static Function<ISOLanguageCode, String> ISO_LANG_TO_CODE = new Function<ISOLanguageCode, String>() {
        @Override
        public String apply(ISOLanguageCode isoLanguage) {
            return isoLanguage.getTwoLetterCode().toLowerCase();
        }
    };

    @Override
    public MarshallerPool getSkosMarshallerPool() {
        return SkosMarshallerPool.getInstance();
    }
}

