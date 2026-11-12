package dev.erickmurakami.portfolio.infrastructure.repository;

import java.util.Locale;
import java.util.Optional;

import dev.erickmurakami.portfolio.domain.models.DeveloperProfile;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DeveloperProfileRepository implements PanacheRepository<DeveloperProfile>{

    public Optional<DeveloperProfile> findByGithubUsername(String githubUsername){
        return find("githubUsername = ?1", githubUsername.trim().toLowerCase(Locale.ROOT)).firstResultOptional();
    }
}
