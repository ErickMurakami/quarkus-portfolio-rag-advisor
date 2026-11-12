package dev.erickmurakami.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.erickmurakami.portfolio.application.dto.DeveloperProfileRequest;
import dev.erickmurakami.portfolio.application.dto.DeveloperProfileResponse;
import dev.erickmurakami.portfolio.application.dto.SkillRequest;
import dev.erickmurakami.portfolio.application.services.DeveloperProfileService;
import dev.erickmurakami.portfolio.domain.enums.SkillCategory;
import dev.erickmurakami.portfolio.domain.models.DeveloperProfile;
import dev.erickmurakami.portfolio.infrastructure.repository.DeveloperProfileRepository;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class DeveloperProfileServiceTest {

    @Mock
    DeveloperProfileRepository repository;

    @InjectMocks
    DeveloperProfileService service;

    private DeveloperProfile profile;

    @BeforeEach
    void setup() {
        profile = new DeveloperProfile("Erick Murakami", "erickmurakami");
    }

    @Test
    void shouldCreateDeveloperWithSkills(){
        var request = new DeveloperProfileRequest("Erick Murakami", "erickmurakami", List.of(new SkillRequest("Java", SkillCategory.LANGUAGE), new SkillRequest("Quarkus", SkillCategory.FRAMEWORK)));
        DeveloperProfileResponse response = service.create(request);

        assertNotNull(response);
        assertEquals(2, profileFromPersisted().getSkills().size());
        verify(repository).persist(any(DeveloperProfile.class));
    }

    @Test
    void shouldCreateDeveloperWithoutSkills(){
        var request = new DeveloperProfileRequest("Erick Murakami", "erickmurakami", null);
        DeveloperProfileResponse response = service.create(request);
        assertNotNull(response);
        assertEquals(0, profileFromPersisted().getSkills().size());
        verify(repository).persist(any(DeveloperProfile.class));
    }

    @Test
    void shouldNormalizeGithubUsernameWhenCreatingDeveloper(){
        var request = new DeveloperProfileRequest("Erick Murakami", "  ErickMurakami  ", null);
        DeveloperProfileResponse response = service.create(request);
        assertNotNull(response);
        assertEquals("erickmurakami", profileFromPersisted().getGithubUsername());
        verify(repository).persist(any(DeveloperProfile.class));
    }

    @Test
    void shouldFindDeveloperById(){
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(profile));
        DeveloperProfileResponse response = service.findById(1L);
        assertNotNull(response);
        verify(repository).findByIdOptional(1L);
    }

    @Test
    void shouldFindDeveloperByGithubUsername(){
        when(repository.findByGithubUsername("erickmurakami")).thenReturn(Optional.of(profile));
        DeveloperProfileResponse response = service.findByGithubUsername("erickmurakami");
        assertNotNull(response);
        verify(repository).findByGithubUsername("erickmurakami");
    }

    @Test
    void shouldThrowExceptionWhenDeveloperNotFoundById(){
        when(repository.findByIdOptional(1L)).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.findById(1L);
        });
        assertEquals("Developer profile not found for ID: 1", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenDeveloperNotFoundByGithubUsername(){
        when(repository.findByGithubUsername("johndoe")).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.findByGithubUsername("johndoe");
        });
        assertEquals("Developer profile not found for GitHub username: johndoe", exception.getMessage());
    }

    private DeveloperProfile profileFromPersisted(){
        var captor = ArgumentCaptor.forClass(DeveloperProfile.class);
        verify(repository).persist(captor.capture());
        return captor.getValue();
    }
}
