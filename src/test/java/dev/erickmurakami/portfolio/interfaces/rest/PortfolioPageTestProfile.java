package dev.erickmurakami.portfolio.interfaces.rest;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class PortfolioPageTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        // HTTP/template checks need neither a database nor background ingestion.
        return Map.of(
                "quarkus.datasource.jdbc.initial-size", "0",
                "quarkus.datasource.jdbc.min-size", "0",
                "quarkus.datasource.devservices.enabled", "false",
                "quarkus.hibernate-orm.active", "false",
                "quarkus.flyway.active", "false",
                "quarkus.flyway.migrate-at-start", "false",
                "quarkus.scheduler.enabled", "false");
    }
}
