package dev.erickmurakami.portfolio.application.services;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import dev.erickmurakami.portfolio.domain.enums.DocumentSource;
import dev.erickmurakami.portfolio.domain.models.DocumentChunk;
import dev.erickmurakami.portfolio.infrastructure.github.ReadmeContent;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DocumentChunkingService {

    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 150;

    private static final Pattern MD_HEADING = Pattern.compile("(?=^#{1,6}[ \\t]+)", Pattern.MULTILINE);

    public List<DocumentChunk> chunkReadme(Long devId, ReadmeContent readme) {
        if (devId == null) {
            throw new IllegalArgumentException("Developer ID cannot be null");
        }
        if (readme == null || readme.content().isBlank()) {
            return List.of();
        }

        List<DocumentChunk> chunks = new ArrayList<>();
        String markdown = readme.content().replace("\r\n", "\n").replace('\r', '\n');

        String[] sections = MD_HEADING.split(markdown);

        for (String section : sections) {
            if (section.isBlank()) {
                continue;
            }

            for (String content : splitSection(section.strip())) {
                chunks.add(new DocumentChunk(
                        devId,
                        content,
                        DocumentSource.GITHUB,
                        readme.accessScope(),
                        readme.sourceReference()));
            }
        }
        return List.copyOf(chunks);
    }

    public List<String> splitSection(String section) {
        if (section.length() <= CHUNK_SIZE) {
            return List.of(section);
        }

        List<String> parts = new ArrayList<>();

        int start = 0;

        while (start < section.length()) {
            int end = Math.min(start + CHUNK_SIZE, section.length());

            if (end < section.length()) {
                int boundary = findBoundary(section, start, end);

                if (boundary > start) {
                    end = boundary;
                }
            }
            String chunk = section.substring(start, end).strip();

            if (!chunk.isEmpty()) {
                parts.add(chunk);
            }

            if (end == section.length()) {
                break;
            }
            start = Math.max(start + 1, end - CHUNK_OVERLAP);
        }
        return parts;
    }

    public int findBoundary(String text, int start, int end) {
        int minimun = start + CHUNK_SIZE / 2;

        for (int i = end - 1; i >= minimun; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i + 1;
            }
        }
        return end;

    }
}
