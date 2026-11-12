package dev.erickmurakami.portfolio.application.services;

import java.util.Locale;

import dev.erickmurakami.portfolio.application.dto.DeveloperProfileRequest;
import dev.erickmurakami.portfolio.application.dto.DeveloperProfileResponse;
import dev.erickmurakami.portfolio.application.dto.SkillRequest;
import dev.erickmurakami.portfolio.domain.models.DeveloperProfile;
import dev.erickmurakami.portfolio.domain.models.Skill;
import dev.erickmurakami.portfolio.infrastructure.repository.DeveloperProfileRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DeveloperProfileService {

    @Inject
    DeveloperProfileRepository repository;

    @Transactional
    public DeveloperProfileResponse create(DeveloperProfileRequest request){
        String username = request.githubUsername().trim().toLowerCase(Locale.ROOT);
        DeveloperProfile profile = new DeveloperProfile(request.fullName(), username);
        if(request.skills() != null && !request.skills().isEmpty()){
            for(SkillRequest skill : request.skills()){
                profile.addSkill(new Skill(skill.name(), skill.category()));
            }
        }
        repository.persist(profile);
        return DeveloperProfileResponse.from(profile);
    }

    @Transactional
    public DeveloperProfileResponse findById(Long id){
        return DeveloperProfileResponse.from(repository.findByIdOptional(id).orElseThrow(() -> new RuntimeException("Developer profile not found for ID: " + id)));
    }

    @Transactional
    public DeveloperProfileResponse findByGithubUsername(String githubUsername){
        return DeveloperProfileResponse.from(repository.findByGithubUsername(githubUsername.trim().toLowerCase(Locale.ROOT)).orElseThrow(() -> new RuntimeException("Developer profile not found for GitHub username: " + githubUsername)));
    }
}
