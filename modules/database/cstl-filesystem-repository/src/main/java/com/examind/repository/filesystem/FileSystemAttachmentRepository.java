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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import org.constellation.dto.StringList;
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

    private final Map<Integer, List<Attachment>> linkedAttachment = new HashMap<>();

    public FileSystemAttachmentRepository() {
        super(Attachment.class, StringList.class);
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

            Path metadataAttDir = getDirectory(METADATA_X_ATTACHMENT_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(metadataAttDir)) {
                for (Path metadataAttFile : directoryStream) {
                    StringList attList = (StringList) getObjectFromPath(metadataAttFile, pool);
                    String fileName = metadataAttFile.getFileName().toString();
                    Integer metadataId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));
                    List<Attachment> linked = new ArrayList<>();
                    for (Integer linkedAttachmentId : getIntegerList(attList)) {
                        linked.add(byId.get(linkedAttachmentId));
                    }
                    linkedAttachment.put(metadataId, linked);
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
    public boolean existsById(Integer id) {
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
    public int delete(Integer id) {
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

            for (List<Attachment> atts : linkedAttachment.values()) {
                for (Attachment a : atts) {
                    if (a.getId() == id) {
                        atts.remove(a);
                        break;
                    }
                }
            }
            return 1;
        }
        return 0;
    }

    @Override
    public int deleteAll() {
        int i = 0;
        for (Integer id : byId.keySet()) {
            i = i + delete(id);
        }
        return i;
    }

    ////--------------------------------------------------------------------///
    ////------------------------    LINKED         -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public List<Attachment> getLinkedAttachment(int metadataID) {
        if (linkedAttachment.containsKey(metadataID)) {
            return linkedAttachment.get(metadataID);
        }
        return new ArrayList<>();
    }

    @Override
    public void deleteForMetadata(int metadataId) {
        List<Attachment> atts = getLinkedAttachment(metadataId);
        for (Attachment att : atts) {
            delete(att.getId());
        }
    }

    @Override
    public void linkMetadataAndAttachment(int metadataID, int attId) {
      

        Path metadataAttDir = getDirectory(METADATA_X_ATTACHMENT_DIR);
        boolean found = false;
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(metadataAttDir)) {
            for (Path metaAttFile : directoryStream) {
                String fileName = metaAttFile.getFileName().toString();
                Integer currentMetadataId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                // update file
                if (currentMetadataId == metadataID) {
                    found = true;
                    StringList attList = (StringList) getObjectFromPath(metaAttFile, pool);
                    List<Integer> attIds = getIntegerList(attList);
                    if (!attIds.contains(attId)) {
                        attIds.add(attId);

                        // update fs
                        writeObjectInPath(attList, metaAttFile, pool);

                        // update memory
                        List<Attachment> atts = linkedAttachment.get(metadataID);
                        atts.add(byId.get(attId));
                    }
                }
            }

            // create new file
            if (!found) {
                // update fs
                StringList dataList = new StringList(Arrays.asList(attId + ""));
                Path dataDataFile = metadataAttDir.resolve(metadataID + ".xml");
                writeObjectInPath(dataList, dataDataFile, pool);

                // update memory
                List<Attachment> atts = new ArrayList<>();
                atts.add(byId.get(attId));
                linkedAttachment.put(metadataID, atts);
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void unlinkMetadataAndAttachment(int metadataID, int attId) {
        Path dataDataDir = getDirectory(METADATA_X_ATTACHMENT_DIR);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dataDataDir)) {
            for (Path metaAttFile : directoryStream) {
                String fileName = metaAttFile.getFileName().toString();
                Integer currentmetadataId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                // update file
                if (currentmetadataId == metadataID) {
                    StringList attList = (StringList) getObjectFromPath(metaAttFile, pool);
                    List<Integer> attIds = getIntegerList(attList);
                    if (attIds.contains(attId)) {
                        attIds.remove(attId);

                        // update fs
                        writeObjectInPath(attList, metaAttFile, pool);

                        // update memory
                        List<Attachment> atts = linkedAttachment.get(metadataID);
                        atts.remove(byId.get(attId));
                    }
                }
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.WARNING, "Error while unlinking style and data", ex);
        }
    }
}
