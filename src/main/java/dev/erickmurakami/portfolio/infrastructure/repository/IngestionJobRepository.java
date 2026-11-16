package dev.erickmurakami.portfolio.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;

import dev.erickmurakami.portfolio.domain.models.IngestionJob;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IngestionJobRepository implements PanacheRepository<IngestionJob> {

    public Optional<IngestionJob> findByIdOptional(UUID id) {
        return findByIdOptional(id);
    }
}
