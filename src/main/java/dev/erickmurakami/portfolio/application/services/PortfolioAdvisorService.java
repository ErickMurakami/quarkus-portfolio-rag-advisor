package dev.erickmurakami.portfolio.application.services;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.erickmurakami.portfolio.application.dto.AdvisorDraft;
import dev.erickmurakami.portfolio.application.dto.AdvisorResponse;
import dev.erickmurakami.portfolio.application.dto.PortfolioSource;
import dev.erickmurakami.portfolio.application.dto.PortfolioTip;
import dev.erickmurakami.portfolio.application.dto.RetrievedChunk;
import dev.erickmurakami.portfolio.domain.enums.AccessScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PortfolioAdvisorService {

    @Inject
    PortfolioRetrievalService retrieval;

    @Inject
    PortfolioAdvisorAi ai;

    @Inject
    ObjectMapper mapper;

    public AdvisorResponse advise(Long devId, String question) {
        List<RetrievedChunk> chunks = retrieval.retrieve(devId, question);

        if (chunks.isEmpty()) {
            return new AdvisorResponse(
                    "No evidence found for the developer",
                    List.of(),
                    List.of(),
                    List.of());
        }

        Map<String, String> aliases = repositoryAliases(chunks);
        String prompt = buildPrompt(question, chunks, aliases);
        AdvisorDraft draft = parse(ai.analyse(prompt));

        Map<String, PortfolioSource> sources = new LinkedHashMap<>();

        for (RetrievedChunk chunk : chunks) {
            String reference = chunk.sourceReference();

            boolean privateRepo = chunk.accessScope() == AccessScope.PRIVATE;

            sources.putIfAbsent(reference, new PortfolioSource(
                    aliases.get(reference),
                    privateRepo ? null : reference,
                    chunk.similarity()));
        }

        Set<String> allowedRepos = sources.values()
                .stream()
                .map(p -> p.repo())
                .collect(Collectors.toSet());

        List<PortfolioTip> tips = draft.tips() == null
                ? List.of()
                : draft.tips().stream()
                        .filter(tip -> tip != null)
                        .map(tip -> new PortfolioTip(
                                tip.title(),
                                tip.description(),
                                tip.priority(),
                                tip.relatedRepos() == null
                                        ? List.of()
                                        : tip.relatedRepos().stream()
                                                .filter(allowedRepos::contains)
                                                .distinct()
                                                .toList()))
                        .toList();

        return new AdvisorResponse(draft.answer(),
                draft.strengths() == null ? List.of() : List.copyOf(draft.strengths()),
                tips,
                List.copyOf(sources.values()));
    }

    private String buildPrompt(String question, List<RetrievedChunk> chunks, Map<String, String> aliases) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Developer question:\n")
                .append(question)
                .append("\n\nRepository evidence\n\n");

        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);

            prompt.append("Evidence ")
                    .append(i + 1)
                    .append("\nRepository: ")
                    .append(aliases.get(chunk.sourceReference()))
                    .append("\nContent:\n")
                    .append(chunk.content())
                    .append("\n\n");
        }

        prompt.append("""
                    Analyse only the repository evidence above.
                    Provide specific and actionable portfolio tips.
                    Return valid JSON using required schema.
                """);

        return prompt.toString();
    }

    private AdvisorDraft parse(String response) {
        try {
            AdvisorDraft draft = mapper.readValue(response, AdvisorDraft.class);

            if (draft == null || draft.answer() == null || draft.answer().isBlank()) {
                throw new IllegalStateException("AI returned an empty analysis.");
            }
            return draft;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("AI returned invalid JSON", ex);
        }
    }

    private String repositoryName(String reference) {
        try {
            String[] parts = URI.create(reference).getPath().split("/");

            if (parts.length >= 3) {
                return parts[2];
            }
        } catch (IllegalArgumentException ex) {

        }
        return reference;
    }

    private Map<String, String> repositoryAliases(List<RetrievedChunk> chunks) {
        Map<String, String> aliases = new LinkedHashMap<>();
        int privateCount = 0;

        for (RetrievedChunk chunk : chunks) {
            String reference = chunk.sourceReference();

            if (aliases.containsKey(reference)) {
                continue;
            }

            if (chunk.accessScope() == AccessScope.PRIVATE) {
                aliases.put(reference, "Private Repository " + ++privateCount);
            } else {
                aliases.put(reference, repositoryName(reference));
            }
        }

        return aliases;
    }
}
