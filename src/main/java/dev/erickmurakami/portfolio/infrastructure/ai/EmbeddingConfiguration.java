package dev.erickmurakami.portfolio.infrastructure.ai;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class EmbeddingConfiguration {

    private static final String HTTP_CLIENT_FACTORY = "dev.langchain4j.http.client.jdk.JdkHttpClientBuilderFactory";

    @ConfigProperty(name = "gemini.api-key")
    String apiKey;

    @Produces
    @ApplicationScoped
    public EmbeddingModel embeddingModel() {
        return createModel(GoogleAiEmbeddingModel.TaskType.RETRIEVAL_DOCUMENT);
    }

    @Produces
    @ApplicationScoped
    @QueryEmbedding
    public EmbeddingModel queryEmbeddingModel() {
        return createModel(GoogleAiEmbeddingModel.TaskType.RETRIEVAL_QUERY);
    }

    private EmbeddingModel createModel(GoogleAiEmbeddingModel.TaskType taskType) {
        System.setProperty("langchain4j.http.clientBuilderFactory", HTTP_CLIENT_FACTORY);
        return GoogleAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-embedding-001")
                .taskType(taskType)
                .outputDimensionality(1024)
                .httpClientBuilder(JdkHttpClient.builder())
                .build();
    }
}
