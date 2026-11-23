package dev.erickmurakami.portfolio.infrastructure.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import dev.erickmurakami.portfolio.application.dto.RetrievedChunk;
import dev.erickmurakami.portfolio.domain.enums.AccessScope;
import dev.erickmurakami.portfolio.domain.enums.RetrievalMode;
import dev.erickmurakami.portfolio.domain.models.DocumentChunk;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DocumentChunkRepository implements PanacheRepository<DocumentChunk> {

    @Inject
    DataSource dataSource;

    public long deleteBySource(Long devId, String sourceReference) {
        return delete("devId = ?1 and sourceReference = ?2", devId, sourceReference);
    }

    @Transactional
    public void replaceDocument(Long devId, String sourceReference, List<DocumentChunk> chunks) {
        deleteBySource(devId, sourceReference);
        persist(chunks);
        flush();
    }

    public List<RetrievedChunk> searchSimilar(
            Long devId, float[] queryVector, int limit) {

        return searchSimilar(
                devId, queryVector, limit, false, RetrievalMode.FOCUSED);
    }

    public List<RetrievedChunk> searchSimilar(
            Long devId,
            float[] queryVector,
            int limit,
            boolean publicOnly) {

        return searchSimilar(
                devId, queryVector, limit, publicOnly,
                RetrievalMode.FOCUSED);
    }

    public List<RetrievedChunk> searchSimilar(
            Long devId,
            float[] queryVector,
            int limit,
            boolean publicOnly,
            RetrievalMode mode) {

        if (devId == null) {
            throw new IllegalArgumentException(
                    "Developer ID cannot be null");
        }

        if (mode == null) {
            throw new IllegalArgumentException(
                    "Retrieval mode cannot be null");
        }

        if (queryVector == null || queryVector.length != 1024) {
            throw new IllegalArgumentException(
                    "Query embedding must have 1024 dimensions");
        }

        if (limit < 1 || limit > 20) {
            throw new IllegalArgumentException(
                    "Limit must be between 1 and 20");
        }

        String vector = IntStream.range(0, queryVector.length)
                .mapToObj(i -> {
                    float value = queryVector[i];

                    if (!Float.isFinite(value)) {
                        throw new IllegalArgumentException(
                                "Embedding contains non-finite values");
                    }

                    return Float.toString(value);
                })
                .collect(Collectors.joining(",", "[", "]"));

        String baseSql = """
                WITH scored AS (
                    SELECT
                        id,
                        content,
                        source_reference,
                        access_scope,
                        1 - (embedding <=> CAST(? AS vector))
                            AS similarity
                    FROM document_chunk
                    WHERE dev_id = ?
                """ +
                (publicOnly
                        ? " AND access_scope = 'PUBLIC'\n"
                        : "")
                +
                """
                        )
                        """;

        String focusedSql = """
                SELECT
                    id, content, source_reference,
                    access_scope, similarity
                FROM scored
                ORDER BY similarity DESC, id
                LIMIT ?
                """;

        String overviewSql = """
                , ranked AS (
                    SELECT
                        *,
                        ROW_NUMBER() OVER (
                            PARTITION BY source_reference
                            ORDER BY similarity DESC, id
                        ) AS repository_rank
                    FROM scored
                )
                SELECT
                    id, content, source_reference,
                    access_scope, similarity
                FROM ranked
                WHERE repository_rank <= 2
                ORDER BY similarity DESC, id
                LIMIT ?
                """;

        String sql = baseSql +
                (mode == RetrievalMode.OVERVIEW
                        ? overviewSql
                        : focusedSql);

        List<RetrievedChunk> results = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, vector);
            statement.setLong(2, devId);
            statement.setInt(3, limit);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    results.add(new RetrievedChunk(
                            rs.getLong("id"),
                            rs.getString("content"),
                            rs.getString("source_reference"),
                            AccessScope.valueOf(
                                    rs.getString("access_scope")),
                            rs.getDouble("similarity")));
                }
            }

        } catch (SQLException ex) {
            throw new IllegalStateException(
                    "Failed to search document embeddings", ex);
        }

        return List.copyOf(results);
    }

}
