package dev.erickmurakami.portfolio.application.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.erickmurakami.portfolio.application.dto.RetrievedChunk;
import dev.erickmurakami.portfolio.application.dto.PortfolioSource;
import dev.erickmurakami.portfolio.domain.enums.AccessScope;
import dev.erickmurakami.portfolio.domain.enums.TipPriority;

@ExtendWith(MockitoExtension.class)
class PortfolioAdvisorServiceTest {
    @Mock PortfolioRetrievalService retrieval;
    @Mock PortfolioAdvisorAi ai;
    @Spy ObjectMapper mapper = new ObjectMapper();
    @InjectMocks PortfolioAdvisorService service;

    private static final String QUESTION = "How can I improve?";
    private static final String PUBLIC = "https://github.com/synthetic/public-demo";
    private static final String PRIVATE = "https://github.com/synthetic/private-demo";

    @Test
    void shouldReturnEmptyRecommendationsWithoutCallingAiWhenNoEvidence() {
        when(retrieval.retrieve(1L, QUESTION)).thenReturn(List.of());
        var result = service.advise(1L, QUESTION);
        assertEquals("No evidence found for the developer", result.answer());
        assertTrue(result.strengths().isEmpty());
        assertTrue(result.tips().isEmpty());
        assertTrue(result.sources().isEmpty());
        verifyNoInteractions(ai, mapper);
    }

    @Test
    void shouldParseAnalysisDeduplicateSourcesAndFilterRelatedRepositories() {
        when(retrieval.retrieve(1L, QUESTION)).thenReturn(List.of(
                chunk(1, PUBLIC, AccessScope.PUBLIC, "Java"),
                chunk(2, PRIVATE, AccessScope.PRIVATE, "Synthetic backend"),
                chunk(3, PRIVATE, AccessScope.PRIVATE, "Synthetic tests"),
                chunk(4, "https://github.com/synthetic/second-private", AccessScope.PRIVATE, "SQL")));
        when(ai.analyse(anyString())).thenReturn("""
                {"answer":"Add examples", "strengths":["Java"], "tips":[null,
                  {"title":"Tests", "description":"Add integration tests", "priority":"HIGH",
                   "relatedRepos":["public-demo","Private Repository 1","Private Repository 2",
                     "public-demo","unrelated","private-demo","https://github.com/synthetic/private-demo",null]}]}
                """);
        var result = service.advise(1L, QUESTION);
        assertEquals("Add examples", result.answer());
        assertEquals(List.of("Java"), result.strengths());
        assertEquals(1, result.tips().size());
        var tip = result.tips().getFirst();
        assertEquals("Tests", tip.title());
        assertEquals("Add integration tests", tip.description());
        assertEquals(TipPriority.HIGH, tip.priority());
        assertEquals(List.of("public-demo", "Private Repository 1", "Private Repository 2"), tip.relatedRepos());
        assertEquals(List.of(new PortfolioSource("public-demo", PUBLIC, 0.9),
                new PortfolioSource("Private Repository 1", null, 0.9),
                new PortfolioSource("Private Repository 2", null, 0.9)), result.sources());
        var prompt = ArgumentCaptor.forClass(String.class);
        verify(ai).analyse(prompt.capture());
        assertTrue(prompt.getValue().contains("Developer question:\n" + QUESTION));
        assertTrue(prompt.getValue().contains("Repository: public-demo\nContent:\nJava"));
        assertEquals(2, prompt.getValue().split("Repository: Private Repository 1", -1).length - 1);
        assertTrue(prompt.getValue().contains("Repository: Private Repository 2\nContent:\nSQL"));
        assertFalse(prompt.getValue().contains(PRIVATE));
        assertFalse(prompt.getValue().contains("second-private"));
        verify(retrieval).retrieve(1L, QUESTION);
        verifyNoMoreInteractions(retrieval);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"answer\":\"OK\"}",
            "{\"answer\":\"OK\",\"strengths\":null,\"tips\":null}",
            "{\"answer\":\"OK\",\"strengths\":[],\"tips\":[]}"})
    void shouldNormalizeMissingNullAndEmptyLists(String json) {
        evidenceAndResponse(json);
        var result = service.advise(1L, QUESTION);
        assertTrue(result.strengths().isEmpty());
        assertTrue(result.tips().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]"})
    void shouldNormalizeNullAndEmptyRelatedRepositories(String relatedRepos) {
        evidenceAndResponse("{\"answer\":\"OK\",\"tips\":[{\"relatedRepos\":" + relatedRepos + "}]}");
        assertTrue(service.advise(1L, QUESTION).tips().getFirst().relatedRepos().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not JSON", "", " ", "{", "{\"answer\":\"OK\",\"tips\":[{\"priority\":\"URGENT\"}]}"})
    void shouldRejectInvalidJson(String json) {
        evidenceAndResponse(json);
        var error = assertThrows(IllegalStateException.class, () -> service.advise(1L, QUESTION));
        assertEquals("AI returned invalid JSON", error.getMessage());
        assertNotNull(error.getCause());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "{\"answer\":null}", "{\"answer\":\"\"}", "{\"answer\":\"   \"}"})
    void shouldRejectEmptyAnalysis(String json) {
        evidenceAndResponse(json);
        assertEquals("AI returned an empty analysis.", assertThrows(IllegalStateException.class,
                () -> service.advise(1L, QUESTION)).getMessage());
    }

    @Test
    void shouldExposeCurrentPrivacyLimitForPrivateContentAndGeneratedFreeText() {
        // Synthetic data: aliases do not redact evidence or model-generated free text.
        String identifyingContent = "Synthetic client ACME-TEST, internal ID TEST-42, " + PRIVATE;
        when(retrieval.retrieve(1L, QUESTION)).thenReturn(List.of(
                chunk(1, PRIVATE, AccessScope.PRIVATE, identifyingContent)));
        when(ai.analyse(anyString())).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(0);
            assertTrue(prompt.contains("Repository: Private Repository 1"));
            assertTrue(prompt.contains(identifyingContent));
            return mapper.writeValueAsString(new dev.erickmurakami.portfolio.application.dto.AdvisorDraft(
                    identifyingContent, List.of(identifyingContent), List.of(
                            new dev.erickmurakami.portfolio.application.dto.PortfolioTip(
                                    identifyingContent, identifyingContent, TipPriority.LOW, List.of("private-demo")))));
        });
        var result = service.advise(1L, QUESTION);
        assertEquals(identifyingContent, result.answer());
        assertEquals(List.of(identifyingContent), result.strengths());
        assertEquals(identifyingContent, result.tips().getFirst().title());
        assertEquals(identifyingContent, result.tips().getFirst().description());
        assertTrue(result.tips().getFirst().relatedRepos().isEmpty());
        assertNull(result.sources().getFirst().sourceReference());
    }

    private void evidenceAndResponse(String response) {
        when(retrieval.retrieve(1L, QUESTION)).thenReturn(List.of(chunk(1, PUBLIC, AccessScope.PUBLIC, "Java")));
        when(ai.analyse(anyString())).thenReturn(response);
    }

    private RetrievedChunk chunk(long id, String reference, AccessScope scope, String content) {
        return new RetrievedChunk(id, content, reference, scope, 0.9);
    }
}
