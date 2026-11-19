package dev.erickmurakami.portfolio.application.dto;

public record PortfolioSource(
        String repo,
        String sourceReference,
        double similarity) {

}
