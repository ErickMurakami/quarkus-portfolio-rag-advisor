package dev.erickmurakami.portfolio.domain.models;

import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import dev.erickmurakami.portfolio.domain.enums.AccessScope;
import dev.erickmurakami.portfolio.domain.enums.DocumentSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_chunk")
public class DocumentChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "dev_id", nullable = false)
    private Long devId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentSource source;
    @Enumerated(EnumType.STRING)
    @Column(name = "access_scope", nullable = false, length = 20)
    private AccessScope accessScope;
    @Column(name = "source_reference", nullable = false, columnDefinition = "TEXT")
    private String sourceReference;
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1024)
    @Column(nullable = false)
    private float[] embedding;

    protected DocumentChunk() {
    }

    public DocumentChunk(Long devId, String content, DocumentSource source, AccessScope accessScope,
            String sourceReference) {
        this.devId = devId;
        this.content = content;
        this.source = source;
        this.accessScope = accessScope;
        this.sourceReference = sourceReference;
    }

    public Long getId() {
        return id;
    }

    public Long getDevId() {
        return devId;
    }

    public String getContent() {
        return content;
    }

    public DocumentSource getSource() {
        return source;
    }

    public AccessScope getAccessScope() {
        return accessScope;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        if (embedding == null || embedding.length != 1024) {
            throw new IllegalArgumentException("Embedding must have 1024 dimensions");
        }
        this.embedding = embedding.clone();
    }

}
