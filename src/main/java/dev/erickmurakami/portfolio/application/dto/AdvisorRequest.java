package dev.erickmurakami.portfolio.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AdvisorRequest(
        @NotBlank String question) {

}
