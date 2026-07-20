package com.ryanh.agent_discord_bot.repository;

import com.ryanh.agent_discord_bot.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member,Integer> {

    Optional<Member> findByDiscordId(String discordId);
    Optional<Member> findByBattleTag(String battletag);
}
