package org.constellation.database.impl.repository;

import java.util.ArrayList;
import java.util.List;
import org.constellation.dto.metadata.Attachment;
import com.examind.database.api.jooq.tables.records.AttachmentRecord;
import com.examind.database.api.jooq.tables.records.MetadataXAttachmentRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.examind.database.api.jooq.Tables.ATTACHMENT;
import static com.examind.database.api.jooq.Tables.METADATA_X_ATTACHMENT;
import org.constellation.repository.AttachmentRepository;
import org.springframework.context.annotation.DependsOn;

@Component
@DependsOn("database-initer")
public class JooqAttachmentRepository extends AbstractJooqRespository<AttachmentRecord, com.examind.database.api.jooq.tables.pojos.Attachment> implements AttachmentRepository {

    public JooqAttachmentRepository() {
        super(com.examind.database.api.jooq.tables.pojos.Attachment.class, ATTACHMENT);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int create(Attachment att) {
        AttachmentRecord attRecord = dsl.newRecord(ATTACHMENT);
        attRecord.setContent(att.getContent());
        attRecord.setUri(att.getUri());
        attRecord.setFilename(att.getFilename());
        attRecord.store();
        return attRecord.getId();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void update(Attachment a) {
        dsl.update(ATTACHMENT)
                .set(ATTACHMENT.CONTENT, a.getContent())
                .set(ATTACHMENT.URI, a.getUri())
                .set(ATTACHMENT.FILENAME, a.getFilename())
                .where(ATTACHMENT.ID.eq(a.getId())).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(Integer id) {
        dsl.delete(METADATA_X_ATTACHMENT).where(METADATA_X_ATTACHMENT.ATTACHEMENT_ID.eq(id)).execute();
        return dsl.delete(ATTACHMENT).where(ATTACHMENT.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteAll() {
        dsl.delete(METADATA_X_ATTACHMENT).execute();
        return dsl.delete(ATTACHMENT).execute();
    }

    @Override
    public Attachment findById(int id) {
        return convertToAttachmentDto(dsl.select()
                                         .from(ATTACHMENT)
                                         .where(ATTACHMENT.ID.eq(id))
                                         .fetchOneInto(com.examind.database.api.jooq.tables.pojos.Attachment.class));
    }

    @Override
    public List<Attachment> findByFileName(String fileName) {
       return convertToAttachmentDtos(
        dsl.select()
           .from(ATTACHMENT)
           .where(ATTACHMENT.FILENAME.eq(fileName))
           .fetchInto(com.examind.database.api.jooq.tables.pojos.Attachment.class));

    }

    @Override
    public List<Attachment> findAll() {
        return convertToAttachmentDtos(
        dsl.select()
           .from(ATTACHMENT)
           .fetchInto(com.examind.database.api.jooq.tables.pojos.Attachment.class));
    }

    @Override
    public boolean existsById(Integer styleId) {
        return dsl.selectCount().from(ATTACHMENT)
                .where(ATTACHMENT.ID.eq(styleId))
                .fetchOne(0, Integer.class) > 0;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void linkMetadataAndAttachment(int metadataID, int attId) {
        MetadataXAttachmentRecord record = dsl.newRecord(METADATA_X_ATTACHMENT);
        record.setAttachementId(attId);
        record.setMetadataId(metadataID);
        record.store();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void unlinkMetadataAndAttachment(int metadataID, int attId) {
        dsl.delete(METADATA_X_ATTACHMENT)
           .where(METADATA_X_ATTACHMENT.ATTACHEMENT_ID.eq(attId))
           .and(METADATA_X_ATTACHMENT.METADATA_ID.eq(metadataID))
           .execute();
    }

    @Override
    public List<Attachment> getLinkedAttachment(int metadataID) {
        return dsl.select(ATTACHMENT.fields())
           .from(ATTACHMENT, METADATA_X_ATTACHMENT)
           .where(ATTACHMENT.ID.eq(METADATA_X_ATTACHMENT.ATTACHEMENT_ID))
           .and(METADATA_X_ATTACHMENT.METADATA_ID.eq(metadataID)).fetchInto(Attachment.class);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteForMetadata(int metadataId) {
        List<Integer> attIds = dsl.select(METADATA_X_ATTACHMENT.ATTACHEMENT_ID)
                                  .from(METADATA_X_ATTACHMENT)
                                  .where(METADATA_X_ATTACHMENT.METADATA_ID.eq(metadataId))
                                  .fetchInto(Integer.class);
        dsl.delete(METADATA_X_ATTACHMENT).where(METADATA_X_ATTACHMENT.METADATA_ID.eq(metadataId)).execute();
        for (Integer attId : attIds) {
            dsl.delete(ATTACHMENT).where(ATTACHMENT.ID.eq(attId)).execute();
        }
    }

    private org.constellation.dto.metadata.Attachment convertToAttachmentDto(final com.examind.database.api.jooq.tables.pojos.Attachment md) {
        if (md != null) {
            return new org.constellation.dto.metadata.Attachment(md.getId(), md.getContent(), md.getUri(), md.getFilename());
        }
        return null;
    }

    private List<org.constellation.dto.metadata.Attachment> convertToAttachmentDtos(final List<com.examind.database.api.jooq.tables.pojos.Attachment> mds) {
        final List<org.constellation.dto.metadata.Attachment> results = new ArrayList<>();
        for (com.examind.database.api.jooq.tables.pojos.Attachment md: mds) {
            results.add(convertToAttachmentDto(md));
        }
        return results;
    }
}
