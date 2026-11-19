package dev.erickmurakami.portfolio.application.dto;

import java.util.List;

public record AdvisorDraft(
        String answer,
        List<String> strengths,
        List<PortfolioTip> tips) {

}
