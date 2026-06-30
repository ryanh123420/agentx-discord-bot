package com.ryanh.agent_discord_bot.service;

import com.ryanh.agent_discord_bot.repository.PostOutRepository;
import org.springframework.stereotype.Service;

@Service
public class PostOutService {

    private final PostOutRepository postOutRepository;

    public PostOutService(PostOutRepository postOutRepository) {
        this.postOutRepository = postOutRepository;
    }


}
