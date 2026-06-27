package com.ryanh.agent_discord_bot.repository;

import com.ryanh.agent_discord_bot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.io.StringReader;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Integer> {

    Optional<User> findByDiscordId(String discordId);
}
