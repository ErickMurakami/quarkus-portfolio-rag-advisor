package dev.erickmurakami.portfolio.application.services;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.erickmurakami.portfolio.application.dto.DeveloperProfileRequest;
import dev.erickmurakami.portfolio.application.dto.ExtractedResume;
import dev.erickmurakami.portfolio.application.dto.SkillRequest;
import dev.erickmurakami.portfolio.infrastructure.document.PdfTextExtractor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ResumeImportService {

    @Inject
    PdfTextExtractor pdfTextExtractor;

    @Inject
    ResumeAiExtractor resumeAiExtractor;

    @Inject
    ObjectMapper mapper;

    public DeveloperProfileRequest importResume(Path pdf, String githubUsername) throws IOException {
        String text = pdfTextExtractor.extract(pdf);
        if (text == null || text.isBlank()) {
            throw new RuntimeException("PDF has no text content");
        }
        if (text.length() > 100_000) {
            throw new IllegalArgumentException("Resume text exceeds supported size");
        }
        String json = resumeAiExtractor.extractDeveloperProfileFromResume(text);

        ExtractedResume resume;

        try {
            resume = mapper.readValue(json, ExtractedResume.class);
        } catch (IOException e) {
            throw new RuntimeException("AI returned an invalid response", e);
        }

        if(resume == null || resume.fullName() == null || resume.fullName().isBlank()){
            throw new RuntimeException("AI did not return a valid full name");
        }

        List<SkillRequest> skills = resume.skills() == null ? List.of() : resume.skills();

        if(skills.stream().anyMatch(s -> s == null || s.name() == null || s.name().isBlank() || s.category() == null)){
            throw new RuntimeException("AI returned invalid skills");
        }

        return new DeveloperProfileRequest(resume.fullName(), githubUsername.trim(), skills);
    }
}
