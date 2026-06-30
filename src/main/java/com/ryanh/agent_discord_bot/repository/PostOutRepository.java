package com.ryanh.agent_discord_bot.repository;

import com.ryanh.agent_discord_bot.model.PostOut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostOutRepository extends JpaRepository<PostOut, Integer> {
}
