package dev.erickmurakami.portfolio.infrastructure.ai;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.voyageai.VoyageAiEmbeddingModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import dev.langchain4j.http.client.jdk.JdkHttpClient;

@ApplicationScoped
public class EmbeddingConfiguration {

    @ConfigProperty(name = "voyage.api-key")
    String apiKey;

    @Produces
    @ApplicationScoped
    public EmbeddingModel embeddingModel() {
        return VoyageAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("voyage-4-lite")
                .httpClientBuilder(JdkHttpClient.builder())
                .build();
    }
}
