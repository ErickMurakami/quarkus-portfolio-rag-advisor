package dev.erickmurakami.portfolio.domain.models;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "dev_profile")
public class DeveloperProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "github_username", nullable = false, unique = true)
    private String githubUsername;

    @OneToMany(mappedBy = "dev", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Skill> skills = new ArrayList<>();

    protected DeveloperProfile() {
    }

    public DeveloperProfile(String fullName, String githubUsername){
        this.fullName = fullName;
        this.githubUsername = githubUsername;
    }

    public void addSkill(Skill skill) {
        skills.add(skill);
        skill.setDev(this);
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public List<Skill> getSkills() {
        return List.copyOf(skills);
    }

}
