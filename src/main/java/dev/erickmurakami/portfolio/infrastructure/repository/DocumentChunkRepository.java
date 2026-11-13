package dev.erickmurakami.portfolio.infrastructure.repository;

import java.util.List;

import dev.erickmurakami.portfolio.domain.models.DocumentChunk;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DocumentChunkRepository implements PanacheRepository<DocumentChunk> {

    public long deleteBySource(Long devId, String sourceReference) {
        return delete("devId = ?1 and sourceReference = ?2", devId, sourceReference);
    }

    @Transactional
    public void replaceDocument(Long devId, String sourceReference, List<DocumentChunk> chunks) {
        deleteBySource(devId, sourceReference);
        persist(chunks);
        flush();
    }
}
