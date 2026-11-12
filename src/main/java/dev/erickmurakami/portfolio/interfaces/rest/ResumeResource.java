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
    public DeveloperProfileRequest importResume(@RestForm("file") FileUpload file, @RestForm("githubUsername") String githubUserName) throws IOException{
        if(file == null)
            throw new BadRequestException("File cannot be null");

        try(InputStream input = Files.newInputStream(file.uploadedFile())){
            byte[] signature = input.readNBytes(5);
            if(!Arrays.equals(signature, "%PDF-".getBytes(StandardCharsets.US_ASCII))){
                throw new BadRequestException("Invalid PDF file");
            }
        }
        try{
            return service.importResume(file.uploadedFile(), githubUserName);
        } catch (IllegalArgumentException e){
            throw new BadRequestException(e.getMessage(), e);
        }
    }
}
