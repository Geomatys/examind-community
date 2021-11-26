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
package com.examind.repository;

import java.util.List;
import org.constellation.dto.CstlUser;
import org.constellation.dto.metadata.Attachment;
import org.constellation.dto.metadata.Metadata;
import org.constellation.repository.AttachmentRepository;
import org.constellation.repository.MetadataRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class AttachmentRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private MetadataRepository metadataRepository;

    public void crude() {

        metadataRepository.deleteAll();
        attachmentRepository.deleteAll();

        List<Attachment> all = attachmentRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        List<Metadata> metas = metadataRepository.findAll();
        Assert.assertTrue(metas.isEmpty());
        
        CstlUser owner = getOrCreateUser();
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        int mid = metadataRepository.create(TestSamples.newMetadata(owner.getId(), "meta-1", null, null, null));

        int sid1 = attachmentRepository.create(TestSamples.newAttachment());
        Attachment s = attachmentRepository.findById(sid1);
        Assert.assertNotNull(s);

        List<Attachment> atts = attachmentRepository.findByFileName("fnmae");
        Assert.assertNotNull(atts);
        Assert.assertFalse(atts.isEmpty());
        Assert.assertEquals(1, atts.size());


        attachmentRepository.linkMetadataAndAttachment(mid, sid1);
        atts = attachmentRepository.getLinkedAttachment(mid);
        Assert.assertNotNull(atts);
        Assert.assertFalse(atts.isEmpty());
        Assert.assertEquals(1, atts.size());


        int sid2 = attachmentRepository.create(TestSamples.newAttachmentQuote());
        s = attachmentRepository.findById(sid2);
        Assert.assertNotNull(s);

        atts = attachmentRepository.findByFileName("fn'mae");
        Assert.assertNotNull(atts);
        Assert.assertFalse(atts.isEmpty());
        Assert.assertEquals(1, atts.size());

        attachmentRepository.linkMetadataAndAttachment(mid, sid2);
        atts = attachmentRepository.getLinkedAttachment(mid);
        Assert.assertNotNull(atts);
        Assert.assertEquals(2, atts.size());

        attachmentRepository.delete(sid1);
        s = attachmentRepository.findById(sid1);
        Assert.assertNull(s);

        atts = attachmentRepository.getLinkedAttachment(mid);
        Assert.assertNotNull(atts);
        Assert.assertEquals(1, atts.size());

        attachmentRepository.delete(sid2);
        s = attachmentRepository.findById(sid2);
        Assert.assertNull(s);
        
        metadataRepository.deleteAll();
    }

}