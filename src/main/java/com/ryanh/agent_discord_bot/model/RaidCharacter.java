package com.ryanh.agent_discord_bot.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.Objects;

@Entity
@Table(name="character")
public class RaidCharacter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @NotBlank
    @Column(unique = true)
    private String name;

    @NotBlank
    private String charClass;

    @NotBlank
    private String spec;

    public RaidCharacter() {
    }

    public RaidCharacter(String name, User user,String charClass, String spec) {
        this.name = name;
        this.user = user;
        this.charClass = charClass;
        this.spec = spec;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCharClass() {
        return charClass;
    }

    public void setCharClass(String charClass) {
        this.charClass = charClass;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RaidCharacter that = (RaidCharacter) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(charClass, that.charClass) && Objects.equals(spec, that.spec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, charClass, spec);
    }
}
