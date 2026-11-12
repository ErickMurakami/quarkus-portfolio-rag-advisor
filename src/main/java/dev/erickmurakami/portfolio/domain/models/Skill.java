package dev.erickmurakami.portfolio.domain.models;

import dev.erickmurakami.portfolio.domain.enums.SkillCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "skill")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private SkillCategory category;

    @ManyToOne
    @JoinColumn(name = "dev_id", nullable = false)
    private DeveloperProfile dev;

    protected Skill() {
    }

    public Skill(String name, SkillCategory category) {
        this.name = name;
        this.category = category;
    }

    void setDev(DeveloperProfile dev) {
        this.dev = dev;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SkillCategory getCategory() {
        return category;
    }

    public DeveloperProfile getDev() {
        return dev;
    }

}
