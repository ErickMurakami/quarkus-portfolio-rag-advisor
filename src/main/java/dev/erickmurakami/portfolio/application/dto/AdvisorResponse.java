package dev.erickmurakami.portfolio.application.dto;

import java.util.List;

public record AdvisorResponse(
        String answer,
        List<String> strengths,
        List<PortfolioTip> tips,
        List<PortfolioSource> sources) {

}
