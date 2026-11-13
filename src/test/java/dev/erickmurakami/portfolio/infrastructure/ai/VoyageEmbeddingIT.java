package dev.erickmurakami.portfolio.infrastructure.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import dev.langchain4j.model.embedding.EmbeddingModel;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class VoyageEmbeddingIT {

    @Inject
    EmbeddingModel model;

    @Test
    void shouldGenerateEmbeddingWith1024Dimensions() {
        var response = model.embed("Econolyze uses Java, Quarkus, PostgreSQL and Clean Architecture.");

        float[] vector = response.content().vector();

        assertNotNull(vector);
        assertEquals(1024, vector.length);

        for (float value : vector) {
            assertTrue(Float.isFinite(value));
        }
    }
}
