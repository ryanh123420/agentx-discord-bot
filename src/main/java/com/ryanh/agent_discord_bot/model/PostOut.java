package com.ryanh.agent_discord_bot.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "postout")
public class PostOut {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    private String discordId;

    @Future
    private LocalDate postDate;

    @NotBlank
    private LocalDateTime datePosted;

    @NotBlank
    private LocalDateTime dateUpdated;

    public PostOut() {
    }

    public PostOut(String discordId, LocalDate postDate, LocalDateTime datePosted, LocalDateTime dateUpdated) {
        this.discordId = discordId;
        this.postDate = postDate;
        this.datePosted = datePosted;
        this.dateUpdated = dateUpdated;
    }

    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public LocalDate getPostDate() {
        return postDate;
    }

    public void setPostDate(LocalDate postDate) {
        this.postDate = postDate;
    }

    public LocalDateTime getDatePosted() {
        return datePosted;
    }

    public void setDatePosted(LocalDateTime datePosted) {
        this.datePosted = datePosted;
    }

    public LocalDateTime getDateUpdated() {
        return dateUpdated;
    }

    public void setDateUpdated(LocalDateTime dateUpdated) {
        this.dateUpdated = dateUpdated;
    }


}
