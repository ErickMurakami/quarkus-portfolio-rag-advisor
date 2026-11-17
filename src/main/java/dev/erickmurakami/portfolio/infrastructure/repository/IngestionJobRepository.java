package dev.erickmurakami.portfolio.infrastructure.repository;

import java.util.UUID;

import dev.erickmurakami.portfolio.domain.models.IngestionJob;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IngestionJobRepository implements PanacheRepositoryBase<IngestionJob, UUID> {

}
