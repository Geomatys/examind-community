/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package com.examind.repository.filesystem;

import static com.examind.repository.filesystem.FileSystemUtilities.*;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import org.constellation.dto.metadata.Attachment;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.AttachmentRepository;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class FileSystemAttachmentRepository extends AbstractFileSystemRepository implements AttachmentRepository {

    private final Map<Integer, Attachment> byId = new HashMap<>();
    private final Map<String, List<Attachment>> byFileName = new HashMap<>();

    public FileSystemAttachmentRepository() {
        super(Attachment.class);
        load();
    }

    private void load() {
        try {
            Path attachmentDir = getDirectory(ATTACHMENT_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(attachmentDir)) {
                for (Path attachmentFile : directoryStream) {
                    Attachment attachment = (Attachment) getObjectFromPath(attachmentFile, pool);
                    byId.put(attachment.getId(), attachment);

                    if (attachment.getFilename() != null) {
                        if (!byFileName.containsKey(attachment.getFilename())) {
                            List<Attachment> atts = new ArrayList<>();
                            atts.add(attachment);
                            byFileName.put(attachment.getFilename(), atts);
                        } else {
                            byFileName.get(attachment.getFilename()).add(attachment);
                        }
                    }
                    incCurrentId(attachment);
                }
            }

        } catch (IOException | JAXBException ex) {
           LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Attachment findById(int id) {
        return byId.get(id);
    }

    @Override
    public boolean existsById(int id) {
        return byId.containsKey(id);
    }

    @Override
    public List<Attachment> findByFileName(String fileName) {
        if (byFileName.containsKey(fileName)) {
            return byFileName.get(fileName);
        }
        return new ArrayList<>();
    }

    @Override
    public List<Attachment> findAll() {
        return new ArrayList<>(byId.values());
    }

    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public int create(Attachment att) {
        if (att != null) {
            final int id = assignCurrentId(att);

            Path attachmentDir = getDirectory(ATTACHMENT_DIR);
            Path attachmentFile = attachmentDir.resolve(id + ".xml");
            writeObjectInPath(att, attachmentFile, pool);

            byId.put(att.getId(), att);

            if (att.getFilename() != null) {
                if (!byFileName.containsKey(att.getFilename())) {
                    List<Attachment> atts = new ArrayList<>();
                    atts.add(att);
                    byFileName.put(att.getFilename(), atts);
                } else {
                    byFileName.get(att.getFilename()).add(att);
                }
            }
            return att.getId();
        }
        return -1;
    }

    @Override
    public void update(Attachment att) {
        if (byId.containsKey(att.getId())) {

            Attachment previous = byId.get(att.getId());
            byFileName.get(previous.getFilename()).remove(att);


            Attachment old = byId.get(att.getId());
            if (old.getFilename() != null) {
                if (byFileName.containsKey(old.getFilename())) {
                    byFileName.get(old.getFilename()).remove(old);
                }
            }

            Path attachmentDir = getDirectory(ATTACHMENT_DIR);
            Path attachmentFile = attachmentDir.resolve(att.getId() + ".xml");
            writeObjectInPath(att, attachmentFile, pool);

            byId.put(att.getId(), att);
            if (att.getFilename() != null) {
                if (!byFileName.containsKey(att.getFilename())) {
                    List<Attachment> atts = new ArrayList<>();
                    atts.add(att);
                    byFileName.put(att.getFilename(), atts);
                } else {
                    byFileName.get(att.getFilename()).add(att);
                }
            }
        }
    }

    @Override
    public int delete(int id) {
        if (byId.containsKey(id)) {

            Attachment att = byId.get(id);
            Path attachmentDir = getDirectory(ATTACHMENT_DIR);
            Path attachmentFile = attachmentDir.resolve(att.getId() + ".xml");
            try {
                Files.delete(attachmentFile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
            }

            byId.remove(att.getId());
            if (att.getFilename() != null) {
                if (byFileName.containsKey(att.getFilename())) {
                    byFileName.get(att.getFilename()).remove(att);
                }
            }
            return 1;
        }
        return 0;
    }

    ////--------------------------------------------------------------------///
    ////------------------------    LINKED         -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public List<Attachment> getLinkedAttachment(int metadataID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteForMetadata(int metadataId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void linkMetadataAndAttachment(int metadataID, int attId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void unlinkMetadataAndAttachment(int metadataID, int attId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
