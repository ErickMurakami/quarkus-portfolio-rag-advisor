package dev.erickmurakami.portfolio.interfaces.rest;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class PortfolioPageTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        // HTTP/template checks need neither a database nor background ingestion.
        // Non-empty synthetic values allow CDI startup without developer credentials.
        return Map.of(
                "gemini.api-key", "test-only-gemini-key",
                "github.token", "test-only-github-token",
                "quarkus.langchain4j.openai.api-key", "test-only-groq-key",
                "quarkus.datasource.jdbc.initial-size", "0",
                "quarkus.datasource.jdbc.min-size", "0",
                "quarkus.datasource.devservices.enabled", "false",
                "quarkus.hibernate-orm.active", "false",
                "quarkus.flyway.active", "false",
                "quarkus.flyway.migrate-at-start", "false",
                "quarkus.scheduler.enabled", "false");
    }
}
