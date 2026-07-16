package com.ryanh.agent_discord_bot.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "postout", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"discord_id", "post_date"})
})
public class PostOut {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    private String discordId;

    @FutureOrPresent
    private LocalDate postDate;

    private LocalDateTime datePosted;

    public PostOut() {
    }

    public PostOut(String discordId, LocalDate postDate, LocalDateTime datePosted) {
        this.discordId = discordId;
        this.postDate = postDate;
        this.datePosted = datePosted;
    }

    public Integer getId() {
        return id;
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
}
