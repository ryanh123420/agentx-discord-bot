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

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Future
    private LocalDate date;

    @NotBlank
    private LocalDateTime datePosted;

    @NotBlank
    private LocalDateTime dateUpdated;

    private DayOfWeek day;

}
