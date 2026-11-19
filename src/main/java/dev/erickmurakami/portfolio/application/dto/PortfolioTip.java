package dev.erickmurakami.portfolio.application.dto;

import java.util.List;

import dev.erickmurakami.portfolio.domain.enums.TipPriority;

public record PortfolioTip(
        String title,
        String description,
        TipPriority priority,
        List<String> relatedRepos) {

}
