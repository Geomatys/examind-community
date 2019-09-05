/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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
import org.constellation.database.impl.AbstractJooqTestTestCase;
import org.constellation.database.impl.TestSamples;
import org.constellation.dto.metadata.Attachment;
import org.constellation.repository.AttachmentRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class JooqAttachmentRepositoryTestCase extends AbstractJooqTestTestCase {

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Test
    @Transactional()
    public void crude() {

        // no removeAll method
        List<Attachment> all = attachmentRepository.findAll();
        for (Attachment p : all) {
            attachmentRepository.delete(p.getId());
        }
        all = attachmentRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        int sid = attachmentRepository.create(TestSamples.newAttachment());
        Assert.assertNotNull(sid);

        Attachment s = attachmentRepository.findById(sid);
        Assert.assertNotNull(s);

        attachmentRepository.delete(s.getId());

        s = attachmentRepository.findById(s.getId());
        Assert.assertNull(s);
    }

}