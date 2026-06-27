package com.ryanh.agent_discord_bot.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.relational.core.sql.In;

import java.util.Objects;

@Entity
@Table(name="users")
public class User {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;
        @NotBlank
        private String battleTag;
        @NotBlank
        @Column(unique = true)
        private String discordId;

        public User() {}

        public User(String battleTag, String discordId) {
                this.battleTag = battleTag;
                this.discordId = discordId;
        }

        public Integer getId() {
                return id;
        }

        public String getBattleTag() {
                return battleTag;
        }

        public void setBattleTag(String battleTag) {
                this.battleTag = battleTag;
        }

        public String getDiscordId() {
                return discordId;
        }

        public void setDiscordId(String discordId) {
                this.discordId = discordId;
        }
}
