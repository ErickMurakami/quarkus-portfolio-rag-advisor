package dev.erickmurakami.portfolio.infrastructure.ai;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class EmbeddingConfiguration {

    @ConfigProperty(name = "gemini.api-key")
    String apiKey;

    @Produces
    @ApplicationScoped
    public EmbeddingModel embeddingModel() {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-embedding-001")
                .taskType(GoogleAiEmbeddingModel.TaskType.RETRIEVAL_DOCUMENT)
                .outputDimensionality(1024)
                .build();
    }
}
