package dev.erickmurakami.portfolio.interfaces.rest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import dev.erickmurakami.portfolio.application.dto.DeveloperProfileRequest;
import dev.erickmurakami.portfolio.application.services.ResumeImportService;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/resumes")
public class ResumeResource {

    @Inject
    ResumeImportService service;

    @POST
    @Path("/import")
    @Blocking
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public DeveloperProfileRequest importResume(@RestForm("file") FileUpload file,
            @RestForm("githubUsername") String githubUserName) throws IOException {
        if (file == null) throw new BadRequestException("File is required");
        try {
            if (githubUserName == null || githubUserName.isBlank())
                throw new BadRequestException("GitHub username is required");
            if (file.size() <= 0 || file.size() > 4 * 1024 * 1024)
                throw new BadRequestException("PDF must be between 1 byte and 4 MiB");
            try (InputStream input = Files.newInputStream(file.uploadedFile())) {
                if (!Arrays.equals(input.readNBytes(5), "%PDF-".getBytes(StandardCharsets.US_ASCII)))
                    throw new BadRequestException("Invalid PDF file");
            }
            try {
                return service.importResume(file.uploadedFile(), githubUserName);
            } catch (RuntimeException ex) {
                // Do not expose model responses or resume text in an error body.
                throw new BadRequestException("Resume extraction failed; check the PDF and provider configuration");
            }
        } finally {
            Files.deleteIfExists(file.uploadedFile());
        }
    }
}
