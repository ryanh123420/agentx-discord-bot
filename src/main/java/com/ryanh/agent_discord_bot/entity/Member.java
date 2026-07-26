package com.ryanh.agent_discord_bot.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Entity
@Table(name="member")
public class Member {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
        private List<RaidCharacter> characters;

        @NotBlank
        private String battleTag;

        @NotBlank
        @Column(unique = true)
        private String discordId;


        public Member() {}

        public Member(String battleTag, String discordId) {
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
