package com.ryanh.agent_discord_bot.repository;

import com.ryanh.agent_discord_bot.entity.PostOut;
import com.ryanh.agent_discord_bot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostOutRepository extends JpaRepository<PostOut, Integer> {

    List<PostOut> findAllByDiscordId(String discordId);
}
