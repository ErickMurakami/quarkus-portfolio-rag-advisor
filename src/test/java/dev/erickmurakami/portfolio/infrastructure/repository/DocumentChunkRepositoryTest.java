package dev.erickmurakami.portfolio.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import dev.erickmurakami.portfolio.application.dto.RetrievedChunk;
import dev.erickmurakami.portfolio.domain.enums.AccessScope;
import dev.erickmurakami.portfolio.domain.enums.RetrievalMode;

class DocumentChunkRepositoryTest {
    private static PostgreSQLContainer postgres;
    private static PGSimpleDataSource dataSource;
    private final DocumentChunkRepository repository = new DocumentChunkRepository();

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker unavailable: real PostgreSQL/pgvector queries were not executed");
        if (postgres == null) {
            postgres = new PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg17")
                    .asCompatibleSubstituteFor("postgres"));
            postgres.start();
            dataSource = new PGSimpleDataSource();
            dataSource.setURL(postgres.getJdbcUrl());
            dataSource.setUser(postgres.getUsername());
            dataSource.setPassword(postgres.getPassword());
            try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                // Apply the production schema inside the disposable database only.
                for (String migration : List.of("V1__init.sql", "V2__document_embeddings.sql", "V3__ingestion_job.sql")) {
                    try (var input = getClass().getResourceAsStream("/db/migration/" + migration)) {
                        assertNotNull(input, migration);
                        statement.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
                    }
                }
            }
        }
        repository.dataSource = dataSource;
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("TRUNCATE dev_profile RESTART IDENTITY CASCADE");
            statement.execute("INSERT INTO dev_profile (id,full_name,github_username) VALUES "
                    + "(1,'Synthetic One','synthetic-one'),(2,'Synthetic Two','synthetic-two')");
        }
    }

    @AfterAll
    static void stopDatabase() {
        if (postgres != null) postgres.stop();
    }

    @Test
    void shouldReturnTopNFocusedIncludingBelowOverviewThreshold() throws Exception {
        insert(1, 1, "a", AccessScope.PUBLIC, 0.9);
        insert(2, 1, "a", AccessScope.PUBLIC, 0.8);
        insert(3, 1, "a", AccessScope.PRIVATE, 0.7);
        insert(4, 1, "b", AccessScope.PUBLIC, 0.64);
        insert(5, 1, "b", AccessScope.PUBLIC, 0.3);
        assertEquals(List.of(1L, 2L, 3L, 4L), ids(search(false, RetrievalMode.FOCUSED, 4)));
        assertEquals(0.64, search(false, RetrievalMode.FOCUSED, 4).getLast().similarity(), 0.00001);
    }

    @Test
    void shouldLimitOverviewToTwoChunksPerSourceAndApplyThreshold() throws Exception {
        insert(1, 1, "a", AccessScope.PUBLIC, 0.99);
        insert(2, 1, "a", AccessScope.PUBLIC, 0.98);
        insert(3, 1, "a", AccessScope.PUBLIC, 0.97);
        insert(4, 1, "b", AccessScope.PUBLIC, 0.8);
        insert(5, 1, "b", AccessScope.PUBLIC, 0.6501);
        insert(6, 1, "c", AccessScope.PUBLIC, 0.6499);
        insert(7, 1, "d", AccessScope.PUBLIC, 0.2);
        var results = search(false, RetrievalMode.OVERVIEW, 20);
        assertEquals(List.of(1L, 2L, 4L, 5L), ids(results));
        assertTrue(results.stream().allMatch(chunk -> chunk.similarity() >= 0.65));
        assertEquals(List.of(1L, 2L, 4L), ids(search(false, RetrievalMode.OVERVIEW, 3)));
    }

    @ParameterizedTest
    @EnumSource(RetrievalMode.class)
    void shouldFilterPrivateAndOtherDevelopersBeforeRankingAndLimit(RetrievalMode mode) throws Exception {
        insert(1, 2, "shared", AccessScope.PUBLIC, 1);
        insert(2, 1, "shared", AccessScope.PRIVATE, 0.99);
        insert(3, 1, "shared", AccessScope.PRIVATE, 0.98);
        insert(4, 1, "shared", AccessScope.PUBLIC, 0.9);
        insert(5, 1, "shared", AccessScope.PUBLIC, 0.8);
        assertEquals(List.of(4L, 5L), ids(search(true, mode, 2)));
        assertTrue(search(true, mode, 20).stream().allMatch(c -> c.accessScope() == AccessScope.PUBLIC));
        assertEquals(List.of(2L, 3L), ids(search(false, mode, 2)));
        assertFalse(ids(search(false, mode, 20)).contains(1L));
    }

    @ParameterizedTest
    @EnumSource(RetrievalMode.class)
    void shouldBreakTiesByIdIncludingPerSourceRanking(RetrievalMode mode) throws Exception {
        insert(30, 1, "a", AccessScope.PUBLIC, 0.9);
        insert(10, 1, "a", AccessScope.PUBLIC, 0.9);
        insert(20, 1, "a", AccessScope.PUBLIC, 0.9);
        insert(5, 1, "b", AccessScope.PUBLIC, 0.9);
        var expected = mode == RetrievalMode.OVERVIEW ? List.of(5L, 10L, 20L) : List.of(5L, 10L, 20L, 30L);
        assertEquals(expected, ids(search(false, mode, 20)));
        assertEquals(expected, ids(search(false, mode, 20)));
    }

    private List<RetrievedChunk> search(boolean publicOnly, RetrievalMode mode, int limit) {
        return repository.searchSimilar(1L, vector(1), limit, publicOnly, mode);
    }

    private List<Long> ids(List<RetrievedChunk> chunks) {
        return chunks.stream().map(RetrievedChunk::id).toList();
    }

    private static float[] vector(double similarity) {
        float[] vector = new float[1024];
        vector[0] = (float) similarity;
        vector[1] = (float) Math.sqrt(1 - similarity * similarity);
        return vector;
    }

    private void insert(long id, long devId, String source, AccessScope scope, double similarity) throws Exception {
        String embedding = Arrays.toString(vector(similarity));
        try (Connection connection = dataSource.getConnection();
                var statement = connection.prepareStatement("""
                    INSERT INTO document_chunk(id,dev_id,content,source,access_scope,source_reference,embedding)
                    VALUES (?,?,'Synthetic evidence','GITHUB',?,?,CAST(? AS vector))
                    """)) {
            statement.setLong(1, id);
            statement.setLong(2, devId);
            statement.setString(3, scope.name());
            statement.setString(4, source);
            statement.setString(5, embedding);
            statement.executeUpdate();
        }
    }
}
