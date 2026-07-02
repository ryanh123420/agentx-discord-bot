package com.ryanh.agent_discord_bot.repository;

import com.ryanh.agent_discord_bot.entity.RaidCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RaidCharacterRepository extends JpaRepository<RaidCharacter,Integer> {
}
