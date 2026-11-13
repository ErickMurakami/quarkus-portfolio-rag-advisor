
package dev.erickmurakami.portfolio.application.services;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.erickmurakami.portfolio.domain.enums.AccessScope;
import dev.erickmurakami.portfolio.domain.enums.DocumentSource;
import dev.erickmurakami.portfolio.domain.models.DocumentChunk;
import dev.erickmurakami.portfolio.infrastructure.github.ReadmeContent;

class DocumentChunkingServiceTest {

    private final DocumentChunkingService service = new DocumentChunkingService();

    private static final Long DEV_ID = 1L;

    @Test
    void shouldSplitMarkdownByHeadings() {
        String markdown = """
                # Econolyze
                Personal finance platform.

                ## Architecture
                Built using Clean Architecture.

                ## Security
                Authentication with JWT.
                """;

        List<DocumentChunk> chunks = service.chunkReadme(
                DEV_ID,
                readme(markdown, AccessScope.PUBLIC));

        assertEquals(3, chunks.size());
        assertTrue(chunks.get(0).content().startsWith("# Econolyze"));
        assertTrue(chunks.get(1).content().startsWith("## Architecture"));
        assertTrue(chunks.get(2).content().startsWith("## Security"));
    }

    @Test
    void shouldSplitLongSectionsWithOverlap() {
        String markdown = "# Architecture\n" + "Java Quarkus ".repeat(250);

        List<DocumentChunk> chunks = service.chunkReadme(
                DEV_ID,
                readme(markdown, AccessScope.PUBLIC));

        assertTrue(chunks.size() > 1);

        for (DocumentChunk chunk : chunks) {
            assertTrue(chunk.content().length() <= 1000);
            assertFalse(chunk.content().isBlank());
        }

        String first = chunks.get(0).content();
        String second = chunks.get(1).content();

        // The second chunk must begin with text
        // already present in the first chunk.
        String overlappingText = second.substring(
                0,
                Math.min(50, second.length()));

        assertTrue(
                first.contains(overlappingText),
                "Consecutive chunks should share overlapping text");
    }

    @Test
    void shouldPreservePrivateAccessScope() {
        List<DocumentChunk> chunks = service.chunkReadme(
                DEV_ID,
                readme("# Internal project", AccessScope.PRIVATE));

        assertEquals(1, chunks.size());
        assertEquals(
                AccessScope.PRIVATE,
                chunks.getFirst().accessScope());
        assertEquals(
                DocumentSource.GITHUB,
                chunks.getFirst().source());
        assertEquals(DEV_ID, chunks.getFirst().devId());
        assertNull(chunks.getFirst().id());
    }

    @Test
    void shouldPreserveSourceReference() {
        List<DocumentChunk> chunks = service.chunkReadme(
                DEV_ID,
                readme("# Econolyze", AccessScope.PUBLIC));

        assertEquals(
                "https://github.com/ErickMurakami/Econolyze",
                chunks.getFirst().sourceReference());
    }

    @Test
    void shouldReturnEmptyListForBlankReadme() {
        List<DocumentChunk> chunks = service.chunkReadme(
                DEV_ID,
                readme("   ", AccessScope.PUBLIC));

        assertTrue(chunks.isEmpty());
    }

    @Test
    void shouldRejectMissingDeveloperId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.chunkReadme(
                        null,
                        readme("# Econolyze", AccessScope.PUBLIC)));
    }

    private ReadmeContent readme(
            String content,
            AccessScope scope) {
        return new ReadmeContent(
                "Econolyze",
                content,
                scope,
                "https://github.com/ErickMurakami/Econolyze");
    }
}
