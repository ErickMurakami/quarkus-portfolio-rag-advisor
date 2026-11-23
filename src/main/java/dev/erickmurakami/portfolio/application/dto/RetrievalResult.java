package dev.erickmurakami.portfolio.application.dto;

import java.net.URI;

public record RetrievalResult(
        String repo,
        String content,
        double similarity,
        String sourceReference) {
    public static RetrievalResult toResult(RetrievedChunk c) {
        return new RetrievalResult(
                repoName(c.sourceReference()),
                c.content(),
                c.similarity(),
                c.sourceReference());
    }

    private static String repoName(String ref) {
        try {
            String[] parts = URI.create(ref).getPath().split("/");

            if (parts.length >= 3) {
                return parts[2];
            }
        } catch (IllegalArgumentException ex) {

        }

        return ref;
    }
}
